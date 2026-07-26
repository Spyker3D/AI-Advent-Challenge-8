# Android Smoke Test Report

## 1. Environment

- Tested revision: `b4f3d18d5ded543d057905491b3155d454e8d0ed`
- Device: `emulator-5554` (`sdk_gphone64_x86_64`)
- Android: 16 / API 36, portrait
- Package: `com.aiassistant`
- Launcher: `com.aiassistant.MainActivity`
- Build variant: debug
- Evidence: `artifacts/smoke/2026-07-26T182951-b4f3d18-level2`

## 2. Scenario summary

| Scenario | Result | Earliest incomplete/failing step | Evidence |
|---|---|---|---|
| SMOKE-01 Application launch | PASS | None | `SMOKE-01/01-final-primary-screen.png`, `SMOKE-01/ui-state.xml` |
| SMOKE-02 Manual text input | BLOCKED | Step 5, dismiss software keyboard | `SMOKE-02/01-initial-empty.png`, `SMOKE-02/02-text-entered-focused.png`, `SMOKE-02/ui-text-entered.xml` |
| SMOKE-03 Microphone permission granted | PASS | None | `SMOKE-03/02-permission-dialog.png`, `SMOKE-03/03-active-after-grant.png`, corresponding UI XML |
| SMOKE-04 Microphone permission denied | PASS | None | `SMOKE-04/02-permission-dialog.png`, `SMOKE-04/03-controlled-denial-message.png`, `SMOKE-04/04-manual-input-usable.png`, corresponding UI XML |
| SMOKE-05 Existing text during voice input | BLOCKED | Step 5, provide real spoken phrase | `SMOKE-05/02-existing-text.png`, `SMOKE-05/03-active-before-real-audio-blocker.png`, corresponding UI XML, `SMOKE-05/logs.txt` |

## 3. Detailed execution

### SMOKE-01: Application launch — PASS

1. Stopped `com.aiassistant`, then launched the confirmed `com.aiassistant/.MainActivity`.
   Expected: application starts without crashing.
   Actual: primary Compose UI loaded and remained responsive.
   Evidence: `SMOKE-01/01-final-primary-screen.png`, `SMOKE-01/ui-state.xml`.
2. Verified primary controls through the live accessibility hierarchy.
   Expected: main input and voice action are visible.
   Actual: an `EditText` was present at hierarchy index 38 and a control with content description `Start voice input` was present at index 36. No unrecoverable error was exposed.

### SMOKE-02: Manual text input — BLOCKED

1. Verified the primary screen and empty main `EditText`.
   Evidence: `SMOKE-02/01-initial-empty.png`, `SMOKE-02/ui-initial.xml`.
2. Focused the main input and entered the documented Unicode text using clipboard paste because Android 16 `input text` returned an ADB `NullPointerException` for Cyrillic.
   Expected: `Купить молоко` is visible once and complete.
   Actual: live hierarchy exposed focused `EditText text="Купить молоко"` exactly.
   Evidence: `SMOKE-02/02-text-entered-focused.png`, `SMOKE-02/ui-text-entered.xml`.
3. Attempted the documented keyboard-dismiss step.
   Expected: software keyboard dismisses and text remains unchanged.
   Actual: the emulator did not expose a software keyboard. BACK navigated to Home rather than dismissing an IME; relaunch then did not expose the draft. After enabling the emulator's show-IME setting and repeating entry, no software keyboard appeared. The required post-dismiss assertion therefore could not be executed reliably.
   Classification: automation/environment BLOCKED, not an application contradiction. Text acceptance itself was verified.

### SMOKE-03: Microphone permission granted — PASS

1. Reset `android.permission.RECORD_AUDIO` to requestable, restarted the app, and verified `Start voice input`.
   Evidence: `SMOKE-03/01-initial-permission-requestable.png`.
2. Tapped `Start voice input`.
   Expected: Android microphone permission dialog.
   Actual: Android dialog exposed `permission_message` and buttons `При использовании приложения`, `Только в этот раз`, and `Запретить`.
   Evidence: `SMOKE-03/02-permission-dialog.png`, `SMOKE-03/ui-permission-dialog.xml`.
