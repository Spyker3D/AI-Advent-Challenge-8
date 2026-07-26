FILE: .codex/smoke/README.md

# Android Smoke Testing

This directory contains user-facing Android smoke scenarios executed through
the configured mobile MCP server.

## Purpose

Smoke testing verifies that the application's most important user paths work on
a real Android device or emulator after the application has been built and
installed.

Smoke tests complement, but do not replace, unit and integration tests.

* Unit and integration tests verify business logic in a controlled environment.
* Smoke tests verify observable application behavior through the Android UI.

## Directory structure

```text
.codex/
└── smoke/
    ├── README.md
    └── scenarios.md

artifacts/
└── smoke/
    └── <run-id>/
        ├── environment.md
        ├── SMOKE-01/
        ├── SMOKE-02/
        └── report.md
```

The `artifacts/smoke` directory is used for generated evidence and reports.

A run identifier should contain the date, time, or tested revision, for example:

```text
2026-07-26-debug
```

## Required environment

Before running smoke tests, verify:

1. Android SDK and ADB are available.
2. At least one emulator or physical device is connected.
3. The mobile MCP server is configured in Codex.
4. The required application build is installed.
5. The application package name is confirmed from the repository.
6. Required permissions and test data can be prepared.
7. The tested Git revision is recorded.

Useful environment check:

```bash
adb devices
```

Do not assume a package name, activity, emulator identifier, APK location, or
Gradle task. Determine them from the current repository and environment.

## Running the scenarios

Start Codex from the repository root and provide the following task:

```text
Use the `smoke-tester` agent.

Execute the scenarios from `.codex/smoke/scenarios.md` through the configured
mobile MCP server.

Determine the application package and available device from the current
repository and environment.

Capture a screenshot after every meaningful step and save all evidence under:

artifacts/smoke/<run-id>/

Return an Android Smoke Test Report.

Do not modify production code or tests.
```

A subset can be requested by scenario identifier:

```text
Execute SMOKE-01 and SMOKE-02 from `.codex/smoke/scenarios.md`.
```

## Scenario result meanings

### PASS

All required steps were executed and every expected result was verified.

### FAIL

The application produced observable behavior that contradicts the expected
result.

### BLOCKED

The scenario could not be completed because of a missing prerequisite,
unsupported automation, unavailable device, external provider, permission
state, test data, network, or environment problem.

### SKIPPED

The scenario was intentionally excluded from the requested scope.

## Evidence requirements

For every scenario:

* capture the initial state;
* capture the result of every meaningful user action;
* capture the final state;
* inspect the UI hierarchy for important assertions when possible;
* collect relevant logs after a failure;
* record the exact failing step;
* preserve evidence from previous runs.

Recommended screenshot naming:

```text
01-initial-state.png
02-input-focused.png
03-text-entered.png
04-final-state.png
```

A scenario must not be marked PASS only because the final screenshot looks
correct. Required assertions must be verified using the strongest available
evidence.

## Voice-to-text limitation

Microphone permission and microphone-button behavior can normally be automated.

Real speech-recognition results may depend on:

* physical or virtual microphone input;
* Android recognition provider;
* Google Play Services;
* network access;
* device language;
* provider timeouts and heuristics.

A scenario requiring actual speech must be marked BLOCKED when the required
audio or provider is unavailable.

Do not report a successful recognition result unless real input was processed
or the application contains an explicit documented debug/test mechanism.

## Updating scenarios after a feature

When a new feature changes user-visible behavior:

1. Inspect the feature diff.
2. Identify new or changed user paths.
3. Update relevant scenarios.
4. Preserve unaffected regression scenarios.
5. Add a new scenario only when it verifies a distinct user outcome.
6. Run both the changed scenarios and the core launch/input scenarios.
7. Store evidence in a new run directory.
8. Include the tested revision in the report.

Do not delete an existing scenario merely because it currently fails.
