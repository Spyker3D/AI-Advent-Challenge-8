# Profile: Code Review

## Purpose

Review an existing code change for correctness, regressions, architectural
consistency, lifecycle risks, platform limitations, and test coverage without
modifying any files.

This profile is strictly read-only.

## Appropriate use cases

Use this profile when:

* reviewing a local Git diff;
* reviewing staged or unstaged changes;
* reviewing a completed implementation before merge;
* checking whether a bug fix satisfies its acceptance criteria;
* checking Android lifecycle, state, concurrency, or callback behavior;
* evaluating test coverage for changed behavior.

Do not use this profile to implement fixes.

## Required workflow

The main agent must follow these stages in order:

1. SCOPE
2. CONTEXT
3. DIFF_INSPECTION
4. IMPACT_ANALYSIS
5. TEST_REVIEW
6. FINDINGS_VALIDATION
7. REPORT
8. DONE

Do not skip stages.

---

## Stage 1: SCOPE

Identify exactly what must be reviewed.

Collect when available:

* original task or bug report;
* expected behavior;
* behavioral acceptance criteria;
* relevant research findings;
* implementation summary;
* changed-file list;
* Git diff;
* validation and test results.

Determine the review target:

* unstaged changes;
* staged changes;
* a commit;
* a commit range;
* a pull-request diff;
* a supplied list of files.

If the review target is unclear, inspect the available Git state and state the
assumption used.

Do not modify Git state.

---

## Stage 2: CONTEXT

Delegate repository context discovery to `researcher` when understanding the
change requires information outside the diff.

The researcher should investigate only the context needed for review, such as:

* applicable AGENTS.md instructions;
* callers and consumers of changed symbols;
* relevant interfaces and implementations;
* state or execution flow;
* dependency injection;
* lifecycle and ownership boundaries;
* related tests;
* repository-confirmed validation commands.

Required researcher output:

1. Relevant files and symbols
2. Callers and consumers
3. Architectural constraints
4. Related state or execution flow
5. Existing tests
6. Repository invariants
7. Risks and uncertainties

The researcher must remain read-only.

Do not delegate broad, unrelated repository exploration.

---

## Stage 3: DIFF_INSPECTION

Inspect the complete review target.

The main agent must:

* inspect every changed file;
* inspect surrounding code where necessary;
* identify added, modified, moved, and deleted behavior;
* distinguish functional changes from formatting or generated output;
* identify changes outside the stated scope;
* verify that unrelated user changes are not incorrectly attributed to the
  implementation being reviewed.

Do not review only the implementation summary. The actual diff is the source of
truth.

---

## Stage 4: IMPACT_ANALYSIS

Delegate the primary review to `reviewer`.

Provide the reviewer with:

* the original task or expected behavior;
* acceptance criteria when available;
* relevant research findings;
* the complete diff or changed-file list;
* repository constraints;
* available test and validation results.

The reviewer must examine:

### Correctness

* Does the implementation satisfy the expected behavior?
* Does it address the verified root cause?
* Are success and failure paths correct?
* Are nullability and error cases handled?
* Is the implementation internally consistent?

### Regression risk

* Can changed behavior break existing callers or consumers?
* Are public contracts or state assumptions changed?
* Are unrelated flows affected?
* Are backward compatibility concerns introduced?

### Architecture

* Are existing abstractions reused?
* Are module and dependency boundaries respected?
* Is ownership placed in the correct layer?
* Were unnecessary abstractions or dependencies introduced?
* Is behavior duplicated?

### Android lifecycle and ownership

When applicable, review:

* backgrounding;
* navigation away;
* configuration changes;
* ViewModel cleanup;
* resource creation and destruction;
* screen and owner lifetimes;
* permission timing;
* process or component restart.

### State and concurrency

When applicable, review:

* duplicate starts;
* cancellation;
* stop versus completion;
* delayed callbacks;
* stale callbacks;
* restart after cancellation;
* callbacks from an earlier operation affecting a newer operation;
* thread requirements;
* race conditions.

### Platform limitations

When applicable, review:

* unavailable services or providers;
* expected framework exceptions;
* provider-controlled completion;
* timeout and heuristic behavior;
* differences between unit-test behavior and real-device behavior.

### Scope

* Are changes limited to the intended behavior?
* Were unrelated refactors introduced?
* Were generated files or artifacts committed unnecessarily?
* Were dependencies, architecture, or public contracts changed without need?

The reviewer must remain read-only and must not implement corrections.

---

## Stage 5: TEST_REVIEW

Review the tests associated with the change.

Determine:

* which changed behaviors are covered;
* which acceptance criteria have automated evidence;
* whether tests verify behavior rather than implementation details;
* whether regression tests fail before the fix and pass after it, when this can
  be established;
* whether important failure and boundary cases are missing;
* which checks require instrumentation, an emulator, or a physical device.

When applicable, inspect coverage for:

* success;
* failure;
* cancellation;
* explicit stop;
* automatic completion;
* duplicate start;
* restart;
* cleanup;
* delayed callbacks;
* stale callbacks;
* unavailable platform services;
* permission denial.

Distinguish:

* tests that were actually run;
* tests reported by another agent;
* tests merely recommended;
* manual checks still required.

Never claim that a test passed without inspectable evidence.

---

## Stage 6: FINDINGS_VALIDATION

Before reporting a finding, confirm that it is:

* caused or exposed by the reviewed change;
* supported by repository evidence;
* reachable through a plausible execution path;
* relevant to correctness, maintainability, architecture, security, lifecycle,
  or test coverage;
* specific enough to act on.

Do not report:

* personal style preferences;
* speculative failures without a plausible path;
* pre-existing issues unrelated to the change, unless explicitly separated as
  non-blocking context;
* duplicate findings describing the same root problem;
* theoretical concerns already prevented by surrounding code.

For every valid finding determine severity:

### Critical

The change can cause catastrophic data loss, severe security exposure, or
fundamental system failure and should not be merged.

### High

The change can produce incorrect behavior, crashes, major regressions, broken
user flows, or serious lifecycle or concurrency failures.

### Medium

The change has a meaningful correctness, architecture, maintainability, or test
gap that should be resolved but is not necessarily release-blocking.

### Low

The change has a limited issue with small impact or a minor evidence gap.

Avoid inflating severity.

---

## Stage 7: REPORT

Return a structured review report.

# Code Review Report

## Review scope

Describe:

* what was reviewed;
* which diff, files, commit, or range was used;
* what expected behavior and acceptance criteria were available;
* whether repository context was researched.

## Executive summary

Summarize:

* overall quality;
* whether the intended behavior appears correctly implemented;
* the most important risks;
* whether the change is ready to proceed.

## Findings

Report findings first, ordered by severity.

For every finding include:

### [Severity] Short title

**Location:** repository-relative file and symbol or line range

**Problem:**
Explain the defect or gap.

**Impact:**
Describe a concrete failure scenario or consequence.

**Evidence:**
Reference the relevant code path, contract, test, or repository invariant.

**Required correction:**
Describe the acceptance criterion for resolving the issue without prescribing
an unnecessary full implementation.

**Validation:**
Explain how the correction should be verified.

If no findings are identified, explicitly write:

> No correctness, regression, architecture, or test-coverage findings were
> identified in the reviewed scope.

## Positive observations

Report strong aspects of the change, such as:

* limited scope;
* clear ownership;
* good reuse of existing abstractions;
* strong regression tests;
* safe lifecycle handling;
* clear error handling.

Do not add praise merely to fill the section.

## Test and validation assessment

List:

* tests inspected;
* commands actually executed;
* observed results;
* acceptance criteria covered;
* missing automated coverage;
* manual or instrumentation validation still required.

## Residual risks

List risks that remain even if no code defect was identified, including:

* untested platform behavior;
* unavailable device validation;
* provider-specific behavior;
* dependency on external services;
* incomplete evidence.

## Verdict

Use exactly one verdict:

* Approved
* Approved with minor findings
* Changes required
* Insufficient evidence

Explain the verdict briefly.

---

## Read-only invariant

Throughout the entire workflow, no agent may:

* modify source code;
* modify tests;
* reformat files;
* update dependencies;
* create, move, rename, or delete files;
* stage or unstage changes;
* create commits;
* reset or clean the repository;
* apply suggested fixes.

Inspect Git status before and after the review when possible.

If the repository state changes during the review, report it.

---

## Stop conditions

Stop and report `Insufficient evidence` when:

* the review target cannot be identified;
* the relevant diff is unavailable;
* expected behavior is too ambiguous to evaluate correctness;
* essential repository context cannot be inspected;
* validation claims cannot be verified and are necessary for the verdict.

Do not guess missing behavior.

Do not implement corrections.

Do not delegate work to agents other than `researcher` and `reviewer`.

---

## Completion criteria

The profile is complete only when:

* the review scope is explicitly stated;
* the complete available diff has been inspected;
* relevant repository context has been checked;
* findings have been validated against plausible execution paths;
* test and validation evidence has been assessed;
* residual risks have been reported;
* the repository remains unchanged;
* a final verdict has been issued.