3. Granted foreground microphone permission and waited.
   Expected: responsive active voice state or controlled provider result.
   Actual: app regained focus without crash; hierarchy exposed `Stop voice input`, proving a stable active state.
   Evidence: `SMOKE-03/03-active-after-grant.png`, `SMOKE-03/ui-active-after-grant.xml`.

### SMOKE-04: Microphone permission denied — PASS

1. Reset microphone permission to requestable, restarted, tapped `Start voice input`, and verified the Android dialog.
   Evidence: `SMOKE-04/01-initial-permission-requestable.png`, `SMOKE-04/02-permission-dialog.png`, `SMOKE-04/ui-permission-dialog.xml`.
2. Tapped the live `permission_deny_button`.
   Expected: app remains responsive, does not listen, and shows controlled idle/error behavior.
   Actual: app displayed its controlled `Voice input` dialog with `Microphone permission is required for voice input.`
   Evidence: `SMOKE-04/03-controlled-denial-message.png`, `SMOKE-04/ui-controlled-denial-message.xml`.
3. Dismissed the controlled message and verified `Start voice input` (not `Stop voice input`). Focused the main field and entered `manual`.
   Expected: manual input remains usable.
   Actual: hierarchy exposed `EditText text="manual"` and the idle `Start voice input` action.
   Evidence: `SMOKE-04/04-manual-input-usable.png`, `SMOKE-04/ui-manual-input-usable.xml`.

### SMOKE-05: Existing text during voice input — BLOCKED

1. Granted microphone permission, restarted the app, and restored the documented existing text.
   Expected: `Купить молоко` is visible exactly.
   Actual: live hierarchy exposed `EditText text="Купить молоко"`.
   Evidence: `SMOKE-05/01-initial-permission-granted.png`, `SMOKE-05/02-existing-text.png`, `SMOKE-05/ui-existing-text.xml`.
2. Tapped `Start voice input`.
   Expected: active recognition state.
   Actual: hierarchy exposed `Stop voice input`; existing text remained unchanged.
   Evidence: `SMOKE-05/03-active-before-real-audio-blocker.png`, `SMOKE-05/ui-active-before-real-audio-blocker.xml`.
3. Could not provide the real spoken phrase `и хлеб`.
   Expected: real audio reaches the provider, a final result is received, and the field becomes exactly `Купить молоко и хлеб`.
   Actual: the configured mobile MCP/emulator automation exposes no real microphone audio injection or documented recognition-result test mechanism. No synthetic recognition result was used. Steps 5–10 and the exact-once assertion were not executed.
   Evidence: established permission/provider/text preconditions above; recent device logs in `SMOKE-05/logs.txt`.

## 4. Failures and blockers

- SMOKE-02 earliest blocked step: step 5. This is a UI automation/emulator IME limitation. The field accepted exact Cyrillic input, but the required keyboard-dismiss transition could not be produced. Suspected repository area: none established. Confidence: high that the blocker is environmental; no application root cause claimed. Recommended investigation: rerun on a device/emulator configuration that presents a software keyboard to automation.
- SMOKE-05 earliest blocked step: step 5. Real audio input is unavailable through the configured automation, while permission, provider activation, and existing text were verified. Suspected repository area: none; no failure was observed. Confidence: high. Recommended investigation: rerun on a physical device with real spoken Russian audio, or use an explicitly documented application test hook if one is added in a separate approved change.

## 5. Residual risks

- SMOKE-02 post-keyboard-dismiss preservation remains unverified.
- SMOKE-05 provider final-result delivery, exact append-once behavior, spacing, and stable non-listening completion remain unverified.
- Provider/network/language behavior was not claimed as tested beyond observable recognition start.
- Visual screenshots are backed by UI hierarchy assertions for all important exposed controls and text.

## 6. Final result

`PASS_WITH_BLOCKED_SCENARIOS`

Three scenarios passed. Two independent scenarios were blocked by explicit automation/environment prerequisites; no scenario failed with contradictory application behavior.
