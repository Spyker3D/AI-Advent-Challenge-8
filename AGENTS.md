# AI Assistant Project Instructions

## Scope and source of truth

These instructions apply to the whole repository. Base changes on active source and build configuration, not on generated artifacts or copied snapshots.

- Treat `settings.gradle.kts`, module `build.gradle.kts` files, `package.json` files, active source, and tests as authoritative.
- Do not edit `rag-indexer/input/project_sources`; it is a copied indexing input, not active application source.
- Do not hand-edit generated indexes and reports under `rag-indexer/output`, `app/src/main/assets/rag`, `.developer-assistant`, or `.support-assistant` unless the task explicitly concerns generated output. Use their owning generator instead.
- Never read, print, or modify secrets in `local.properties`. The build reads `OPENAI_API_KEY`, `PRIVATE_VPS_BASE_URL`, `PRIVATE_VPS_API_KEY`, and `PRIVATE_VPS_MODEL` from that untracked file.

## Required agent workflow

For implementation tasks, the main agent owns this sequence:

1. Use `researcher` and `module-research` to gather repository and platform evidence.
2. Use `implementation-plan` to produce:
    - behavioral contracts;
    - interaction ownership;
    - completion ownership;
    - state-transition table;
    - test matrix;
    - exact editable files.
3. Stop for explicit human approval.
4. Send the approved plan to `implementer`.
5. Run `implementation-review-loop`.
6. Run `android-validation`.
7. Report the first generation, reviewer findings, second generation, validation
   results, and remaining risks.

Subagents cannot grant human approval.

The user must not be required to enumerate implementation defects that violate an
already approved behavioral contract. Those defects belong to the automatic
reviewer-to-implementer corrective loop.

### Execution loop workflow exception

When an execution loop is explicitly started, the normal implementation workflow above is replaced by the autonomous workflow described in:

```text
execution-loop/EXECUTION_LOOP.md
```

During an execution loop:

* The main agent may inspect, modify, test, review, and commit repository changes directly.
* Using the `researcher`, `implementer`, `reviewer`, `test-writer`, or other specialized subagents is optional, not mandatory.
* Failure to start a specialized subagent is not a task blocker when the main agent can perform the same work using its own repository tools.
* The main agent must not stop for human approval between research, planning, implementation, testing, review, commit, or consecutive tasks.
* The task acceptance criteria and repository rules serve as approval for the bounded task.
* The main agent must perform an internal research, implementation, self-review, and validation sequence before completing each task.
* The main agent must stop only when all tasks are completed or when the current task has a genuine implementation or environment blocker that the main agent cannot work around safely.
* Missing optional subagent infrastructure is not by itself a valid `blocked` reason.
* A task may be marked `blocked` only when the main agent itself cannot read the required source, edit the required files, run the necessary validation, or create the requested commit.

This exception applies only while processing tasks from:

```text
execution-loop/tasks.json
```

The standard required agent workflow remains in effect for normal interactive implementation requests.


## Technology stack

- Kotlin with Gradle Kotlin DSL; the Gradle wrapper is the project build entry point.
- Android application built with the Android Gradle Plugin.
- Jetpack Compose with Material 3 and Navigation Compose.
- MVVM presentation using `ViewModel`, `StateFlow`, immutable UI state, and sealed UI events.
- Dagger for dependency injection.
- Kotlin Coroutines and Flow for asynchronous programming.
- Retrofit, OkHttp, and Gson for HTTP communication and JSON serialization.
- Room for local persistence and DataStore Preferences for application settings.
- Android Calendar Provider for calendar integration.
- JVM-based Gradle modules for `rag-core`, `rag-indexer`, and `developer-assistant`.
- Node.js CommonJS services for MCP servers using the built-in `node:test` runner where configured.
- JUnit-based testing for Android modules and Kotlin Test for JVM modules.

## Architecture and dependency direction

The Android application follows a modular domain/data/presentation split:

```text
app
├── feature:chat ────────┐
├── feature:settings ────┼──> core:domain
│                        └──> core:ui
├── core:data ──────────────> core:domain + core:network
├── core:network
└── core:ui ────────────────> core:domain

developer-assistant ──> rag-core
rag-indexer ──────────> rag-core
```

