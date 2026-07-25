# Testing Profile

## Purpose

Use this profile to perform two levels of repository-aware validation:

1. **Level 1 — Code Testing**

    * discover insufficiently tested production business logic;
    * add focused unit or integration tests;
    * execute relevant test tasks;
    * report results and uncovered risks.

2. **Level 2 — Android Smoke Testing**

    * execute documented user scenarios through the configured mobile MCP server;
    * interact with a real Android emulator or physical device;
    * verify observable UI behavior;
    * capture screenshots and diagnostic evidence;
    * report passed, failed, blocked, and skipped scenarios.

This profile coordinates specialized agents and produces one unified testing
report.

---

## Invocation

Example:

```text
Work strictly according to `.codex/profiles/testing.md`.

Run both testing levels for the current repository.
```

A narrower scope may be supplied:

```text
Work strictly according to `.codex/profiles/testing.md`.

Run Level 1 only.
Find and test at least three insufficiently covered production files.
```

```text
Work strictly according to `.codex/profiles/testing.md`.

Run Level 2 only.
Execute scenarios SMOKE-01 through SMOKE-04.
```

When the user does not explicitly limit the scope, execute both levels.

---

## Agents

This workflow uses the following specialized agents:

* `researcher`

    * studies the repository;
    * identifies architecture, affected modules, existing tests, build commands,
      application identifiers, and testing constraints;
    * does not modify files.

* `test-writer`

    * selects meaningful production behavior;
    * creates or updates unit and integration tests;
    * runs confirmed Gradle test tasks;
    * reports test results and failures.

* `smoke-tester`

    * executes Android smoke scenarios through the configured mobile MCP server;
    * captures screenshots, UI state, and logs;
    * does not modify production code or tests.

* `reviewer`

    * verifies that test selection, assertions, execution evidence, and conclusions
      are supported;
    * identifies missing coverage and unreliable results;
    * issues the final verdict.

Do not substitute one agent for another when the required role is available.

---

## Global invariants

Throughout the workflow:

* Read every applicable `AGENTS.md`.
* Preserve unrelated user changes.
* Do not create commits.
* Do not stage or unstage files.
* Do not reset, clean, or rewrite Git history.
* Do not change dependencies without demonstrated necessity.
* Do not weaken assertions to obtain passing tests.
* Do not disable or delete failing tests.
* Do not claim that a command or scenario passed unless it was actually
  executed and its result was inspected.
* Do not invent Gradle tasks, package names, activities, selectors, device
  identifiers, test results, screenshots, or logs.
* Separate application defects from test implementation errors and environment
  blockers.
* Keep code-test changes focused on testing.
* During smoke execution, production code and test code are read-only.
* Store generated smoke evidence only in the designated evidence directory.

---

# Workflow

## Stage 1 — Scope

Determine the requested testing scope.

Possible scopes:

* `LEVEL_1`

    * unit and integration tests only;

* `LEVEL_2`

    * Android smoke scenarios only;

* `FULL`

    * both code tests and Android smoke scenarios;

* `CHANGE_VALIDATION`

    * test the current branch, diff, feature, bug fix, or pull request;

* `SMOKE_UPDATE`

    * update scenarios for a newly implemented feature and rerun validation.

When the user does not specify a narrower scope, use `FULL`.

Record:

* requested objective;
* current Git branch or revision when available;
* changed files when the request concerns a diff;
* required minimum number of production files for Level 1;
* requested smoke scenario identifiers;
* whether scenario updates are allowed.

Do not begin implementation before the scope is clear enough to test observable
behavior.

---

## Stage 2 — Repository Research

Delegate repository investigation to `researcher`.

The researcher must inspect:

1. Repository structure and Android modules.
2. Applicable `AGENTS.md` instructions.
3. Gradle configuration and available test source sets.
4. Existing unit, integration, instrumentation, and UI tests.
5. Test frameworks and utilities already used by the project.
6. Production files containing meaningful business behavior.
7. Existing coverage gaps that can be inferred from source and tests.
8. Application package name and build variants.
9. Confirmed build and test commands.
10. Existing smoke documentation.
11. Current Git diff when validation is change-based.
12. Android-specific execution constraints.

For `LEVEL_1` or `FULL`, the research report must propose meaningful production
test targets.

For `LEVEL_2` or `FULL`, the research report must identify:

* the application package;
* relevant screen entry point;
* likely stable UI controls;
* required permissions;
* device or provider dependencies;
* scenario preconditions;
* confirmed APK build/install path when available.

