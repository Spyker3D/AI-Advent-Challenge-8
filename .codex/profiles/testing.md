# Testing Profile

## Purpose

Use this profile to perform repository-aware validation throughout the entire
testing lifecycle.

The workflow supports three different testing phases:

1. **Bootstrap**

   Establish the initial automated testing baseline.

   This phase discovers insufficiently tested production business logic,
   creates focused unit or integration tests, validates them, and establishes
   the repository testing baseline.

2. **Validation**

   Validate an existing testing baseline after repository changes.

   This phase primarily reuses existing tests, updates them only when required,
   executes Android smoke scenarios, and produces fresh validation evidence.

3. **Feature Update**

   Validate a newly implemented or deployed feature.

   This phase updates automated tests, updates smoke scenarios for changed
   user-visible behavior, executes the complete validation pipeline, and
   produces a new unified report.

This profile coordinates specialized agents and produces one unified testing
report.

---

## Invocation

Example:

```text
Work strictly according to `.codex/profiles/testing.md`.

Run FULL_VALIDATION.
```

Examples of narrower scopes:

### Initial repository bootstrap

```text
Work strictly according to `.codex/profiles/testing.md`.

Run LEVEL_1_BOOTSTRAP.

Find and test at least three insufficiently covered production files.
```

---

### Code validation only

```text
Work strictly according to `.codex/profiles/testing.md`.

Run LEVEL_1_VALIDATION.
```

---

### Smoke validation only

```text
Work strictly according to `.codex/profiles/testing.md`.

Run LEVEL_2.

Execute all applicable Android smoke scenarios.
```

---

### Pull request validation

```text
Work strictly according to `.codex/profiles/testing.md`.

Run CHANGE_VALIDATION for the current branch.
```

---

### Newly implemented feature

```text
Work strictly according to `.codex/profiles/testing.md`.

I implemented a new feature.

Update automated tests if necessary.

Update smoke scenarios.

Run the complete validation workflow.
```

This request selects `FEATURE_UPDATE`.

---

When the user does not specify the workflow:

* use `LEVEL_1_BOOTSTRAP` only when the repository does not yet contain the
  required initial testing baseline;

* otherwise use `FULL_VALIDATION`.

---

## Agents

This workflow uses the following specialized agents.

### researcher

Responsibilities:

* inspect repository architecture;
* inspect AGENTS.md instructions;
* inspect existing testing infrastructure;
* inspect existing smoke scenarios;
* inspect current Git changes;
* identify affected production behavior;
* determine whether existing tests already provide sufficient coverage.

The researcher never modifies repository files.

---

### test-writer

Responsibilities:

* establish the initial testing baseline;
* update existing automated tests when justified;
* create regression tests for newly discovered production defects;
* execute repository test commands;
* classify failures.

The test-writer does not modify production code unless the workflow explicitly
requests bug fixing.

---

### smoke-tester

Responsibilities:

* execute Android smoke scenarios through the configured mobile MCP server;
* collect screenshots;
* collect UI hierarchy;
* collect diagnostic logs;
* report observable user behavior.

The smoke-tester never modifies production code.

---

### reviewer

Responsibilities:

* independently review testing evidence;
* verify conclusions;
* identify missing validation;
* produce the final verdict.

---

Do not substitute one specialized agent for another when the required role is
available.

---

# Global Invariants

Throughout every workflow:

* Read every applicable AGENTS.md.
* Preserve unrelated user changes.
* Never create commits.
* Never stage or unstage files.
* Never rewrite Git history.
* Never weaken assertions merely to obtain passing tests.
* Never disable failing tests.
* Never invent screenshots, logs, Gradle tasks, package names, device
  identifiers, selectors, or execution results.
* Never report PASS unless execution evidence exists.
* Distinguish production defects from testing defects.
* Keep production code unchanged unless the workflow explicitly requests a bug
  fix.
* Smoke execution must treat production code and automated tests as read-only.
* Every physical smoke execution must create a new evidence directory.

---

# Workflow

## Stage 1 — Scope

Determine which workflow the user requested.

Possible workflows:

---

### LEVEL_1_BOOTSTRAP

Purpose:

Create the initial automated testing baseline.

Responsibilities:

* discover insufficiently tested production behavior;
* select meaningful production targets;
* create unit or integration tests;
* execute repository test commands;
* establish repository testing coverage.

