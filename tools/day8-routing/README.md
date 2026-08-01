# Day 8 model routing

The Android implementation lives in `core:domain/routing`. This opt-in runner executes the ten-case corpus against a host Ollama instance and writes JSON plus Markdown reports.

```powershell
python tools/day8-routing/scripts/run_model_routing.py
Get-Content tools/day8-routing/reports/latest.md
ollama list
```

Set `OLLAMA_BASE_URL` only when Ollama is not available at `http://localhost:11434`. The runner does not install or pull models.

Important: the Android RouteModelRequestUseCase is opt-in and must only be invoked while LOCAL_OLLAMA is selected; ordinary chat does not invoke it.