The researcher must distinguish:

* repository evidence;
* inference;
* unknowns;
* environment prerequisites.

### Research gate

Proceed only when the workflow has:

* confirmed repository testing conventions;
* confirmed relevant commands or explained why they cannot be confirmed;
* meaningful test targets for Level 1 when applicable;
* sufficient application and scenario context for Level 2 when applicable.

If the repository cannot be inspected or expected behavior cannot be
established, stop with `BLOCKED`.

---

# Level 1 — Code Testing

Skip this section when the scope excludes Level 1.

## Stage 3 — Test Target Selection

Delegate test planning and implementation to `test-writer`.

Unless the user specifies a different number, Level 1 must cover at least three
distinct production files containing meaningful business logic.

A selected target must have observable behavior such as:

* state transitions;
* validation;
* mapping or transformation;
* formatting or normalization;
* branching;
* error handling;
* result merging;
* cancellation;
* duplicate-event protection;
* stale-callback protection;
* repository or use-case behavior.

Do not count the following as meaningful targets merely to reach the required
number:

* generated files;
* constants;
* previews;
* resource declarations;
* simple data classes;
* trivial getters and setters;
* framework-only wrappers without testable application behavior.

For every target, record:

* repository-relative production path;
* corresponding test path;
* behavior to cover;
* why existing coverage is insufficient;
* selected test level: unit or integration.

---

## Stage 4 — Test Implementation

The `test-writer` must:

1. Inspect the selected production file.
2. Inspect nearby and existing test conventions.
3. Define observable expected behavior.
4. Add focused success, failure, boundary, and regression cases as applicable.
5. Reuse existing test frameworks and utilities.
6. Avoid unnecessary production changes.
7. Keep tests deterministic.
8. Use behavior-oriented test names.
9. Preserve existing test behavior.
10. Record all changed files.

Production code must not be changed during this stage unless the workflow was
explicitly invoked to fix a confirmed defect.

When a new test reveals a production defect:

* do not weaken the test;
* classify the result as `CONFIRMED_PRODUCTION_DEFECT`;
* report the suspected production location;
* continue with independent targets when reliable;
* do not silently convert the testing workflow into a bug-fix workflow.

---

## Stage 5 — Code-Test Validation

Run validation from narrowest to broadest:

1. Newly added or changed test class.
2. Relevant module test task.
3. Broader repository test task when available and practical.

Only use commands confirmed by the repository.

For each executed command, record:

* exact command;
* exit result;
* passed and failed tests when available;
* relevant error output;
* whether the failure is related to current changes.

Classify unsuccessful results as:

* `TEST_IMPLEMENTATION_ERROR`;
* `CONFIRMED_PRODUCTION_DEFECT`;
* `PRE_EXISTING_FAILURE`;
* `ENVIRONMENT_BLOCKER`.

The `test-writer` may correct mistakes in newly written tests.

It must not:

* change assertions merely to match defective behavior;
* add retries or delays to hide nondeterminism;
* disable tests;
* replace meaningful checks with weak assertions;
* report an unexecuted test as passing.

### Level 1 completion gate

Level 1 is complete only when:

* the required number of meaningful production targets was addressed;
* test files compile, unless blocked by an identified environment issue;
* all available relevant commands were executed;
* every failure is classified;
* remaining gaps are documented.

Possible Level 1 results:

* `PASS`;
* `PASS_WITH_LIMITATIONS`;
* `FAIL`;
* `BLOCKED`.

---

# Level 2 — Android Smoke Testing

Skip this section when the scope excludes Level 2.

## Stage 6 — Scenario Preparation

Use scenarios from:

```text
.codex/smoke/scenarios.md
```

Read:

```text
.codex/smoke/README.md
```

before execution.

Unless the user specifies a subset, execute all scenarios that are applicable
to the current environment.

Before execution, verify that each selected scenario contains:

* identifier;
* purpose;
* preconditions;
* ordered steps;
* expected result;
* test data when required;
* blocking conditions when applicable.

If the scope is `SMOKE_UPDATE`, inspect the current feature diff and update
`.codex/smoke/scenarios.md` before execution.

Scenario update rules:

* preserve unaffected regression scenarios;
* add a scenario only for a distinct user outcome;
* update an existing scenario when behavior changed intentionally;
* do not delete scenarios merely because they fail;
* document why every scenario was added, updated, or removed.

---

## Stage 7 — Android Preflight

Delegate execution to `smoke-tester`.

Before interacting with the application, the smoke tester must:

