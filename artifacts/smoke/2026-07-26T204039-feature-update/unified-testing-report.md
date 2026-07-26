# Unified Testing Report — FEATURE_UPDATE

## Scope

- Feature: clear-chat confirmation dialog
- Branch: `day_38_new_feature`
- Revision: `8b3da64`
- Device: Pixel 6 AVD (`emulator-5554`), Android 16
- Package: `com.aiassistant`
- Build variant: `debug`

## Overall verdict

`PASS_WITH_LIMITATIONS`

The new clear-chat confirmation behavior passed physical end-to-end smoke
validation. The overall result is not an unconditional pass because one
unrelated feature unit test remains failing, two pre-existing smoke scenarios
were blocked by environment/automation prerequisites, and the focused Compose
instrumentation tests were compiled but not executed.

## Changed testing artifacts

- `.codex/smoke/scenarios.md`
  - Added `SMOKE-06: Clear-chat confirmation`.
  - Preserved SMOKE-01 through SMOKE-05.

No unit or integration tests were added or changed during this FEATURE_UPDATE.
The existing focused Compose instrumentation test is the appropriate automated
layer for the transient dialog behavior; domain, persistence, and settings
semantics were not changed.

## Code-test results

| Command | Result | Classification |
|---|---|---|
| `:feature:chat:compileDebugAndroidTestKotlin` | PASS after sandbox retry | Initial sandbox failure: `ENVIRONMENT_BLOCKER` |
| `:feature:chat:testDebugUnitTest` | 38/39 passed | One `PRE_EXISTING_FAILURE` |

The remaining unit failure is
`CalendarToolDefinitionsTest > parses list tool arguments`, caused by the
existing malformed escaped JSON fixture. It is unrelated to the clear-chat
feature.

`ClearChatConfirmationDialogTest` compiled successfully but was not executed on
the device. Its assertions must therefore not be reported as passing runtime
tests.

## Build and installation

| Operation | Result |
|---|---|
| `:app:assembleDebug` | PASS after sandbox/Kotlin-daemon retry |
| `:app:installDebug` | PASS; installed on one Pixel 6 AVD |

## Android smoke results

| Scenario | Result | Notes |
|---|---|---|
| SMOKE-01 Application launch | PASS | Primary chat UI and input controls verified |
| SMOKE-02 Manual text input | BLOCKED | Android input shell could not inject exact Russian text |
| SMOKE-03 Microphone permission granted | PASS | Permission and active-state behavior verified |
| SMOKE-04 Microphone permission denied | PASS | Controlled denial and usable manual input verified |
| SMOKE-05 Existing text during voice input | BLOCKED | No real audio or documented recognition-result injection |
| SMOKE-06 Clear-chat confirmation | PASS | Both dialog branches and setting invariants verified |

SMOKE-06 physically verified:

- tapping `Clear Chat` opens the confirmation dialog;
- the exact title `Очистить историю сообщений?`;
- exact actions `Отмена` and `Очистить`;
- Cancel closes the dialog and preserves the unique message;
- Confirm closes the dialog, removes the message, and shows
  `Start a conversation with AI`;
- `Sticky Facts` remains selected after both branches;
- `RAG OFF` remains unchanged after both branches.

The dialog screenshot is the reliable exact-label artifact because the saved
dialog XML contains encoding-corrupted Russian text.

## Evidence

- Smoke report: `report.md`
- Environment: `environment.md`
- Dialog screenshot:
  `SMOKE-06/02-clear-confirmation-dialog.png`
- Cancel/message preservation:
  `SMOKE-06/03-ui-state-after-cancel.xml`
- RAG after Cancel:
  `SMOKE-06/04-ui-state-rag-after-cancel.xml`
- Confirm/empty state:
  `SMOKE-06/05-ui-state-after-confirm.xml`
- RAG after Confirm:
  `SMOKE-06/06-ui-state-final-rag.xml`

All evidence was written to the new directory
`artifacts/smoke/2026-07-26T204039-feature-update/`; previous evidence was not
overwritten.

## Remaining limitations

- Focused Compose instrumentation tests were compiled but not executed.
- Exact Russian manual input remains unverified in SMOKE-02.
- Final speech-recognition merging remains unverified in SMOKE-05.
- The affected feature unit task is not fully green because of the unrelated
  pre-existing calendar fixture failure.
- `.codex/profiles/testing.md` was already modified in the working tree and is
  unrelated to this FEATURE_UPDATE.