- `app` is the Android composition root: application/activity setup, Compose navigation, theme, and Dagger component/modules.
- `core:domain` owns entities, repository and service interfaces, use cases, agent orchestration, memory/state machines, invariants, RAG contracts, and domain logic.
- `core:data` implements domain interfaces and owns Room, DataStore, mappers, backend selection, Android RAG adapters, and MCP support adapters.
- `core:network` owns Retrofit APIs, DTOs, interceptors, and network construction.
- `core:ui` owns reusable Compose components.
- Feature modules own screens, UI state/events, ViewModels, and feature-specific Android integration. They depend on domain abstractions, not data implementations.
- Dagger bindings connect implementations to domain interfaces in `core/data/.../DataModule.kt`; `app/.../AppComponent.kt` assembles application modules.
- Suspend repository work performs network/database I/O off the main thread; `ChatRepositoryImpl` uses `withContext(Dispatchers.IO)`.

## Module structure

- `app`: APK, manifest/resources/assets, `AIAssistantApplication`, `MainActivity`, navigation, theme, application-level Dagger wiring.
- `core/domain`: domain entities, repositories, use cases, LLM/agent contracts, task memory and state machines, invariants, MCP orchestration, RAG, and support service.
- `core/data`: repository implementations, Room database/DAOs/entities, DataStore, mappers, LLM clients, Android RAG loading, and support providers.
- `core/network`: OpenAI, Ollama, and Private VPS Retrofit APIs/DTOs, auth interceptors, and network DI.
- `core/ui`: shared Compose message and loading components.
- `feature/chat`: chat/memory/MCP screens and ViewModels plus calendar tool parsing, preview, confirmation, and Calendar Provider repository.
- `feature/settings`: settings and support screens, UI state/events, and ViewModels.
- `rag-core`: reusable JVM scanner, chunker, embeddings, manifest, storage, indexing, and retrieval primitives.
- `rag-indexer`: JVM CLI and Gradle tasks for building, comparing, searching, and validating RAG indexes.
- `developer-assistant`: JVM CLI for project file tools, validation, RAG-assisted help, MCP Git access, and pull-request review.
- `mcp-server`: tested main MCP server with Git, support CRM, comment-upsert, and optional weather tools.
- `mcp-notes-server`, `mcp-tasks-server`: standalone Node.js MCP demo servers.
- `support-knowledge`: Markdown knowledge base packaged as `core:data` assets and indexed by developer tooling.

## Naming conventions

- Packages use lowercase reverse-domain names rooted at `com.aiassistant`; package paths mirror responsibility, such as `core.domain.repository` and `feature.chat.presentation.viewmodel`.
- Types, Compose functions, and files use `PascalCase`: `ChatRepository`, `SettingsViewModel`, `ChatScreen.kt`.
- Functions and properties use `camelCase`: `sendMessage`, `privateVpsBaseUrl`.
- Constants use `UPPER_SNAKE_CASE` in companion objects: `CALENDAR_WRITE_TIMEOUT_MS`, `OPENAI_MAX_OUTPUT_TOKENS`.
- Interfaces describe capabilities without an `I` prefix: `ChatRepository`, `LlmClient`, `SupportKnowledgeProvider`.
- Implementations use an `Impl` suffix when paired with an interface: `ChatRepositoryImpl`, `SettingsRepositoryImpl`.
- Presentation contracts use `FeatureUiState` and `FeatureUiEvent`; mutable state is private (`_uiState`) and exposed as read-only `StateFlow` (`uiState`).
- Use cases use verb-led names ending in `UseCase`: `SendMessageUseCase`, `SaveChatSettingsUseCase`.
- Retrofit transport types end in `Dto`; Room types end in `Entity`; conversion logic belongs in `mapper` packages.
- Dagger files use responsibility suffixes such as `Module`, `Component`, `Factory`, and `Key`.
- Tests end in `Test` and mirror the production package. Kotlin test names may use backticks for behavior descriptions.

## Established patterns

