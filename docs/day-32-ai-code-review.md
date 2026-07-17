# Day 32: Reactive AI Code Review

## Реактивный сценарий

`.github/workflows/ai-code-review.yml` автоматически запускается на Pull Request `opened`, `synchronize` и `reopened`:

```text
PR -> full-history checkout -> MCP Git diff -> shared Day 31 RAG
   -> OpenAI gpt-4.1-mini -> Markdown -> PR comment upsert
```

Workflow имеет `contents: read`, `pull-requests: write` и concurrency по номеру PR. Новый push отменяет устаревший запуск. Выполняются только PR из того же репозитория; `pull_request_target` не используется.

## MCP

Существующий `mcp-server/server.js` сохраняет `get_current_git_branch` и добавляет:

- `get_changed_files(baseRef, headRef)` — `git diff --name-status`;
- `get_git_diff(baseRef, headRef)` — `git diff --unified=3 --no-ext-diff`.

Операции используют `execFile` с массивом аргументов, фиксированной командой `git`, timeout и лимитом вывода. Произвольные shell-команды не принимаются, refs валидируются. В CI `MCP_PROJECT_ROOT` указывает на checkout, а `MCP_DISABLE_WEATHER=true` отключает weather scheduler.

## RAG и OpenAI

`review-pr` переиспользует Day 31 компоненты `ProjectScanner`, `ProjectChunker`, `ProjectIndexer`, `IndexStorage` и `ProjectRetriever`; отдельный индекс не создаётся. Индекс охватывает README, `docs/**`, Gradle, API/схемы и поддерживаемый код с прежними исключениями секретов и служебных каталогов.

В OpenAI передаются base/head ref, changed files, unified diff, top-K RAG chunks и метаданные truncation — не вся кодовая база и не embeddings. Модель по умолчанию `gpt-4.1-mini`. Diff больше 120 000 символов анализируется частично с явным ограничением. Пустой diff создаёт короткий Markdown без вызова OpenAI. Ошибка MCP, RAG или OpenAI завершает команду ошибкой.

## Публикация

Результат содержит `<!-- ai-developer-review -->`. `scripts/upsert-ai-review.js` находит marker через GitHub REST API: выполняет `PATCH` существующего issue comment или `POST` нового. Многострочный Markdown передаётся JSON body. Используется `GITHUB_TOKEN`, а не OpenAI key. `ai-review.md` загружается artifact-ом с `if: always()`, включая ошибку публикации.

Review содержит итог/риск, потенциальные баги, архитектурные проблемы, рекомендации, сильные стороны и ограничения. Findings используют `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO` и должны подтверждаться diff или RAG.

## GitHub Secret

Добавьте repository Actions secret:

```text
OPENAI_API_KEY=<OpenAI API key>
```

Workflow устанавливает Java 17, Node.js 20, Ollama, скачивает `nomic-embed-text:latest`, запускает MCP и review pipeline.

## Локальная отладка

Запустите Ollama и MCP:

```powershell
ollama serve
$env:MCP_PROJECT_ROOT = (Get-Location).Path
$env:MCP_DISABLE_WEATHER = "true"
node mcp-server/server.js
```

В другом терминале:

```powershell
$env:OPENAI_API_KEY = "..."
./gradlew.bat --console=plain :developer-assistant:run --args="review-pr --project-root=. --base-ref=day_31 --head-ref=HEAD --output=build/ai-review.md"
```

Локальный режим предназначен для отладки и не заменяет PR trigger. Fork PR в первой версии пропускаются, чтобы секреты не передавались чужому коду.

## Пример

```markdown
<!-- ai-developer-review -->

## AI Code Review

### Итог
Изменения имеют средний риск.

### Потенциальные баги
#### [HIGH] Ошибка не обрабатывается
- Файл: `feature/example/Example.kt`
- Строки: `42-51`
- Проблема: исключение выходит из coroutine.
- Рекомендация: обработать исключение и обновить UI state.

### Ограничения ревью
- Анализ выполнен по diff и найденному RAG-контексту.
```

## Тесты

```powershell
npm test --prefix mcp-server
./gradlew.bat :developer-assistant:test
```

Покрыты changed files, diff, invalid ref, rename, deletion, shell injection, non-Git directory; diff/RAG prompt, запись ответа, пустой/большой diff, отсутствие API key; marker, создание и обновление комментария без дубликата.
