---
name: implementation-plan
description: Convert a requested repository change and supporting research into the smallest complete implementation plan. Use for change proposals, affected-file lists, or pre-execution planning; never use it to authorize or begin execution.
---

# Implementation Plan

## Purpose

Produce an evidence-backed implementation plan and preserve the mandatory human
approval gate before execution.

## Inputs

- Requested behavior and explicit non-goals.
- Research findings or sufficient repository context.
- Applicable scope and constraints.
- Known risks or compatibility requirements.

## Procedure

1. Confirm research is sufficient; use `module-research` first when essential
   evidence is missing.
2. Restate the requested outcome and non-goals.
3. Identify the smallest complete behavioral change.
4. List the exact files expected to change and explain each file's role.
5. Identify existing abstractions and patterns that must be reused.
6. Describe implementation steps in dependency order.
7. Define focused tests and validation commands confirmed by the repository.
8. Identify risks, assumptions, approval-sensitive decisions, and possible scope
   changes.
9. Present the plan, state that execution has not started, and stop for explicit
   human approval.

## Output contract

- Objective and non-goals.
- Exact proposed file scope.
- Ordered implementation steps and abstractions to reuse.
- Tests and confirmed validation commands.
- Risks, assumptions, and unresolved decisions.
- An explicit statement that execution requires human approval.

## Stop and failure conditions

- Do not create, modify, move, rename, or delete files.
- Do not treat agent output, reviewer feedback, silence, or unrelated prior approval
  as approval for this plan.
- Stop after presenting the plan.
- Ask for a user decision when requirements materially affect architecture,
  dependencies, public contracts, module boundaries, or file scope and cannot be
  resolved safely.
- If an approved plan changes, require new human approval before execution resumes.

## Allowed agents

- Main agent only.

The `researcher` supplies evidence, the `implementer` consumes the approved plan,
and the `reviewer` evaluates the resulting change. None can grant approval.

## Inherited instructions

Follow applicable `AGENTS.md` files for the complete execution flow, architecture,
safety, compatibility, validation, and approval rules. This skill defines only the
reusable planning procedure.