- Constructor injection with `@Inject`; bind interfaces to implementations with Dagger `@Binds` and construct shared objects with `@Provides`/`@Singleton`.
- Domain interfaces with data-layer implementations; keep Retrofit, Room, DataStore, Android `Context`, and DTO details outside feature ViewModels and domain consumers.
- Small use-case classes that delegate to repository interfaces.
- Immutable state updates via `data class.copy`; expose `StateFlow` through `asStateFlow()` and launch UI work in `viewModelScope`.
- Sealed classes/interfaces for closed state, event, result, and pending-action sets; exhaustive `when` expressions handle them.
- `Result<T>` for recoverable repository/service boundaries; validate inputs before external calls and map transport failures to controlled failures.
- Explicit entity/DTO/domain mapping rather than leaking storage or transport models across module boundaries.
- Calendar writes are two-phase: tool execution creates a `PendingCalendarAction`; only `confirm(...)` performs create/update/delete after permission checks.
- RAG indexing uses scanner → chunker → embedding client → storage/manifest; reuse `rag-core` in JVM tools.
- Tests use fakes for domain boundaries and MockWebServer for HTTP behavior; real OpenAI/VPS integration tests are opt-in through environment variables.

## Real commands

Run commands from the repository root on Windows. Use the narrowest relevant task first.

```powershell
# Compile or build Android
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug

# Android/JVM unit tests
.\gradlew.bat :feature:chat:testDebugUnitTest
.\gradlew.bat :core:domain:testDebugUnitTest
.\gradlew.bat :core:data:testDebugUnitTest
.\gradlew.bat :core:network:testDebugUnitTest
.\gradlew.bat :rag-core:test
.\gradlew.bat :developer-assistant:test
.\gradlew.bat test

# Main MCP server
npm ci --prefix mcp-server
npm test --prefix mcp-server
npm start --prefix mcp-server

# RAG indexer
.\gradlew.bat :rag-indexer:run
.\gradlew.bat :rag-indexer:validateIndex
.\gradlew.bat :rag-indexer:searchDemo -Pquery="rememberSaveable"

# Developer Assistant
.\gradlew.bat :developer-assistant:run --args="--project-root=."
.\gradlew.bat :developer-assistant:run --args="index-support-knowledge --project-root=. --knowledge-root=support-knowledge"

# Repository hygiene
git diff --check
git diff
git status --short
```

`core:network` contains environment-gated integration tests. `OpenAiIntegrationTest` requires `OPENAI_API_KEY`; `PrivateVpsIntegrationTest` requires the private VPS environment configuration. Do not turn these into unconditional external writes or claim they ran when prerequisites are absent.

## Five good examples to follow

1. `core/domain/src/main/java/com/aiassistant/core/domain/memory/TaskStateMachine.kt` — models allowed transitions with exhaustive `when` expressions and returns copied immutable state instead of mutating shared state.
2. `core/data/src/main/java/com/aiassistant/core/data/di/DataModule.kt` — keeps Dagger interface bindings in one composition boundary and provides singleton persistence objects with explicit migrations.
3. `core/data/src/main/java/com/aiassistant/core/data/mapper/ChatMessageMapper.kt` — isolates Room/domain conversion and preserves optional token metrics in both directions.
4. `feature/settings/src/main/java/com/aiassistant/feature/settings/presentation/viewmodel/SettingsViewModel.kt` — exposes read-only `StateFlow`, accepts sealed UI events, and runs persistence/connection work in `viewModelScope`.
5. `feature/chat/src/main/java/com/aiassistant/feature/chat/calendar/CalendarToolExecutor.kt` — validates permissions and arguments, represents writes as pending actions, and performs mutations only through the explicit confirmation path with a timeout.

## Project-specific anti-patterns to prohibit

1. Do not add dependencies from `feature:*` or `core:domain` to `core:data`/`core:network`, or expose Retrofit DTOs and Room entities outside their owning layers. Existing feature code consumes domain interfaces and models.
2. Do not edit `rag-indexer/input/project_sources` as if it were live code, and do not manually patch generated RAG indexes instead of updating source/generator logic.
3. Do not perform calendar create/update/delete directly from an LLM tool call or UI preview. Preserve `PendingCalendarAction` plus explicit `CalendarToolExecutor.confirm(...)`, permission checks, ambiguity handling, and write timeout.
4. Do not execute network, Room, file-system, or indexing work on the Android main thread. Preserve suspend APIs and the existing coroutine/`Dispatchers.IO` boundaries.
5. Do not hardcode, log, commit, or expose API keys or bearer tokens. Keep secret values in ignored local/environment configuration and ensure tests do not contact real external services unless explicitly enabled.

## Typical Kotlin file template

Use only the sections needed by the file. This template matches the repository's constructor injection, interface boundary, coroutine, and `Result` conventions.

