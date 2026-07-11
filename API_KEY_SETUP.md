# OpenAI API key setup

1. Create an API key in OpenAI Platform and enable separate API billing.
2. Add the key to the root `local.properties` file without quotes:

```properties
OPENAI_API_KEY=your_openai_api_key
```

3. Sync Gradle and rebuild the application.
4. Select `Online` in Settings and send a test message.

`local.properties` is ignored by Git. Never put a real key in source code, resources,
tests, Gradle properties, CI configuration, documentation, or commits.

> This educational project passes the key through `BuildConfig`, so the key is embedded
> in the APK. This approach must not be used in a published production application.
