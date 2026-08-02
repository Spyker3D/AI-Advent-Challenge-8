# Day 10 micro-first evaluation

This standard-library experiment evaluates five support-error categories. It embeds prototypes with `nomic-embed-text:latest`, scores each category by the maximum cosine similarity to any normalized prototype, and accepts a match only when its top score is at least `0.70` and its margin is at least `0.06`. Other inputs go to `qwen2.5:7b-instruct`, which returns a strict classification object and may use `AMBIGUOUS` for genuine uncertainty.

Maximum-prototype scoring was selected from the approved whole-dataset comparison: it accepted 6 of 30 examples with zero incorrect accepts, versus 2 of 30 with zero incorrect accepts for centroid and top-three aggregation at the same thresholds.

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

Fallback receives only the unchanged original input as `prompt`, never embeddings or prototypes. The `system` instruction retains the cause-first algorithm and every category, and explicitly says that advice to retry later is not itself a cause. `/api/generate` uses temperature zero and an exact three-field schema: allowed `category`, numeric `confidence` from 0 to 1, and nonblank Russian `reason` with `minLength: 1` and `maxLength: 160`. The parser performs only these structural validations and does not reclassify output from presentation keywords. Valid structured output uses one large-model call. Structurally invalid output receives at most one correction retry.

Generated `reports/results.jsonl` contains the exact per-example evaluation contract. `reports/summary.txt` contains coverage, large-model calls, average/median/P95 and route-specific latency, label/route/micro/fallback accuracy, incorrect confident accepts, all three groups, and every fallback-reason count.