1. Confirm that the configured mobile MCP server is available.
2. Confirm that ADB can see a device or emulator.
3. Select an explicit device when multiple devices are present.
4. Confirm the application package name from repository evidence.
5. Confirm the application build or install state.
6. Record the device model and Android version.
7. Record the tested build variant and Git revision when available.
8. Create a unique evidence directory.

Recommended evidence directory:

```text
artifacts/smoke/<run-id>/
```

Do not reuse or overwrite an earlier run.

If the device, MCP server, application build, or required package information
is unavailable, classify Level 2 as `BLOCKED`.

---

## Stage 8 — Smoke Execution

For every selected scenario, the `smoke-tester` must:

1. Restore documented preconditions.
2. Start from a known application state.
3. Execute steps in order through mobile MCP.
4. Capture a screenshot after every meaningful step.
5. Inspect UI hierarchy for important assertions when available.
6. Record expected and actual behavior.
7. Stop the scenario after the earliest blocking failure.
8. Capture logs and UI state on failure.
9. Restore a known state before the next scenario.
10. Classify the result as:

    * `PASS`;
    * `FAIL`;
    * `BLOCKED`;
    * `SKIPPED`.

A scenario may be marked `PASS` only when every required step and assertion was
executed and verified.

A tool call completing without an error is not evidence that the application
behavior is correct.

---

## Stage 9 — Smoke Evidence

For every scenario, preserve:

* initial-state screenshot;
* screenshots after meaningful actions;
* final-state screenshot;
* relevant UI hierarchy or UI dump;
* failure logs when applicable;
* step-by-step execution notes.

Recommended structure:

```text
artifacts/smoke/<run-id>/
├── environment.md
├── SMOKE-01/
│   ├── 01-initial-state.png
│   ├── 02-action.png
│   ├── 03-final-state.png
│   ├── ui-state.txt
│   └── logs.txt
├── SMOKE-02/
└── report.md
```

Evidence files must use descriptive and stable names.

Do not claim that a screenshot exists unless it was actually created.

---

## Voice-to-Text Rules

Microphone permission testing and speech-recognition testing are separate
behaviors.

The workflow must distinguish:

* microphone control is visible;
* permission dialog is shown;
* permission is granted or denied correctly;
* listening state starts;
* real audio reaches the recognition provider;
* final recognition result is received;
* final text is processed correctly by the application.

Do not claim that speech recognition passed when only the permission flow was
tested.

Mark a speech-dependent scenario `BLOCKED` when:

* real audio input is unavailable;
* the emulator cannot provide microphone input;
* a compatible recognition provider is unavailable;
* required Google services are missing;
* required network access is unavailable;
* the application has no documented test mechanism for recognition results.

A blocked speech scenario does not automatically invalidate independent smoke
scenarios.

### Level 2 completion gate

Level 2 is complete only when:

* preflight information is recorded;
* all selected scenarios have a result;
* required screenshots were captured for executed steps;
* failures include evidence and earliest failing step;
* blocked scenarios identify the missing prerequisite;
* no scenario is marked PASS without verified assertions.

Possible Level 2 results:

* `PASS`;
* `PASS_WITH_BLOCKED_SCENARIOS`;
* `FAIL`;
* `BLOCKED`.

---

# Review

## Stage 10 — Independent Review

After applicable testing levels are complete, delegate the complete results to
`reviewer`.

The reviewer must inspect:

### Level 1

* whether selected targets contain meaningful business logic;
* whether the required number of production files was actually covered;
* whether tests verify observable behavior;
* whether assertions are sufficiently strong;
* whether edge and regression cases are appropriate;
* whether commands were really executed;
* whether failures were classified correctly;
* whether tests introduced unnecessary coupling or nondeterminism.

### Level 2

* whether scenarios represent real user behavior;
* whether preconditions were restored;
* whether screenshots correspond to documented steps;
* whether important assertions used inspectable UI evidence;
* whether PASS, FAIL, BLOCKED, and SKIPPED are justified;
* whether voice-to-text limitations were represented honestly;
* whether failure analysis is supported by logs or UI state.

### Review verdict

The reviewer must return one of:

* `APPROVED`;
* `APPROVED_WITH_LIMITATIONS`;
* `CHANGES_REQUIRED`;
* `BLOCKED`.

When the reviewer identifies an error in newly created tests:

1. Return the issue to `test-writer`.
2. Correct the test.
3. Rerun the affected validation.
4. Resubmit the result to `reviewer`.

When the reviewer identifies missing or unreliable smoke evidence:

1. Return the affected scenario to `smoke-tester`.
2. Rerun only when the environment remains valid.
3. Capture the missing evidence.
4. Resubmit the result to `reviewer`.

Do not rerun unrelated successful work without reason.

Do not modify production code to satisfy reviewer findings within this profile.

---

# Final Report

## Stage 11 — Unified Testing Report

Return one final report using the following structure.

```markdown
# Unified Testing Report

## 1. Scope

- Requested objective:
- Testing scope:
- Tested branch or revision:
- Current diff:
- Repository limitations:

## 2. Repository Research

### Architecture and modules

### Existing test setup

### Confirmed commands

### Testing constraints

## 3. Level 1 — Code Testing

### Result

PASS | PASS_WITH_LIMITATIONS | FAIL | BLOCKED | NOT_REQUESTED

### Selected production targets

| Production file | Test file | Behavior covered | Selection reason |
|---|---|---|---|

### Tests added or updated

### Commands executed

| Command | Result | Notes |
|---|---|---|

### Failures

### Remaining coverage gaps

## 4. Level 2 — Android Smoke Testing

### Result

PASS | PASS_WITH_BLOCKED_SCENARIOS | FAIL | BLOCKED | NOT_REQUESTED

### Environment

- Device:
- Android version:
- Package:
- Build variant:
- Evidence directory:

### Scenario summary

| Scenario | Result | Earliest failing step | Evidence |
|---|---|---|---|

### Detailed failures and blockers

### Residual UI risks

## 5. Changed Files

### Test files

### Smoke scenario files

### Evidence and reports

## 6. Reviewer Assessment

- Verdict:
- Blocking findings:
- Limitations:
- Required follow-up:

## 7. Final Verdict

PASS | PASS_WITH_LIMITATIONS | FAIL | BLOCKED
```

---

## Final verdict rules

Use `PASS` only when:

* every requested testing level passed;
* the required Level 1 production targets were meaningfully covered;
* relevant tests passed;
* all required smoke scenarios passed;
* reviewer verdict is `APPROVED`.

Use `PASS_WITH_LIMITATIONS` when:

* completed checks passed;
* limitations or independently blocked scenarios remain;
* reviewer verdict is `APPROVED_WITH_LIMITATIONS`;
* the limitations are clearly documented.

Use `FAIL` when:

* a code test exposes a confirmed production defect;
* a smoke scenario exposes contradictory application behavior;
* required tests fail because of current changes;
* reviewer returns `CHANGES_REQUIRED` and the issue cannot be corrected within
  test or evidence scope.

Use `BLOCKED` when:

* required validation cannot be executed because of repository or environment
  constraints;
* no reliable conclusion can be made;
* both levels are unavailable;
* reviewer returns `BLOCKED`.

Never convert a failure into a limitation merely to produce a successful final
verdict.

---

# Change-Validation Flow

When invoked after a pull request, feature, or bug fix:

1. Inspect the current diff.
2. Map changed production files to existing tests.
3. Add or update relevant tests when coverage is missing.
4. Run focused and module-level code tests.
5. Identify affected smoke scenarios.
6. Update smoke scenarios only when user-visible behavior intentionally changed.
7. Build and install the relevant Android variant.
8. Execute affected smoke scenarios.
9. Execute core smoke scenarios when practical.
10. Collect evidence.
11. Run reviewer.
12. Produce the Unified Testing Report.

The workflow must not assume that all smoke scenarios are affected by every
change.

---

# New-Feature Smoke Update Flow

When the user says:

> I deployed a new feature. Update the smoke scenarios and run everything again.

Perform:

1. Research the feature and current diff.
2. Identify new and changed user-visible outcomes.
3. Review existing scenarios for overlap.
4. Update `.codex/smoke/scenarios.md`.
5. Explain every scenario change.
6. Run relevant unit and integration tests.
7. Build and install the application.
8. Run the full applicable smoke set through mobile MCP.
9. Capture new evidence in a new run directory.
10. Run reviewer.
11. Produce the Unified Testing Report.

Do not replace the entire scenario set when only one user path changed.

---

# Stop Conditions

Stop and return the best available report when:

* repository instructions conflict with the requested operation;
* expected behavior cannot be established;
* relevant test commands cannot be identified;
* the Android device or MCP server is unavailable;
* the application cannot be installed or launched;
* required external speech services are unavailable;
* continuing would overwrite user work or earlier evidence;
* reviewer cannot validate the supplied evidence.

A partial, evidence-based result is preferable to an unsupported PASS.