```kotlin
package com.aiassistant.core.data.repository

import com.aiassistant.core.domain.repository.ExampleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExampleRepositoryImpl @Inject constructor(
    private val source: ExampleSource,
    private val mapper: ExampleMapper
) : ExampleRepository {

    override suspend fun load(id: String): Result<Example> = withContext(Dispatchers.IO) {
        if (id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("id must not be blank"))
        }

        runCatching {
            mapper.toDomain(source.load(id))
        }
    }

    private companion object {
        const val DEFAULT_LIMIT = 20
    }
}
```

For presentation files, keep `MutableStateFlow` private, expose `StateFlow` with `asStateFlow()`, update immutable state with `copy`, and launch suspend work in `viewModelScope`.

## Behavioral contracts

Before implementing user-facing, asynchronous, callback-based, or platform-integrated
behavior, define a concise behavioral contract.

For every user-visible action, identify:

- what the user expects the action to mean;
- which application or platform operation it invokes;
- who decides when the interaction starts;
- who decides when the interaction finishes;
- successful completion behavior;
- interruption and cancellation behavior;
- failure behavior;
- whether the output is persistent state or a one-time effect.

UI labels, icons, content descriptions, state names, and invoked operations must have
matching semantics.

Do not map a user-visible action such as Stop, Save, Retry, Confirm, Pause, or Cancel
to a semantically different operation.

Do not accept framework-default behavior as the intended product behavior without
explicitly comparing it with the user interaction contract.

## Interaction ownership

For every interactive feature, identify the interaction owner and completion owner.

Possible owners include:

- the user;
- the Android framework;
- an external service;
- a timeout;
- the application lifecycle.

For user-controlled interactions, explicit user completion is preferred over
framework heuristics unless the requested behavior explicitly allows automatic
completion.

The plan must state:

- who starts the interaction;
- who finishes the interaction;
- whether pauses are allowed;
- whether the platform may finish the interaction automatically;
- what happens if the platform cannot provide the required interaction model;
- whether a fallback, restart, aggregation, or different abstraction is necessary.

Do not claim that an interaction is manually controlled when the selected platform
API can still terminate it automatically.

## Resource ownership and lifecycle

Every stateful or platform resource must have one clearly identified owner.

Before implementation, identify:

- who creates the resource;
- which lifecycle the resource follows;
- which operation temporarily stops or cancels active work;
- which operation permanently releases the resource;
- whether the resource owner can outlive the current screen or Composable.

A Composable must not permanently destroy a dependency owned by a ViewModel.

UI lifecycle callbacks may request temporary stop or cancellation, but permanent
resource cleanup belongs to the resource owner, normally `ViewModel.onCleared()`,
a scoped component, repository, or service.

For Android platform integrations, explicitly analyze:

- configuration change;
- navigation away;
- application backgrounding;
- repeated start;
- user-requested stop;
- cancellation;
- cancellation followed immediately by restart;
- delayed callback after cancellation;
- delayed callback from an earlier operation after a new operation starts;
- permission denial and revocation;
- unavailable or failing platform providers.

## Platform API boundaries

Treat Android framework APIs, services, ContentProviders, Binder calls, and
callback-based integrations as failure-prone external boundaries.

Platform adapters must:

- keep Android framework types outside ViewModels when an abstraction is practical;
- identify and enforce thread requirements;
- convert expected error codes and framework failures into controlled results;
- handle permission races and service unavailability;
- ignore callbacks from obsolete operations;
- distinguish successful completion, stopping, cancellation, and destruction when
  the underlying API distinguishes them;
- document platform limitations that affect the requested UX.

A permission check before a framework call does not guarantee that the call cannot
fail.

When the selected platform API cannot guarantee the requested UX, stop planning and
present the limitation and viable repository-compatible alternatives before
implementation.

## Required state-transition analysis

For stateful, asynchronous, callback-based, permission-gated, or lifecycle-bound
features, the implementation plan must contain a state-transition table.

The table must cover all applicable transitions:

1. idle to active;
2. active to successful completion;
3. active to user-requested stop;
4. active to cancellation;
5. active to controlled failure;
6. active to lifecycle interruption;
7. cancellation to delayed callback;
8. cancellation to new operation;
9. new operation receiving a callback from the previous operation;
10. repeated start while already active;
11. owner cleanup while active;
12. provider-driven automatic completion;
13. user pause followed by continuation.

