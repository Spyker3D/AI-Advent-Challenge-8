# AI Assistant Project Instructions

## Scope and source of truth

These instructions apply to the whole repository. Base changes on active source and build configuration, not on generated artifacts or copied snapshots.

- Treat `settings.gradle.kts`, module `build.gradle.kts` files, `package.json` files, active source, and tests as authoritative.
- Do not edit `rag-indexer/input/project_sources`; it is a copied indexing input, not active application source.
- Do not hand-edit generated indexes and reports under `rag-indexer/output`, `app/src/main/assets/rag`, `.developer-assistant`, or `.support-assistant` unless the task explicitly concerns generated output. Use their owning generator instead.
- Never read, print, or modify secrets in `local.properties`. The build reads `OPENAI_API_KEY`, `PRIVATE_VPS_BASE_URL`, `PRIVATE_VPS_API_KEY`, and `PRIVATE_VPS_MODEL` from that untracked file.

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
│                       └──> core:ui
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

## When uncertain

Ask the user before:

- changing project architecture;
- introducing new dependencies;
- renaming public APIs;
- changing module boundaries;
- modifying more than one feature module;
- deleting code whose purpose is unclear.
