# Profile: Bug Fix

## Purpose

Investigate a reported defect, identify its verified root cause, implement the
smallest complete fix, and validate that related behavior remains correct.

## Required workflow

1. INTAKE
2. REPRODUCTION
3. INVESTIGATION
4. ROOT_CAUSE
5. IMPLEMENTATION
6. REVIEW
7. VALIDATION
8. DONE

Do not skip stages.

## Delegation

### INVESTIGATION

Delegate repository investigation to `researcher`.

Required output:

- reproduction evidence;
- relevant files and symbols;
- execution or state flow;
- root-cause hypothesis;
- related tests;
- regression risks.

The researcher must not modify files.

### IMPLEMENTATION

After the root cause is supported by evidence, delegate the smallest complete
fix to `implementer`.

Required input:

- verified root cause;
- approved scope;
- files allowed to change;
- required regression test;
- validation commands.

Required output:

- changed files;
- implementation summary;
- tests added or changed;
- unresolved limitations.

### REVIEW

Delegate the resulting diff to `reviewer`.

Required output:

- correctness findings;
- regression risks;
- missing tests;
- verdict.

If the reviewer reports a material defect, return the task to `implementer`.

### VALIDATION

The main agent must:

- run the narrowest relevant test;
- run affected module tests;
- compile the affected module;
- inspect git diff;
- inspect git status;
- run an Android smoke test when the environment permits it.

Do not claim that a check passed unless it was executed and inspected.

## Prohibited

- Do not implement before identifying the root cause.
- Do not ignore failing tests.
- Do not modify tests only to hide incorrect behavior.
- Do not perform unrelated refactoring.
- Do not create commits without explicit permission.

## Final report

# Bug Fix Report

## Problem
## Reproduction
## Root cause
## Changed files
## Fix
## Tests and validation
## Review result
## Remaining risks
## Final status