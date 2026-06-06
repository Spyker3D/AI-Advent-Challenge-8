# API Key Setup

To use the AI Assistant app with OpenRouter API, you need to:

1. Get your API key from [OpenRouter](https://openrouter.ai/)
2. Open the file `core/data/src/main/java/com/aiassistant/core/data/repository/ChatRepositoryImpl.kt`
3. Replace `YOUR_OPENROUTER_API_KEY` with your actual API key:

```kotlin
companion object {
    private const val API_KEY = "sk-or-v1-..." // Your actual API key here
    private const val BEARER_PREFIX = "Bearer "
}
```

**Important**: Never commit your API key to version control. Consider using:
- Build config fields
- Local properties file
- Environment variables

For production apps, implement proper secret management.