Unless the user specifies otherwise, this workflow must cover at least three
meaningful production files.

---

### LEVEL_1_VALIDATION

Purpose:

Validate the existing automated testing baseline.

Responsibilities:

* inspect existing tests;
* determine whether current repository changes require additional tests;
* update tests only when repository evidence justifies doing so;
* execute repository test commands.

This workflow does **not** search for three additional production files merely
to repeat the bootstrap requirement.

---

### LEVEL_2

Purpose:

Execute Android smoke scenarios only.

Responsibilities:

* execute documented scenarios;
* capture screenshots;
* collect logs;
* classify results.

---

### FULL_VALIDATION

Purpose:

Perform complete repository validation.

Responsibilities:

1. validate automated tests;
2. execute code tests;
3. build the application;
4. execute Android smoke scenarios;
5. collect new evidence;
6. produce one Unified Testing Report.

`FULL_VALIDATION` reuses the established testing baseline whenever it is still
valid.

It creates or updates automated tests only when:

* repository changes introduce uncovered behavior;
* current expectations became obsolete;
* a regression requires additional coverage;
* the user explicitly requests additional tests.

---

### CHANGE_VALIDATION

Purpose:

Validate the current branch, pull request, bug fix, or repository diff.

Responsibilities:

* inspect changed production files;
* map changes to existing tests;
* update tests only when required;
* identify affected smoke scenarios;
* execute focused validation.

---

### FEATURE_UPDATE

Purpose:

Validate a newly implemented or newly deployed feature.

Responsibilities:

* inspect changed production behavior;
* update automated tests where necessary;
* update smoke scenarios for changed user-visible behavior;
* preserve unaffected regression scenarios;
* execute complete validation;
* produce a fresh Unified Testing Report.

---

When the workflow has been determined, record:

* requested objective;
* current Git branch when available;
* current revision;
* current diff when applicable;
* selected workflow;
* requested smoke scenarios;
* whether smoke scenario updates are permitted.

Do not begin implementation before observable expected behavior is sufficiently
understood.

---

## Stage 2 — Repository Research

Delegate repository investigation to `researcher`.

The researcher must inspect:

1. repository structure;
2. Android modules;
3. AGENTS.md instructions;
4. Gradle configuration;
5. existing unit tests;
6. existing integration tests;
7. existing instrumentation tests;
8. existing smoke scenarios;
9. production modules containing business logic;
10. existing testing coverage;
11. application package name;
12. build variants;
13. confirmed Gradle commands;
14. current Git diff;
15. Android execution constraints.

For `LEVEL_1_BOOTSTRAP`, the researcher must propose meaningful production
targets that currently lack sufficient automated testing.

For every validation workflow
(`LEVEL_1_VALIDATION`, `FULL_VALIDATION`,
`CHANGE_VALIDATION`, `FEATURE_UPDATE`)
the researcher must instead determine:

* whether an adequate testing baseline already exists;
* which existing tests cover changed production behavior;
* whether additional automated tests are actually required.

For workflows containing smoke execution, identify:

* application package;
* application entry point;
* stable UI controls;
* required permissions;
* external dependencies;
* scenario preconditions;
* confirmed application build.

Research results must distinguish:

* repository evidence;
* inference;
* unknowns;
* environment prerequisites.

---

### Research Gate

Proceed only when:

* repository testing conventions are understood;
* executable test commands have been confirmed;
* required validation targets have been identified;
* smoke prerequisites are understood.

Otherwise return:

`BLOCKED`.

# Level 1 — Code Testing

Skip this section when the selected workflow excludes Level 1.

---

## Stage 3 — Test Planning

Delegate planning and implementation to `test-writer`.

The planning strategy depends on the selected workflow.

---

### LEVEL_1_BOOTSTRAP

The objective is to establish the initial repository testing baseline.

Unless the user specifies otherwise, select at least three distinct production
files containing meaningful business logic.

Selection criteria:

* observable state transitions;
* validation logic;
* mapping or transformation;
* formatting or normalization;
* branching;
* error handling;
* repository or use-case behavior;
* cancellation;
* stale callback protection;
* duplicate event protection;
* result aggregation;
* business rules.

Do not satisfy the required target count using:

* generated files;
* previews;
* constants;
* resources;
* simple data classes;
* trivial getters or setters;
* framework wrappers without application behavior.

For every selected target record:

