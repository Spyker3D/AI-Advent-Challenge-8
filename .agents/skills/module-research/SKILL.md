---
name: module-research
description: Research a repository module, its architecture, dependencies, conventions, tests, and confirmed validation commands without implementing changes. Use for module inspection, analysis, mapping, or pre-plan evidence gathering; do not use for execution.
---

# Module Research

## Purpose

Produce an evidence-based, read-only report about one repository module before
planning or review.

## Inputs

- Module path or Gradle module name.
- Research question or intended change.
- Optional focus such as architecture, presentation, persistence, tests, or
  integration boundaries.

## Procedure

1. Confirm the requested module and research scope.
2. Read every applicable `AGENTS.md`, including nested module instructions.
3. Inspect the module build file and declared project dependencies.
4. Inspect active source structure; do not treat copied or generated files as
   authoritative.
5. Identify entry points, primary abstractions, state models, and integrations
   relevant to the question.
6. Inspect representative implementations and relevant tests.
7. Classify the requested behavior using all applicable categories:
    - pure transformation;
    - synchronous user interaction;
    - asynchronous operation;
    - callback-based platform integration;
    - permission-gated integration;
    - lifecycle-bound resource;
    - persistent mutation;
    - external service boundary.
8. For interactive behavior, identify:
    - interaction owner;
    - completion owner;
    - timeout owner;
    - cancellation owner;
    - whether the platform may finish the interaction automatically;
    - whether platform-default completion matches the requested UX.
9. For lifecycle-bound or callback-based work, identify:
    - resource owner;
    - state transitions;
    - stopping and cancellation semantics;
    - stale-callback risk;
    - restart behavior;
    - thread requirements;
    - expected platform exceptions;
    - provider limitations.
10. Determine whether the selected or existing platform API can actually guarantee
    the requested behavior. Do not assume that an API name or basic happy path proves
    UX compatibility.
11. Identify the required validation level:
    - JVM unit test;
    - Robolectric;
    - instrumentation;
    - manual device validation.
12. Derive validation commands only from build configuration, package scripts,
   workflows, or repository documentation.
13. Cross-check findings against applicable architecture and safety constraints.
14. Return the research report and stop.

## Output contract

- Module responsibilities and relevant structure.
- Important abstractions and dependency direction.
- Existing patterns and conventions.
- Relevant tests and confirmed validation commands.
- Constraints, uncertainties, risks, and repository-relative file references.
- Behavioral contract and expected user-visible semantics.
- Interaction owner and completion owner.
- Platform-driven automatic completion risks.
- Resource ownership and lifecycle boundary.
- State-transition table or the evidence required to create it.
- Stop, cancel, cleanup, restart, and stale-callback distinctions.
- Platform/API limitations that may prevent the requested UX.
- Required automated and manual validation levels.

## Stop and failure conditions

- Remain read-only; do not create, modify, move, rename, or delete files.
- Do not implement changes or approve an implementation plan.
- If the module or requested evidence does not exist, report that directly.
- If sources conflict, identify the conflict and prefer active source and build
  configuration.
- Never invent commands, APIs, modules, or architectural layers.
- Stop and report the limitation when the available platform API cannot guarantee
    the requested interaction model.
- Do not silently reinterpret user-controlled completion as framework-controlled
  completion.
- Do not recommend implementation until completion ownership and lifecycle behavior
  are understood.

## Allowed agents

- Main agent.
- `researcher`.
- `reviewer` when additional read-only module context is necessary.

The `implementer` should consume completed research rather than repeat broad
discovery during execution.

## Inherited instructions

Follow applicable global, repository, and nested `AGENTS.md` files for language,
source-of-truth rules, secrets, architecture, naming, safety, and research policy.
Do not duplicate those instructions here.
