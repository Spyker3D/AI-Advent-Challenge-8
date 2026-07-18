# Developer Assistant CLI

> Day 34 добавляет в этот же CLI безопасного project file assistant: обычная строка считается целью, `/diff` показывает последний proposed diff, а `--dry-run` запрещает запись. Полная архитектура, сценарии и ограничения описаны в корневом `README.md`.

Локальный Kotlin/JVM CLI для вопросов о реальной кодовой базе. Он сканирует README, `docs`, Gradle и исходники, строит локальный инкрементальный RAG-индекс через Ollama, получает Git-ветку через MCP и генерирует итоговый ответ через OpenAI Responses API.

## Архитектура

- `rag-core` — scanner, chunker, SHA-256 manifest, локальный JSON vector index, Ollama embeddings и cosine retrieval.
- `developer-assistant` — конфигурация, command loop, OpenAI Responses и MCP-клиенты.
- `mcp-server` — JSON-RPC tool `get_current_git_branch` с фиксированной командой Git.

Модуль не зависит от Android SDK и не попадает в APK.

## Запуск

Нужны JDK 17, Ollama с локальной embedding-моделью и OpenAI API key:

```powershell
ollama pull nomic-embed-text:latest
ollama serve
$env:OPENAI_API_KEY = "your-api-key"
```

В отдельном терминале запустите MCP для того же проекта:

```powershell
$env:MCP_PROJECT_ROOT = "C:\Projects\AI-Advent-Challenge-8"
node mcp-server/server.js
```

```powershell
./run-developer-assistant.bat

# Ручной запуск через Gradle может искажать кириллицу в Windows-консоли.
# Для прямого запуска сначала соберите distribution:
chcp 65001
gradlew.bat --console=plain :developer-assistant:installDist
./developer-assistant/build/install/developer-assistant/bin/developer-assistant.bat --project-root=.
```

Без `--project-root` используется рабочая директория. Ollama настраивается через `OLLAMA_BASE_URL` и `OLLAMA_EMBEDDING_MODEL`; значения по умолчанию — `http://localhost:11434` и `nomic-embed-text:latest`. OpenAI настраивается через `OPENAI_BASE_URL` и `OPENAI_GENERATION_MODEL`; defaults — `https://api.openai.com/v1` и `gpt-4.1-mini`.

API key ищется сначала в environment variable `OPENAI_API_KEY`, затем в `<project-root>/local.properties`:

```properties
OPENAI_API_KEY=your-api-key
```

Environment имеет приоритет. Если ключ не найден, CLI завершается с понятной ошибкой. `local.properties` исключён из Git и RAG-сканирования, а значение ключа не выводится в логах. Через CLI-параметры ключ не принимается.

Также доступны `--embedding-base-url`, `--embedding-model`, `--openai-base-url`, `--generation-model`, `--mcp-url`, `--top-k`, `--chunk-size`, `--chunk-overlap`, `--max-file-size`, `--debug`.

## Команды и индекс

- `/help <question>` — RAG, ветка через MCP и ответ Ollama с источниками.
- `/status` — состояние проекта, индекса и сервисов.
- `/reindex` — полная перестройка.
- `/exit` — выход.

Индекс хранится в `<project-root>/.developer-assistant/index.json`, manifest — в `manifest.json`. Metadata индекса содержит `embeddingProvider`, `embeddingModel` и фактическую `embeddingDimensions`, определённую по первому ответу Ollama. При смене модели или старом index-формате выполняется полная переиндексация. Embeddings и индекс не отправляются в OpenAI.

## RAG modes

`review-pr` поддерживает три режима RAG:

- `off` — пропускает RAG и выполняет review только по PR metadata, changed files и git diff; Ollama не требуется;
- `existing` — использует существующий индекс, но никогда не строит и не обновляет его; если индекса нет, review продолжается без RAG;
- `update` — строит или инкрементально обновляет индекс перед поиском.

Режим по умолчанию — `update` для обратной совместимости.

```powershell
./gradlew.bat :developer-assistant:run --args="review-pr --project-root=. --base-ref=main --head-ref=HEAD --output=build/ai-review.md --rag-mode=off"
```

GitHub Actions использует `--rag-mode=off`, чтобы не устанавливать Ollama и не тратить время на полную индексацию проекта.

## Фильтры и безопасность

Поддерживаются `.md`, `.txt`, `.kt`, `.kts`, `.java`, `.gradle`, `.toml`, `.xml`, `.json`, `.yaml`, `.yml`, `.js`, `.ts`. Исключаются `.git`, `.gradle`, `.idea`, `.developer-assistant`, `build` на любой глубине, `out`, `node_modules`, `dist`, `generated`, `captures`, `.externalNativeBuild`, `.cxx`, `secrets`. Не читаются `.env`, `local.properties`, `secrets.properties`, ключи, архивы, Android-пакеты, media и бинарные файлы. Лимит по умолчанию — 1 MiB; ошибка отдельного пути не прекращает сканирование.

> В OpenAI передаются только вопрос, Git-ветка и top-K найденных чанков с путями и строками — не весь проект и не embeddings. Проверьте исключения своего проекта. Содержимое секретных файлов и API-ключ не логируются.

## Ограничения

Chunking эвристический, без полного AST; watch mode отсутствует. MCP настраивается одним project root при старте, CLI намеренно не вызывает Git напрямую. При недоступном MCP `/help` продолжает работу и сообщает, что ветка недоступна. Без Ollama embeddings обновление и поиск невозможны; Ollama для генерации не используется, OpenAI Embeddings API не используется.