* production file;
* planned test file;
* observable behavior;
* current coverage gap;
* selected test level;
* justification.

---

### LEVEL_1_VALIDATION

The objective is to validate the existing testing baseline.

Do not search for three additional production targets.

Instead:

1. inspect existing tests;
2. map production behavior to existing coverage;
3. inspect current repository changes;
4. determine whether existing tests remain sufficient.

Only create or modify tests when repository evidence demonstrates that:

* changed business behavior is not covered;
* expected behavior intentionally changed;
* a regression requires additional verification;
* an existing automated test became obsolete;
* the user explicitly requested additional coverage.

When no automated-test changes are required, explicitly record:

```
Existing automated testing baseline is sufficient.

No code-test changes are required for this validation run.
```

---

### CHANGE_VALIDATION

Inspect the current Git diff.

For every changed production file determine:

* existing automated tests;
* missing coverage;
* obsolete expectations;
* regression risk.

Only affected areas should receive updated tests.

---

### FEATURE_UPDATE

Inspect the new feature.

Determine:

* newly introduced business behavior;
* modified business behavior;
* unchanged behavior.

Rules:

* add tests for genuinely new behavior;
* update tests for intentionally changed behavior;
* preserve valid existing tests;
* do not duplicate equivalent coverage.

The initial bootstrap requirement remains satisfied by the existing repository
baseline.

---

## Stage 4 — Test Implementation

Delegate implementation to `test-writer`.

The test-writer must:

1. inspect production behavior;
2. inspect nearby testing conventions;
3. inspect existing tests;
4. define observable expected behavior;
5. reuse existing testing infrastructure;
6. keep tests deterministic;
7. use behavior-oriented names;
8. preserve valid existing tests;
9. avoid unnecessary production changes;
10. record every modified file.

When new automated tests are created they should include, where appropriate:

* successful behavior;
* failure behavior;
* boundary conditions;
* regression cases;
* error handling;
* cancellation behavior;
* duplicate protection;
* stale callback protection.

The workflow must never replace an existing strong assertion with a weaker one.

Production code must remain unchanged unless the workflow explicitly includes bug
fixing.

---

### Confirmed Production Defects

When an automated test exposes a production defect:

do not weaken the test.

Instead:

* classify the result as
  `CONFIRMED_PRODUCTION_DEFECT`;
* identify the suspected production location;
* continue independent validation whenever practical;
* do not silently convert the workflow into bug fixing.

---

### No-Test-Change Outcome

When repository evidence shows that the existing automated testing baseline
already provides sufficient coverage, record:

* inspected production behavior;
* inspected automated tests;
* justification for reusing the existing baseline.

This is considered a successful validation outcome.

---

## Stage 5 — Code-Test Validation

Execute validation from the narrowest practical scope toward broader validation.

Recommended order:

1. newly added or modified test classes;
2. existing focused tests covering changed behavior;
3. relevant module test task;
4. broader repository task when practical.

Only execute commands confirmed by repository evidence.

For every executed command record:

* exact command;
* execution result;
* exit code;
* passed tests;
* failed tests;
* relevant output;
* relationship to current changes.

Never report a command as executed unless execution evidence exists.

---

### Failure Classification

Every unsuccessful execution must be classified as exactly one of:

* `TEST_IMPLEMENTATION_ERROR`
* `CONFIRMED_PRODUCTION_DEFECT`
* `PRE_EXISTING_FAILURE`
* `ENVIRONMENT_BLOCKER`

The test-writer may fix mistakes introduced into newly written tests.

The test-writer must not:

* weaken assertions;
* introduce retries merely to hide nondeterminism;
* disable failing tests;
* replace observable verification with trivial assertions;
* report PASS without execution.

---

### Level 1 Completion Gate

LEVEL_1_BOOTSTRAP completes only when:

* the required production targets were covered;
* automated tests compile unless blocked by environment;
* repository test commands executed;
* every failure classified;
* remaining coverage gaps documented.

LEVEL_1_VALIDATION,
CHANGE_VALIDATION,
FEATURE_UPDATE,
and FULL_VALIDATION complete when:

* the existing testing baseline has been inspected;
* necessary test updates have been completed;
* repository test commands executed;
* every failure classified;
* justification exists whenever no test modifications were required.

---

Possible Level 1 results:

* PASS
* PASS_WITH_LIMITATIONS
* FAIL
* BLOCKED