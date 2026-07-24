# Feature Chat Instructions

## Scope

These instructions apply to `feature/chat/**` and complement the repository-level
`AGENTS.md`. Follow the root instructions for shared workflow, safety, architecture,
validation, and reporting requirements.

## Module boundary

This module owns chat presentation, memory and task UI, MCP demonstrations, RAG
presentation, and Android calendar interaction.

- Depend on established `core:domain` contracts and shared `core:ui` components.
- Do not import `core:data` or `core:network`.
- Keep transport DTOs, persistence entities, backend clients, and application-level
  dependency injection outside this module.
- Reuse existing domain services and repository interfaces instead of duplicating
  business logic in presentation code.

## Presentation rules

- Compose screens render state and forward user actions; they do not access
  repositories or Android providers directly.
- ViewModels keep `MutableStateFlow` private and expose read-only state with
  `StateFlow` or `asStateFlow()`.
- Update immutable UI state with `copy` or `MutableStateFlow.update`.
- Launch lifecycle-bound suspend work in `viewModelScope`.
- Use sealed types for closed event, state, result, and pending-action sets.
- Keep platform access and parsing in focused collaborators rather than Composables.
- Do not add unrelated responsibilities to `ChatViewModel`; prefer an existing
  specialized ViewModel or a focused collaborator.

## Interaction ownership

For every user-facing interaction, identify:

- who starts the interaction;
- who decides that it is complete;
- who may cancel it;
- whether the Android framework or provider may complete it automatically;
- whether framework-default completion matches the intended UX.

For user-controlled interactions, explicit user completion is preferred over
framework heuristics unless the approved behavior explicitly allows automatic
completion.

Do not claim that an interaction is manually controlled when the selected Android
API may still terminate it automatically because of silence detection, timeout,
provider behavior, or another platform heuristic.

When the selected API cannot guarantee the requested interaction model, stop at the
planning stage and report:

- the platform limitation;
- the observable UX consequence;
- repository-compatible alternatives;
- whether an alternative changes dependencies, architecture, or scope.

## Compose and ViewModel lifecycle

- A Composable may observe ViewModel state and forward user and lifecycle events.
- A Composable must not permanently destroy a dependency owned by a ViewModel.
- `DisposableEffect` may permanently release only resources created and owned by
  that effect.
- Removal from composition, navigation away, configuration change, and application
  backgrounding are different lifecycle events and must not be treated as identical.
- ViewModel-owned resources are permanently released from `ViewModel.onCleared()`
  unless active repository code establishes another owner.
- Temporary lifecycle interruption should stop or cancel active work without
  violating the declared ownership model.
- Sensitive active work such as microphone, camera, location, or media capture must
  not continue unexpectedly after the application is no longer visible.

## Callback-based Android integrations

For callback-based APIs:

- assign every operation a session identity, generation, token, or equivalent stale
  callback protection;
- ignore callbacks from obsolete operations;
- handle callback after cancellation;
- handle callback after owner cleanup;
- handle callback from operation A after operation B starts;
- handle duplicate starts;
- distinguish successful completion, user stop, cancellation, provider-driven
  completion, and permanent destruction;
- define which callback commits user-visible output;
- prevent the UI from remaining indefinitely in an active state.

A single global Boolean cancellation flag is not sufficient when an older callback
can arrive after a newer operation has started.

## Android permission boundaries

- Handle ordinary permission denial and permanent denial separately.
- Do not assume `LocalContext.current` is directly an `Activity`.
- Unwrap `ContextWrapper` when Activity-only APIs are required.
- A permission check and a later platform call are separate failure boundaries.
- Platform calls must still handle permission revocation, `SecurityException`,
  unavailable providers, and framework failure after permission is granted.

## Placement and naming

- Put Compose screens in `presentation/screen` and name them `*Screen`.
- Put primary ViewModels in `presentation/viewmodel` and name them `*ViewModel`.
- Use `*UiState` for immutable screen state and `*UiEvent` for user intents.
- Keep MCP presentation code in `presentation/mcp`, memory presentation code in
  `presentation/memory`, and calendar behavior in `calendar`.
- Keep tests in the matching production package and name test classes `*Test`.

## Calendar invariants

- Require `READ_CALENDAR` for reads and `WRITE_CALENDAR` for mutations.
- Create, update, and delete must first produce a `PendingCalendarAction`.
- Perform mutations only after explicit UI confirmation through the established
  `CalendarToolExecutor.confirm(...)` path.
- Preserve pending actions across Android permission requests.
- Do not mutate ambiguous matches or unsupported recurring events.
- Keep Calendar Provider access off the main thread and preserve the calendar-write
  timeout and retry guidance.
- Calendar tests use fakes and never write to a real device calendar.

