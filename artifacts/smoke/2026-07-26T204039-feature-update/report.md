# Android Smoke Test Report

## 1. Environment

- Tested revision: `8b3da64` on `day_38_new_feature`
- Device: Pixel 6 AVD, `emulator-5554` (`sdk_gphone64_x86_64`)
- Android: `16`
- Orientation: portrait
- Package/activity: `com.aiassistant` / `com.aiassistant.MainActivity`
- Build variant: `debug`
- Evidence: `artifacts/smoke/2026-07-26T204039-feature-update/`

See `environment.md` for preflight details.

## 2. Scenario summary

| Scenario | Result | Duration | Primary evidence |
|---|---|---:|---|
| SMOKE-01 Application launch | PASS | Not recorded | `SMOKE-01/01-launch-primary-screen.png`, `SMOKE-01/ui-state.xml` |
| SMOKE-02 Manual text input | BLOCKED | Not recorded | `SMOKE-02/automation-error.txt`, `SMOKE-02/01-input-automation-blocked.png`, `SMOKE-02/ui-state.xml` |
| SMOKE-03 Microphone permission granted | PASS | Not recorded | `SMOKE-03/01-microphone-permission-dialog.png`, `SMOKE-03/02-active-voice-state.png`, paired UI XML |
| SMOKE-04 Microphone permission denied | PASS | Not recorded | `SMOKE-04/01-permission-denied-explanation.png`, `SMOKE-04/02-stable-idle-input-usable.png`, paired UI XML |
| SMOKE-05 Existing text during voice input | BLOCKED | Not recorded | `SMOKE-05/blocker.txt`, `SMOKE-05/01-primary-state-no-audio-source.png`, `SMOKE-05/ui-state.xml` |
| SMOKE-06 Clear-chat confirmation | PASS | Not recorded | `SMOKE-06/01-message-sticky-rag-off.png` through `SMOKE-06/06-final-rag-off.png`, paired UI XML |

## 3. Detailed execution

### SMOKE-01 — PASS

1. Stopped and launched `com.aiassistant` through mobile MCP.
2. Waited 2.5 seconds for loading.
3. UI hierarchy exposed `AI Assistant`, editable main input with placeholder
   `Type your message...`, and voice action `Start voice input`.
4. The primary screen displayed `Start a conversation with AI`; no crash or
   unrecoverable error was present.

Assertion evidence: `SMOKE-01/ui-state.xml`.
Screenshot: `SMOKE-01/01-launch-primary-screen.png`.

### SMOKE-02 — BLOCKED

1. Focused the main `EditText`; hierarchy confirmed it was focused.
2. Attempted to enter the exact Russian scenario text through mobile input.
3. Android's input shell failed twice with an `InputShellCommand.sendText`
   null-pointer error. No test text entered the field.
4. Dependent assertions about exact content and preservation after keyboard
   dismissal were not executed.

Earliest blocked step: step 3.
Expected: complete exact Russian text visible in the input.
Actual: automation could not inject the text.
Evidence: `SMOKE-02/automation-error.txt`, screenshot and UI XML in `SMOKE-02/`.

### SMOKE-03 — PASS

1. Revoked `android.permission.RECORD_AUDIO`, restarted the app, and tapped
   `Start voice input`.
2. Hierarchy showed Android's microphone dialog:
   `Разрешить приложению AI Assistant записывать аудио` with grant/deny actions.
3. Chose `При использовании приложения`.
4. After focus returned, hierarchy showed the application screen and exact
   active-state action `Stop voice input`; the app remained responsive.

Evidence: both screenshots and both UI XML files in `SMOKE-03/`.

### SMOKE-04 — PASS

1. Stopped voice input, revoked microphone permission, restarted the app, and
   tapped `Start voice input`.
2. Denied the Android permission with `Запретить`.
3. The app displayed a controlled dialog titled `Voice input` with
   `Microphone permission is required for voice input.`
4. Dismissed it with `OK`; hierarchy showed `Start voice input`, not an active
   listening state.
