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