## Voice and long-form dictation invariants

Voice input for chat is a user-controlled text-entry interaction, not a short
single-command interaction.

The behavioral contract must explicitly distinguish:

- starting voice input;
- receiving partial or intermediate recognition;
- platform-detected end of one speech segment;
- a natural pause while the user is thinking;
- continuation after a pause;
- explicit user stop;
- cancellation without committing unintended text;
- committing recognized text into the current draft;
- permanent recognizer cleanup.

The implementation must not silently treat platform-detected silence as equivalent
to the user's explicit Stop action unless automatic completion is part of the
approved behavior.

For long-form dictation:

- normal pauses must not unexpectedly end the whole user interaction;
- the user must retain control over final completion when the approved UX requires
  manual stopping;
- provider-driven completion of one recognition segment must be handled explicitly;
- recognized segments must not be lost, duplicated, or committed out of order;
- automatic message sending is prohibited unless explicitly requested;
- recognized text is merged into the current draft using the approved text-merging
  behavior;
- cancellation must not commit late or obsolete results;
- navigation away and application backgrounding must stop active microphone work;
- permanent recognizer destruction belongs to the declared resource owner.

If the Android recognition provider cannot guarantee uninterrupted long-form
dictation until explicit user stop, the plan must describe how the implementation
will preserve the user-level interaction across provider-controlled segments or
report that the requested UX requires a different approach.

Do not hide this limitation behind a microphone button that visually remains active
while the provider has already stopped listening.

## Testing

Run the narrow module checks from the repository root:

```powershell
.\gradlew.bat :feature:chat:testDebugUnitTest
.\gradlew.bat :feature:chat:compileDebugKotlin
```

- Test changed behavior and its relevant failure paths.
- Use `FakeCalendarRepository` for calendar unit tests.
- Run `:app:compileDebugKotlin` when changes affect application DI, navigation,
  permissions, or feature integration.
- Inherit broader validation and final-report requirements from the root instructions.
  
  For stateful, callback-based, permission-gated, or lifecycle-bound changes, cover all
  applicable scenarios:

- idle to active;
- active to successful completion;
- active to user stop;
- active to cancellation;
- active to controlled failure;
- duplicate start while already active;
- cleanup while active;
- delayed callback after cancellation;
- callback from operation A after operation B starts;
- unavailable provider;
- ordinary permission denial;
- permanent permission denial;
- platform call throwing a controlled exception;
- provider-driven automatic completion;
- natural pause followed by continuation.

For voice-input changes, also validate:

1. short phrase recognition;
2. brief pause followed by continued speech;
3. several-second pause followed by continued speech;
4. dictation longer than a single short phrase;
5. explicit user stop;
6. cancellation without unintended text commit;
7. backgrounding while active;
8. navigation away while active;
9. cancel followed by immediate restart;
10. late callback from a previous session;
11. unavailable recognition service.

Mark every applicable scenario as one of:

- covered by JVM unit test;
- covered by instrumentation test;
- covered by manual device validation;
- not applicable, with a reason.

Do not report voice input as fully validated when only compilation and JVM tests were
executed.

## Reference implementations

- `calendar/CalendarToolExecutor.kt` — permission, validation, pending-action, and
  confirmation boundaries.
- `calendar/AndroidCalendarRepository.kt` — Calendar Provider access and I/O handling.
- `presentation/viewmodel/MemoryViewModel.kt` — focused MVVM state and domain
  delegation.
- `presentation/mcp/McpDemoViewModel.kt` — immutable state updates, lifecycle jobs,
  and cleanup.

## Module Definition of Done

In addition to the root Definition of Done:

- Module dependency boundaries remain intact.
- Screens do not gain direct repository or provider access.
- UI state remains immutable and read-only outside its owner.
- Calendar permission, confirmation, ambiguity, recurrence, and timeout safeguards
  remain intact.
- Changed behavior has focused tests.
- Relevant feature tests and compilation pass; application compilation also passes
  when integration changed.
- Every user-visible action maps to an operation with matching semantics.
- Interaction owner and completion owner are explicitly documented.
- Framework-driven automatic completion is checked against the approved UX.
- Natural pauses do not unexpectedly terminate user-controlled long-form input.
- Stop, cancellation, provider-driven completion, and permanent cleanup are distinct
  where the platform behavior requires them to be distinct.
- ViewModel-owned resources are not permanently destroyed by Composable disposal.
- Backgrounding, navigation away, restart, duplicate start, and stale callbacks have
  defined behavior.
- Platform limitations affecting the requested interaction are explicitly reported.
- Voice-input changes include device-level or instrumentation validation for pause
  and explicit-stop behavior, or clearly report that those checks remain unverified.
