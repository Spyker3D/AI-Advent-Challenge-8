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
7. Derive validation commands only from build configuration, package scripts,
   workflows, or repository documentation.
8. Cross-check findings against applicable architecture and safety constraints.
9. Return the research report and stop.

## Output contract

- Module responsibilities and relevant structure.
- Important abstractions and dependency direction.
- Existing patterns and conventions.
- Relevant tests and confirmed validation commands.
- Constraints, uncertainties, risks, and repository-relative file references.

## Stop and failure conditions

- Remain read-only; do not create, modify, move, rename, or delete files.
- Do not implement changes or approve an implementation plan.
- If the module or requested evidence does not exist, report that directly.
- If sources conflict, identify the conflict and prefer active source and build
  configuration.
- Never invent commands, APIs, modules, or architectural layers.

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
