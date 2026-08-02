# Day 10 micro-first evaluation

This Kotlin/JVM experiment evaluates five support-error categories. It embeds prototypes with `nomic-embed-text:latest`, scores each category by maximum cosine similarity to any normalized prototype, and accepts a match only when its top score is at least `0.70` and its margin is at least `0.06`. Other inputs use `qwen2.5:7b-instruct` fallback classification.

Maximum-prototype scoring was selected from the approved whole-dataset comparison: it accepted 6 of 30 examples with zero incorrect accepts, versus 2 of 30 with zero incorrect accepts for centroid and top-three aggregation at the same thresholds.

## Prerequisites

- JDK 17; use the repository Gradle wrapper.
- For live evaluation only: Ollama at `http://127.0.0.1:11434` with `nomic-embed-text:latest` and `qwen2.5:7b-instruct` installed.

Offline tests do not start or contact Ollama.

## Commands

Run from the repository root:

```powershell
.\gradlew.bat :day10-micro-first:test
.\gradlew.bat :day10-micro-first:run
```

To select another Ollama server:

```powershell
.\gradlew.bat :day10-micro-first:run --args="--base-url=http://127.0.0.1:11434"
```

The Gradle `run` task uses the repository root as its working directory. Prototype and test data remain in `tools/day10-micro-first/data`.

## Evaluation contract

Prototype embeddings are sent in one `/api/embed` batch; cases are embedded independently so failures can be recorded per example. Micro status is `OK` or `UNSURE`. Fallback reasons are exactly `LOW_SCORE`, `LOW_MARGIN`, `EMBEDDING_ERROR`, `INVALID_VECTOR`, `PROTOTYPE_INITIALIZATION_ERROR`, and `MICRO_RESULT_INVALID`; `LOW_SCORE` has priority when both thresholds fail.

Fallback receives only the unchanged original input. The cause-first system prompt asks for exactly `category`, `confidence`, and `reason`. The schema and parser require a supported category, numeric confidence from 0 to 1, and nonblank Russian reason of at most 160 characters. Valid output makes one large-model call; structurally invalid output receives at most one correction retry.

Generated `reports/results.jsonl` preserves the 20-field per-example contract. `reports/summary.txt` contains coverage, large-model calls, average/median/P95 and route-specific latency, label/route/micro/fallback accuracy, incorrect confident accepts, all groups, and every fallback-reason count. The console prints total requests, micro and fallback handled counts, large-model calls, average latency, and total/correct/accuracy for every expected category.
