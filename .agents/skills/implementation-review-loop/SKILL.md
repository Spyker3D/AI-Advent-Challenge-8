---
name: implementation-review-loop
description: Run one adversarial post-implementation review and correct blocking findings that remain inside the approved plan and editable scope.
---

# Implementation Review Loop

## Purpose

Complete one automatic reviewer-to-implementer correction pass without requiring the
user to enumerate defects that are already covered by the approved behavior and file
scope.

## Inputs

- Approved implementation plan.
- Human approval evidence.
- Behavioral contracts.
- State-transition table.
- Interaction and resource ownership decisions.
- Approved editable files.
- First implementation diff.
- Test and validation evidence.

## Procedure

1. Ask `reviewer` to inspect the first implementation using all mandatory review
   lenses.
2. Classify each finding.

### Class A: corrective finding

A finding is corrective when resolving it:

- preserves the approved user-visible behavior;
- makes the implementation conform to an existing behavioral contract;
- stays within approved editable files;
- introduces no dependency;
- changes no architecture;
- changes no module boundary;
- changes no externally consumed public contract;
- does not expand the feature or its non-goals.

Class A findings remain covered by the existing human approval.

### Class B: scope-changing finding

A finding requires new human approval when resolving it changes:

- requested user-visible behavior;
- explicit non-goals;
- architecture;
- dependencies;
- public contracts;
- module boundaries;
- editable file scope.

3. If Class A blocking findings exist:
    - give `implementer` the original approved plan;
    - provide the violated contracts and acceptance criteria;
    - do not prescribe exact implementation code unless repository evidence permits
      only one valid pattern;
    - allow one corrective implementation pass.
4. Run focused tests and Android validation again.
5. Ask `reviewer` to review the corrected diff.
6. Stop after the second review.
7. Request new human approval only for unresolved Class B findings.

## Required review topics

- user action semantics;
- interaction owner;
- completion owner;
- platform-driven automatic completion;
- pauses and long-running interaction;
- stop versus cancel;
- lifecycle and resource ownership;
- provider failures;
- restart and stale callbacks;
- behavioral test coverage;
- manual Android validation.

## Output contract

- First implementation summary.
- First reviewer findings.
- Classification of findings as Class A or Class B.
- Corrective changes applied.
- Validation before and after correction.
- Second reviewer findings.
- Remaining platform limitations and manual checks.
- Clear diff between first and second generations.
- Whether any remaining work requires new human approval.

## Stop conditions

- Do not silently expand approved behavior.
- Do not apply more than one corrective pass.
- Do not claim that compilation proves platform UX behavior.
- Do not claim user-controlled completion when the platform may still terminate the
  interaction automatically.