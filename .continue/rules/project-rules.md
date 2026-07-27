---
name: AI Assistant project rules
description: Project-specific architecture, safety, and validation rules
alwaysApply: true
---

# AI Assistant project rules

Follow the global agent workflow.

For this repository:

1. Read the root `AGENTS.md`.
2. Read the nearest module-level `AGENTS.md` for every affected module.
3. Preserve existing Android module boundaries and Clean Architecture.
4. Never read, expose, or modify `local.properties`.
5. Do not modify generated artifacts, copied snapshots, or indexed project
   copies.
6. Use repository-confirmed Gradle tasks.
7. Add focused tests for changed behavior.
8. Report pre-existing failures separately from failures introduced by the
   current implementation.