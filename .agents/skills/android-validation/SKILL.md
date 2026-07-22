---
name: android-validation
description: Select and run proportionate repository-confirmed validation for Android or Kotlin changes, then report actual results. Use after implementation or when explicitly asked to validate existing Android code; do not use for Node-only or documentation-only changes.
---

# Android Validation

## Purpose

Validate affected Android and Kotlin modules with the narrowest confirmed checks
and report real results without hiding failures.

## Inputs

- Changed-file list or Git diff.
- Affected Gradle modules and expected behavior.
- Approved plan and tests added or modified.
- Known environmental limitations.

## Procedure

1. Read every applicable `AGENTS.md`, especially nested validation guidance.
2. Inspect the diff and map changed files to affected Gradle modules.
3. Select the narrowest relevant unit-test task.
4. Select the narrowest relevant Kotlin compile task.
5. Add `:app:compileDebugKotlin` when DI, navigation, permissions, or application
   integration changed.
6. Add `:app:assembleDebug` only for APK-level or cross-module changes that warrant
   assembly.
7. Run selected commands sequentially and inspect exit codes and output.
8. Run `git diff --check`, then inspect `git diff` and `git status --short`.
9. Report passed, failed, and skipped checks with reasons.

Use repository-confirmed task shapes; never invent a task:

```powershell
.\gradlew.bat :<module>:testDebugUnitTest
.\gradlew.bat :<module>:compileDebugKotlin
```

## Output contract

- Affected modules and commands actually executed.
- Exit status and concise observed result for each command.
- Failures attributable to the current change.
- Skipped checks with reasons and remaining risks or blockers.
- Results of whitespace, diff, and status inspection.

## Stop and failure conditions

- Do not run Node tests through this Android-specific procedure.
- Do not run environment-gated external integration tests unless explicitly
  requested and configured.
- Do not suppress failures, delete tests, or weaken checks.
- Stop when a required check fails and correction is outside the approved plan.
- Require new human approval if a correction changes the plan, dependencies,
  architecture, public contracts, module boundaries, or editable scope.
- Never claim success for a skipped, unavailable, or uninspected command.

## Allowed agents

- Main agent owns final validation and reporting.
- `implementer` may run narrow checks within the approved plan.
- `reviewer` may inspect evidence and read-only Git state, but must not apply fixes
  or run commands that require writes in its sandbox.
- `researcher` may identify commands but should not execute validation.

## Inherited instructions

Follow applicable `AGENTS.md` files for the Definition of Done, narrowest-test-first
policy, Git inspection, failure handling, module checks, and external-service rules.
