# LLM provider setup

The Settings screen supports three providers: **OpenAI**, **Local Ollama**, and
**Private VPS**. Select the provider first; the screen then shows its relevant fields.

## OpenAI

1. Create an API key in OpenAI Platform and enable separate API billing.
2. Add it to the untracked root `local.properties` file without quotes:

```properties
OPENAI_API_KEY=your_openai_api_key
```

3. Sync Gradle and rebuild the application.
4. Select **OpenAI** in Settings. The default model is `gpt-4.1-mini`; model names do
   not need an `openai/` prefix.

## Local Ollama

No API key is required. Install and start Ollama, then pull a model:

```powershell
ollama pull qwen2.5:7b-instruct
```

Select **Local Ollama** and configure the Base URL (`http://10.0.2.2:11434` for an
Android emulator, or the host LAN address for a physical device), an installed model
tag, and optional generation settings. The app normalizes the URL but does not start
Ollama or download missing models.

## Private VPS

Development defaults may be supplied in root `local.properties`:

```properties
PRIVATE_VPS_BASE_URL=https://your-vps.example/
PRIVATE_VPS_API_KEY=your_demo_user_api_key
PRIVATE_VPS_MODEL=qwen2.5:3b
```

Select **Private VPS** and configure the Base URL, model, and API key. Settings values
override build defaults without rebuilding. Use **Test VPS connection** to verify the
server and model. Use HTTPS outside local debugging and a dedicated non-admin key.

## Secret handling

`local.properties` is ignored by Git. Never put a real key in source code, resources,
tests, Gradle properties, CI configuration, documentation, or commits. Build defaults
are embedded in the APK and can be extracted, so this educational configuration is
not suitable for distributing production secrets.