# Day 10 micro-first evaluation

This standard-library experiment evaluates five support-error categories. It embeds prototypes with `nomic-embed-text:latest`, builds normalized centroids, and accepts a cosine match only when its top score is at least `0.70` and its margin is at least `0.06`. Other inputs go to `qwen2.5:7b-instruct`, which returns a strict Russian presentation object and may use `AMBIGUOUS` for genuine uncertainty.

## Prerequisites

- Python 3.10 or newer.
- Ollama reachable at `http://127.0.0.1:11434`.
- Local Ollama models `nomic-embed-text:latest` and `qwen2.5:7b-instruct`.

Offline tests do not start or contact Ollama.

## Commands

Run from the repository root:

```powershell
python tools/day10-micro-first/test_runner.py
python -m json.tool tools/day10-micro-first/data/prototypes.json > $null
Get-Content tools/day10-micro-first/data/test_cases.jsonl | ForEach-Object { $_ | ConvertFrom-Json | Out-Null }
python tools/day10-micro-first/run_micro_first.py
```

Optional server/model overrides:

```powershell
python tools/day10-micro-first/run_micro_first.py --base-url http://127.0.0.1:11434 --micro-model nomic-embed-text:latest --fallback-model qwen2.5:7b-instruct
```

Prototype embeddings are batched through `/api/embed`; case embeddings are requested independently so failures can be recorded per case. Micro status is `OK` or `UNSURE`. Fallback reasons are exactly `LOW_SCORE`, `LOW_MARGIN`, `EMBEDDING_ERROR`, `INVALID_VECTOR`, `PROTOTYPE_INITIALIZATION_ERROR`, and `MICRO_RESULT_INVALID`; when both thresholds fail, `LOW_SCORE` has priority. Vectors must be finite, non-zero, and dimensionally consistent.

Fallback receives only the unchanged original input as `prompt`, never embeddings or prototypes. A short classification instruction and any correction request are sent in `system`. `/api/generate` uses temperature zero, a strict `category`, `confidence`, `title`, `message`, `user_action` schema, and at most one correction retry for malformed output. Russian user actions cannot expose internal category or action enum names.

Generated `reports/results.jsonl` contains the exact per-example evaluation contract. `reports/summary.txt` contains coverage, large-model calls, average/median/P95 and route-specific latency, label/route/micro/fallback accuracy, incorrect confident accepts, all three groups, and every fallback-reason count.
