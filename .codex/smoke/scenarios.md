# Android Smoke Scenarios

## General rules

* Execute scenarios in identifier order unless a different scope is requested.
* Restore the documented preconditions before every scenario.
* Capture a screenshot after every meaningful step.
* Use UI hierarchy assertions when available.
* Record each scenario as `PASS`, `FAIL`, `BLOCKED`, or `SKIPPED`.
* Do not modify production code during smoke execution.
* Do not assume that real speech input is available.
* Replace generic control descriptions with real discovered UI labels,
  content descriptions, or stable selectors in the execution report.

---

## SMOKE-01: Application launch

### Purpose

Verify that the application starts successfully and displays its primary user
interface.

### Preconditions

* A debug build is installed.
* The application process is stopped.
* No system dialog is covering the application.

### Steps

1. Launch the application.
2. Wait until the initial loading state completes.
3. Verify that the primary screen is visible.
4. Verify that the main text input is visible.
5. Verify that the voice or microphone action is visible.
6. Capture the final state.

### Expected result

* The application launches without crashing.
* No unrecoverable error is displayed.
* The main text input is available.
* The voice-input action is available.

### Failure evidence

When the application does not reach the expected screen, capture:

* the final screenshot;
* current UI hierarchy;
* application logs;
* package and launch information.

---

## SMOKE-02: Manual text input

### Purpose

Verify that the main text field accepts and preserves manually entered text.

### Preconditions

* The application is on its primary screen.
* The main text field is empty.
* No permission or system dialog is visible.

### Test data

```text
Купить молоко
```

### Steps

1. Tap the main text input.
2. Verify that the input receives focus.
3. Enter `Купить молоко`.
4. Verify that the complete text is visible in the input.
5. Dismiss the software keyboard.
6. Verify that the entered text remains unchanged.
7. Capture the final state.

### Expected result

* The field accepts the complete test text.
* The text is not truncated, duplicated, or cleared.
* Dismissing the keyboard does not remove the text.
* The application remains responsive.

---

## SMOKE-03: Microphone permission granted

### Purpose

Verify that the application requests microphone permission and remains in a
valid state after permission is granted.

### Preconditions

* The application is on its primary screen.
* Microphone permission is not currently granted.
* The permission can be requested again in the current device state.

### Steps

1. Tap the voice or microphone action.
2. Verify that an Android microphone permission dialog appears.
3. Capture the permission dialog.
4. Grant microphone permission.
5. Wait for the application to regain focus.
6. Verify that the application does not crash.
7. Verify that the UI represents either:

    * an active voice-input state; or
    * a stable state with a controlled provider message.
8. Capture the final state.

### Expected result

* The microphone permission request is displayed.
* Permission can be granted.
* The application remains responsive.
* The application does not remain in an invalid loading state.
* Any unavailable-provider condition is handled as a controlled result.

### Environment note

Resetting permission may require an ADB command derived from the confirmed
application package name. Do not guess the package name.

---

## SMOKE-04: Microphone permission denied

### Purpose

Verify that denial of microphone permission does not crash the application or
leave it in a false active state.

### Preconditions

* The application is on its primary screen.
* Microphone permission is not currently granted.
* The permission can be requested again in the current device state.

### Steps

1. Tap the voice or microphone action.
2. Verify that the Android microphone permission dialog appears.
3. Deny microphone permission.
4. Wait for the application to regain focus.
5. Verify that the application remains responsive.
6. Verify that the application is not shown as actively listening.
7. Verify that a controlled explanation, error state, or stable idle state is
   displayed.
8. Verify that manual text input remains usable.
9. Capture the final state.

### Expected result

* Permission denial does not crash the application.
* Voice input does not remain active.
* The user can continue using the application.
* The UI does not falsely indicate successful permission or active recording.

---

## SMOKE-05: Existing text during voice input

### Purpose

Verify that existing manually entered text is preserved when a final
voice-recognition result is received.

### Preconditions

* The application is on its primary screen.
* Microphone permission is granted.
* A compatible speech-recognition provider is available.
* Real audio input or an explicitly documented test input mechanism is
  available.
* The main text input is empty.

### Test data

Existing text:

```text
Купить молоко
```

Voice phrase:

