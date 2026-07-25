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