5. Tapped the main `EditText`; hierarchy confirmed it was focused, demonstrating
   manual input remained usable.

Evidence: both screenshots and both UI XML files in `SMOKE-04/`.

### SMOKE-05 — BLOCKED

The configured automation provided no real audio input and the app exposes no
documented smoke-test injection mechanism for a final speech-recognition result.
Execution stopped before dependent recognition assertions, as required by the
scenario. Microphone permission behavior from SMOKE-03/04 was not treated as
recognition-result coverage.

Evidence: `SMOKE-05/blocker.txt`, screenshot and UI XML in `SMOKE-05/`.

### SMOKE-06 — PASS

1. Recorded the initial strategy as `Sliding Window`; RAG was already `RAG OFF`.
2. Selected `Sticky Facts`. Hierarchy showed its control disabled/selected and
   displayed the Sticky Facts panel.
3. Opened `More`; hierarchy showed exact `RAG OFF`.
4. Entered and sent `SMOKE-06 clear confirmation 2026-07-26`.
5. Hierarchy found the complete unique user message immediately; no assistant
   response was required.
6. Tapped the top-bar action with exact content description `Clear Chat`.
7. The modal hierarchy showed exact title `Очистить историю сообщений?` and
   exact actions `Отмена` and `Очистить`.
8. Tapped the clickable `Отмена` action. The dialog disappeared, the complete
   unique message remained, `Sticky Facts` remained selected, and `More` still
   showed `RAG OFF`.
9. Opened `Clear Chat` again and re-verified the exact title/actions.
10. Tapped the clickable `Очистить` action. The dialog disappeared, the unique
    message was absent, and exact empty state `Start a conversation with AI`
    was visible.
11. `Sticky Facts` remained selected and `More` still showed `RAG OFF`.
12. Restored the initially recorded `Sliding Window` strategy. RAG required no
    restoration because it was initially and finally off.

Strong assertions came from live mobile MCP hierarchy checks and the saved UI
XML. Screenshots:

- `SMOKE-06/01-message-sticky-rag-off.png`
- `SMOKE-06/02-clear-confirmation-dialog.png`
- `SMOKE-06/03-after-cancel-message-preserved.png`
- `SMOKE-06/04-rag-off-after-cancel.png`
- `SMOKE-06/05-after-confirm-empty-sticky.png`
- `SMOKE-06/06-final-rag-off.png`

The assistant request entered `Status: Failed` because the backend did not
complete, but the scenario explicitly did not require an assistant response;
the user message was observable and both clear-dialog branches were fully
executed.

## 4. Failures and blockers

No observable production failure was found in the executed assertions.

- SMOKE-02 blocker: exact Russian input injection failed in Android's input
  shell. Suspected area: test automation/device input boundary. Confidence:
  high. Recommended investigation: provide a Unicode-capable mobile input
  method or a documented clipboard/IME mechanism, then rerun SMOKE-02.
- SMOKE-05 blocker: no real audio or documented recognition-result test
  mechanism. Suspected area: environment prerequisite, not application code.
  Confidence: high. Recommended investigation: rerun with a supported real
  audio feed or an explicitly documented debug test mechanism.

No crash/frozen transition occurred, so application logs were not required by
the failure-evidence rules.

## 5. Residual risks

- Exact Russian manual typing and post-keyboard preservation remain unverified
  because SMOKE-02 was blocked.
- Final speech recognition, append-once behavior, separators, and return to
  non-listening state remain unverified because SMOKE-05 was blocked.
- The backend request used to create the SMOKE-06 visible user message failed,
  but this did not prevent or weaken any required clear-chat assertion.
- Screenshot assertions are supplemented by hierarchy evidence; no required
  SMOKE-06 assertion was visual-only.

## 6. Final result

`PASS_WITH_BLOCKED_SCENARIOS`

Four scenarios passed all required executed assertions. Two scenarios were
blocked by explicit environment/automation prerequisites and were not reported
as PASS.