```text
и хлеб
```

Expected combined text:

```text
Купить молоко и хлеб
```

### Steps

1. Enter `Купить молоко` into the main text input.
2. Verify that the existing text is visible.
3. Start voice input.
4. Verify that the application enters an active voice-input state.
5. Provide the phrase `и хлеб` through real audio input or the documented test
   mechanism.
6. Wait for the final recognition result.
7. Verify that the text field contains `Купить молоко и хлеб`.
8. Verify that `и хлеб` appears exactly once.
9. Verify that there is a correct separator between the original and recognized
   text.
10. Capture the final state.

### Expected result

* The original text is preserved.
* The final recognition result is appended exactly once.
* Words are not joined together.
* No unnecessary leading or duplicate whitespace appears.
* The application returns to a stable non-listening state after completion.

### Blocking conditions

Mark this scenario `BLOCKED`, not `FAIL`, when:

* no real audio can be supplied;
* no compatible speech-recognition provider is installed;
* the provider requires unavailable network access;
* the emulator does not support the required microphone input;
* the application has no documented test mechanism for recognition results.

Do not claim that voice recognition was verified by testing only the microphone
permission dialog.

---

## SMOKE-06: Clear-chat confirmation

### Purpose

Verify that clearing the current chat requires explicit confirmation, that
cancelling preserves the message history, and that neither dialog action
changes the selected Context Strategy or the unrelated RAG setting.

### Preconditions

* The application is on its primary screen.
* No permission, system, or application dialog is visible.
* The application can display a user message in the current chat.
* Record the initial selected Context Strategy and RAG setting before changing
  them, so they can be restored after the scenario if required.

### Test data

Unique message:

```text
SMOKE-06 clear confirmation 2026-07-26
```

Scenario settings:

```text
Context Strategy: Sticky Facts
RAG: OFF
```

### Steps

1. Select `Sticky Facts` in the `Context Strategy` control.
2. Open the `More` menu and set the RAG switch to off.
3. Reopen the `More` menu and verify that it displays `RAG OFF`.
4. Enter `SMOKE-06 clear confirmation 2026-07-26` in the main text input and
   tap the `Send message` action.
5. Verify that the complete unique message is visible in the current chat.
   Do not require an assistant response for this scenario.
6. Tap the top-app-bar action with content description `Clear Chat`.
7. Verify that a modal dialog is displayed with the exact title
   `Очистить историю сообщений?`.
8. Verify that the dialog displays both exact actions: `Отмена` and
   `Очистить`.
9. Capture the dialog before choosing an action.
10. Tap `Отмена`.
11. Verify that the confirmation dialog closes.
12. Verify that the complete unique message remains visible in the current
    chat.
13. Verify that `Sticky Facts` is still the selected Context Strategy.
14. Open the `More` menu and verify that it still displays `RAG OFF`, then
    dismiss the menu.
15. Tap the top-app-bar action with content description `Clear Chat` again.
16. Verify the exact dialog title and both exact actions again.
17. Tap `Очистить`.
18. Verify that the confirmation dialog closes.
19. Verify that the unique message is no longer present and that the empty
    state `Start a conversation with AI` is displayed.
20. Verify that `Sticky Facts` is still the selected Context Strategy.
21. Open the `More` menu and verify that it still displays `RAG OFF`.
22. Capture the final state and restore the settings recorded in the
    preconditions when required by subsequent scenarios.

### Expected result

* Tapping `Clear Chat` does not clear the current chat immediately.
* The dialog title and both actions exactly match the required Russian labels.
* `Отмена` dismisses the dialog without removing the unique message.
* `Очистить` dismisses the dialog and clears the current chat.
* The selected `Sticky Facts` Context Strategy remains unchanged after both
  cancellation and confirmation.
* The unrelated RAG setting remains `RAG OFF` after both cancellation and
  confirmation.
* The application remains responsive throughout the interaction.

### Failure evidence

When any assertion fails, capture:

* the state immediately before tapping `Clear Chat`;
* the dialog and its UI hierarchy;
* the state after `Отмена`;
* the state after `Очистить`;
* the visible Context Strategy and `More` menu RAG label;
* application logs for a crash, frozen UI, or unexpected state transition.
