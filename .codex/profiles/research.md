# Profile: Research

## Purpose

Answer a question about the repository using verifiable evidence without
modifying any files.

## Required workflow

1. SCOPE
2. DISCOVERY
3. TRACE
4. VERIFY
5. REVIEW
6. REPORT
7. DONE

## Delegation

Delegate the primary repository investigation to `researcher`.

The researcher must:

- read applicable AGENTS.md files;
- inspect repository structure;
- search relevant symbols and usages;
- inspect dependency injection;
- trace callers and consumers;
- inspect build configuration when relevant;
- inspect related tests;
- distinguish direct evidence from inference.

After the investigation, delegate evidence review to `reviewer`.

The reviewer checks:

- whether the question was actually answered;
- whether important code paths were missed;
- whether conclusions are supported by repository evidence;
- whether assumptions were presented as facts.

## Read-only invariant

No agent may:

- modify source code;
- modify tests;
- reformat files;
- change dependencies;
- create commits;
- change Git state.

The main agent must inspect `git status` before and after the task.

## Final report

# Research Report

## Question
## Executive answer
## Relevant files and symbols
## Execution or data flow
## Dependencies and relationships
## Tests and coverage
## Confirmed conclusions
## Inferences
## Unknowns
## Repository integrity