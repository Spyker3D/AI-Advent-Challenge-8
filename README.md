# AI Assistant

> OpenRouter использует имена моделей вида `openai/gpt-4o-mini`, а прямой
> OpenAI API использует только имя модели без префикса, например `gpt-4.1-mini`.

Модульное Android-приложение на Kotlin и Jetpack Compose с двумя режимами LLM:

- `Online` — прямой OpenAI Responses API (`https://api.openai.com/v1/responses`);
- `Local` — Ollama (`http://10.0.2.2:11434`, модель по умолчанию `qwen2.5:7b-instruct`).

Приложение использует Clean Architecture, MVVM, Dagger 2, Retrofit/OkHttp, Room,
DataStore и локальный RAG. Один и тот же локальный retrieval-контекст передаётся
выбранному online или local генератору.

## Настройка OpenAI

1. Создайте API key в OpenAI Platform.
2. Подключите отдельный API billing (подписка ChatGPT не включает API billing).
3. Добавьте в корневой `local.properties` строку без кавычек:

```properties
OPENAI_API_KEY=your_openai_api_key
```

4. Синхронизируйте Gradle и пересоберите приложение.
5. В Settings выберите `Online`; при необходимости измените модель
   `gpt-4.1-mini`.
6. Выполните тестовый запрос.

`local.properties` не коммитится и указан в `.gitignore`. Если ключ отсутствует,
приложение покажет инструкцию и не отправит запрос с пустым Bearer-заголовком.

> Для учебного проекта ключ передаётся в `BuildConfig` и попадает в APK.
> Это нельзя использовать в опубликованном production-приложении.

## Локальный режим

Установите Ollama и загрузите модель:

```bash
ollama pull qwen2.5:7b-instruct
```

Android Emulator обращается к Ollama хоста по `http://10.0.2.2:11434`.
На физическом устройстве задайте IP компьютера в Settings.

## Проверка

Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Online и Local выбираются в Settings. Имя online-модели редактируется вручную;
локальные URL и модель настраиваются отдельно.