Each transition must identify:

- current state;
- trigger;
- invoked operation;
- next state;
- observable output;
- stale-callback behavior;
- required test or manual check.

## Required behavioral test matrix

For every new stateful or platform-integrated feature, tests must cover all
applicable items:

- successful start;
- successful completion;
- controlled failure;
- user stop;
- cancellation;
- duplicate start;
- cleanup while active;
- delayed callback after cancellation;
- delayed callback from operation A after operation B starts;
- unavailable dependency;
- permission denial;
- permanent permission denial;
- platform call throwing a controlled exception;
- automatic platform completion when the product expects explicit completion.

The plan and final report must mark every item as:

- covered by an automated test;
- covered by an instrumentation or manual check;
- not applicable, with a reason.

Compilation plus happy-path tests are not sufficient evidence for callback-based,
permission-gated, or lifecycle-bound behavior.

## Corrective review loop

Human approval authorizes the requested behavior, approved architecture, dependencies,
public contracts, module boundaries, and editable file scope.

A reviewer finding does not require new approval when the correction:

- makes the implementation conform to already approved behavior;
- preserves the approved architecture;
- introduces no dependency;
- changes no public contract;
- changes no module boundary;
- remains inside approved editable files.

New human approval is required only when the correction expands or changes behavior,
architecture, dependencies, public contracts, module boundaries, non-goals, or
editable scope.

After implementation, the main agent must run one reviewer-to-implementer corrective
loop for blocking in-scope findings:

1. reviewer inspects the first implementation;
2. main agent classifies findings as corrective or scope-changing;
3. implementer corrects all in-scope findings;
4. validation is repeated;
5. reviewer reviews the corrected diff;
6. main agent reports the difference between the first and second iterations.

Do not require the user to rewrite the feature prompt or enumerate implementation
defects for an in-scope corrective pass.

## Definition of Done

A change is done only when all applicable items below are true:

- The change is limited to the responsible module and respects the documented dependency direction.
- New behavior reuses domain interfaces, Dagger bindings, mappers, UI state/events, and existing generators rather than duplicating them.
- Tests cover changed behavior in the owning module, including failure/edge cases relevant to permissions, parsing, state transitions, persistence, or network mapping.
- The narrowest affected Gradle or Node test task passes; broader `test`/`assembleDebug` checks are run when the change crosses modules or affects application wiring.
- Android production code compiles with `:app:compileDebugKotlin` or the narrower affected compile task; APK-level changes pass `:app:assembleDebug`.
- `npm test --prefix mcp-server` passes for changes to `mcp-server`; Node services without configured tests are not reported as tested.
- Generated RAG artifacts are regenerated and validated with their owning tasks when source/indexing behavior changes.
- No real external service mutation occurs during automated tests; environment-gated integration checks are reported separately.
- `git diff --check`, `git diff`, and `git status --short` are inspected; unrelated user changes and secret files remain untouched.
- No required configured check is failing, and skipped checks, missing prerequisites, and remaining risks are stated explicitly.
- The implementation is checked against an explicit behavioral contract, not only
  against file scope and compilation.
- User-visible actions invoke operations with matching semantics.
- Interaction owner and completion owner are explicitly identified.
- Framework-driven automatic completion is verified against the intended UX.
- Platform limitations affecting the requested behavior are reported rather than
  hidden behind a nominally working implementation.
- Resource creation, cancellation, stopping, and permanent cleanup have one clear
  ownership model.
- Applicable lifecycle, restart, and stale-callback scenarios are covered by tests
  or explicitly reported manual checks.
- One corrective reviewer-to-implementer pass has been completed for blocking
  findings that remain inside the approved plan.

## When uncertain

Ask the user before:

- changing project architecture;
- introducing new dependencies;
- renaming public APIs;
- changing module boundaries;
- modifying more than one feature module;
- deleting code whose purpose is unclear.

## Execution loop

When the user requests an execution loop, read and follow:

```text
execution-loop/EXECUTION_LOOP.md
```

The task queue is located at:

```text
execution-loop/tasks.json
```

During an execution loop, process tasks sequentially in ascending `order`, starting with the first task whose status is `pending`.

Do not request confirmation or additional instructions between tasks.
