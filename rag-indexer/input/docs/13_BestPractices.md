# 13_BestPractices

# Best Practices для Android AI Assistant

## Содержание

1. Compose
2. Coroutines
3. Flow
4. Retrofit
5. OpenAI / OpenRouter
6. Prompt Engineering
7. MCP
8. RAG
9. Testing
10. Performance
11. Security
12. Architecture
13. Итоги

---

## Compose

Jetpack Compose должен использоваться как декларативный слой UI.

### Рекомендации

- Не храните бизнес-логику внутри `@Composable`.
- Передавайте в Composable только состояние и callbacks.
- Делайте UI stateless, если это возможно.
- Используйте `remember` только для UI-состояния.
- Используйте `rememberSaveable`, если состояние должно переживать пересоздание Activity.
- Разделяйте большие Composable-функции на маленькие компоненты.
- Не выполняйте сетевые запросы напрямую из Composable.
- Для одноразовых событий используйте `LaunchedEffect`.

### Пример

```kotlin
@Composable
fun ChatScreen(
    state: ChatState,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    ChatContent(
        state = state,
        onMessageChange = onMessageChange,
        onSendClick = onSendClick
    )
}
```

---

## Coroutines

Coroutines используются для асинхронных операций: сеть, база данных, обработка данных.

### Рекомендации

- Используйте `viewModelScope` во ViewModel.
- Используйте `suspend` для асинхронных функций.
- Не блокируйте главный поток.
- Для тяжёлых операций используйте `Dispatchers.IO`.
- Обрабатывайте ошибки через `try/catch`, `Result` или sealed-классы.
- Не используйте `GlobalScope`.

### Пример

```kotlin
fun sendMessage(text: String) {
    viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true)

        val result = sendMessageUseCase(text)

        _state.value = _state.value.copy(
            isLoading = false,
            answer = result.getOrNull(),
            error = result.exceptionOrNull()?.message
        )
    }
}
```

---

## Flow

Flow подходит для потоков данных и реактивного состояния.

### Рекомендации

- Используйте `StateFlow` для состояния экрана.
- Используйте `SharedFlow` для одноразовых событий.
- Не открывайте `MutableStateFlow` наружу.
- Используйте `stateIn`, если нужно преобразовать Flow в StateFlow.
- Используйте `collectAsStateWithLifecycle()` в Compose.
- Избегайте лишних пересчётов и повторных подписок.

### Пример

```kotlin
private val _state = MutableStateFlow(ChatState())
val state: StateFlow<ChatState> = _state.asStateFlow()
```

В Compose:

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

---

## Retrofit

Retrofit используется для сетевого слоя.

### Рекомендации

- Не вызывайте Retrofit напрямую из UI.
- Инкапсулируйте API-вызовы в DataSource или Repository.
- Используйте DTO для сетевых моделей.
- Отделяйте DTO от domain-моделей.
- Обрабатывайте `HttpException`, `IOException`, `SocketTimeoutException`.
- Настраивайте timeout через OkHttp.
- Логирование включайте только в debug-сборках.

### Пример

```kotlin
interface OpenRouterApi {

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Body request: ChatRequestDto
    ): ChatResponseDto
}
```

---

## OpenAI / OpenRouter

AI API нужно использовать аккуратно, учитывая стоимость, задержки и безопасность.

### Рекомендации

- Не храните API Key прямо в исходном коде.
- Ограничивайте размер prompt.
- Следите за количеством токенов.
- Выбирайте модель под задачу.
- Обрабатывайте rate limit.
- Показывайте пользователю состояние загрузки.
- Добавляйте retry только для временных ошибок.
- Не отправляйте лишние персональные данные.

### Пример ошибки

```kotlin
when (exception) {
    is HttpException -> {
        when (exception.code()) {
            401 -> "Invalid API key"
            429 -> "Rate limit exceeded"
            500 -> "Server error"
            else -> "HTTP error: ${exception.code()}"
        }
    }

    is IOException -> "Network error"
    else -> "Unknown error"
}
```

---

## Prompt Engineering

Качество prompt напрямую влияет на качество ответа модели.

### Рекомендации

- Чётко задавайте роль модели.
- Добавляйте контекст.
- Указывайте формат ответа.
- Используйте примеры для сложных задач.
- Не перегружайте prompt лишними данными.
- Отделяйте системные инструкции от пользовательского ввода.
- Не доверяйте пользовательскому вводу как системной инструкции.

### Пример

```text
Ты Android AI Assistant.
Отвечай кратко, структурированно и используй Kotlin-примеры.
Если данных недостаточно, явно скажи об этом.
```

---

## MCP

MCP помогает подключать AI к внешним инструментам и данным.

### Рекомендации

- Делите инструменты по зонам ответственности.
- Не давайте модели доступ ко всем функциям сразу.
- Проверяйте входные параметры инструментов.
- Логируйте вызовы MCP-инструментов.
- Ограничивайте права доступа.
- Используйте подтверждение пользователя для опасных действий.
- Отделяйте read-only операции от write-операций.

### Пример сценария

```text
User request
    │
    ▼
AI decides tool is needed
    │
    ▼
MCP Client
    │
    ▼
MCP Server
    │
    ▼
Tool result
    │
    ▼
Final AI answer
```

---

## RAG

RAG позволяет модели отвечать на основе найденного контекста.

### Рекомендации

- Разбивайте документы на небольшие фрагменты.
- Используйте качественные embeddings.
- Храните метаданные документов.
- Ограничивайте количество retrieved chunks.
- Показывайте источники, если это важно.
- Обновляйте индекс при изменении документов.
- Фильтруйте нерелевантный контекст.
- Не передавайте в prompt весь документ целиком.

### Пример пайплайна

```text
User question
    │
    ▼
Embedding
    │
    ▼
Vector search
    │
    ▼
Relevant chunks
    │
    ▼
Prompt + Context
    │
    ▼
LLM answer
```

---

## Testing

Тестирование особенно важно в AI-приложениях, потому что поведение модели может быть непредсказуемым.

### Рекомендации

- Покрывайте UseCase unit-тестами.
- Тестируйте ViewModel отдельно от UI.
- Используйте fake Repository.
- Проверяйте состояния loading, success и error.
- Тестируйте пустой ввод.
- Тестируйте сетевые ошибки.
- Используйте UI-тесты для ключевых сценариев.
- Не завязывайте unit-тесты на реальные AI API.

### Пример

```kotlin
@Test
fun emptyMessageReturnsError() = runTest {
    val repository = FakeChatRepository()
    val useCase = SendMessageUseCase(repository)

    val result = useCase("")

    assertTrue(result.isFailure)
}
```

---

## Performance

AI-приложения могут быть медленными из-за сети, генерации ответа и обработки контекста.

### Рекомендации

- Показывайте progress indicator.
- Используйте streaming, если API это поддерживает.
- Кэшируйте историю сообщений.
- Не отправляйте слишком большой prompt.
- Сокращайте нерелевантный контекст.
- Используйте pagination для истории.
- Не пересоздавайте тяжёлые объекты в Composable.
- Используйте lazy-компоненты для списков.

### Пример

```kotlin
LazyColumn {
    items(messages) { message ->
        MessageItem(message)
    }
}
```

---

## Security

Security критически важна для AI-приложений, потому что они работают с пользовательским вводом, API-ключами и внешними сервисами.

### Рекомендации

- Не храните API Key в открытом виде.
- Не коммитьте секреты в Git.
- Используйте backend-proxy для production.
- Валидируйте пользовательский ввод.
- Ограничивайте доступ MCP-инструментов.
- Не отправляйте чувствительные данные без необходимости.
- Маскируйте токены в логах.
- Используйте HTTPS.
- Храните локальные секреты через безопасные механизмы Android.

### Плохо

```kotlin
const val API_KEY = "sk-..."
```

### Лучше

```kotlin
val apiKey = BuildConfig.OPENROUTER_API_KEY
```

Для production более безопасный вариант — получать доступ к AI API через собственный backend.

---

## Architecture

Архитектура должна помогать масштабировать проект, а не усложнять его без необходимости.

### Рекомендации

- Используйте MVVM для большинства экранов.
- Используйте MVI для сложных экранов с большим количеством состояний.
- Разделяйте Presentation, Domain и Data.
- Выносите бизнес-логику в UseCase.
- Используйте Repository для доступа к данным.
- Внедряйте зависимости через Hilt.
- Разделяйте проект на feature modules при росте.
- Не смешивайте DTO, domain-модели и UI-модели.
- Делайте зависимости направленными внутрь: UI зависит от Domain, Data реализует интерфейсы Domain.

### Пример структуры

```text
app/
 ├── core/
 │    ├── network/
 │    ├── database/
 │    ├── ui/
 │    └── common/
 ├── feature-chat/
 ├── feature-history/
 ├── feature-settings/
 ├── domain/
 └── data/
```

---

# Итоги

Best Practices помогают сделать Android AI Assistant стабильным, безопасным и масштабируемым.

Главные принципы:

- UI должен быть простым и зависеть от состояния.
- Асинхронная работа должна выполняться через Coroutines и Flow.
- Сетевой слой должен быть изолирован через Retrofit и Repository.
- AI API нужно использовать безопасно и экономно.
- Prompt должен быть структурированным.
- MCP и RAG должны иметь ограничения доступа и качества контекста.
- Архитектура должна разделять ответственность между слоями.
- Тесты должны покрывать бизнес-логику, ViewModel и ключевые пользовательские сценарии.

Такой подход позволяет развивать Android AI Assistant без хаоса в коде и без жёсткой привязки к конкретной AI-модели или поставщику API.
