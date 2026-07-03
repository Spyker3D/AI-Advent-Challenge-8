# 02_Architecture.md

# Архитектура Android AI Assistant

> Документ описывает архитектуру Android-приложения AI Assistant.  
> Используется как внутренняя документация проекта и как источник данных для RAG-индексации.

---

# Содержание

1. Общая идея архитектуры
2. Основные слои приложения
3. MVVM
4. Repository Pattern
5. Data Flow
6. UI Layer
7. ViewModel Layer
8. Domain Layer
9. Data Layer
10. Network Layer
11. Dependency Injection
12. Работа с AI API
13. Потоковая генерация ответов
14. Обработка ошибок
15. Подготовка к RAG
16. Best Practices

---

# 1. Общая идея архитектуры

Android AI Assistant построен как модульное Android-приложение, в котором пользовательский интерфейс отделён от бизнес-логики, сетевого слоя и AI-интеграций.

Основная цель архитектуры — сделать приложение расширяемым.

Это важно, потому что AI-приложение быстро развивается:

- сначала появляется обычный чат;
- потом добавляется история сообщений;
- затем streaming;
- затем настройки модели;
- затем MCP-инструменты;
- затем RAG;
- затем локальные модели;
- затем память пользователя.

Если архитектура изначально не разделена на слои, проект быстро становится сложным для поддержки.

---

# 2. Основные слои приложения

Приложение можно представить как набор слоёв:

```text
UI Layer
    ↓
ViewModel Layer
    ↓
Domain Layer
    ↓
Data Layer
    ↓
Network / Local Storage / AI Provider
```

Каждый слой отвечает только за свою область.

---

## UI Layer

Отвечает за отображение данных.

Примеры:

- Compose screen;
- Composable components;
- Material3 UI;
- navigation;
- отображение loading/error/success-состояний.

UI не должен напрямую вызывать Retrofit или OpenAI API.

---

## ViewModel Layer

ViewModel хранит состояние экрана и обрабатывает действия пользователя.

Примеры задач ViewModel:

- принять текст сообщения;
- вызвать отправку сообщения;
- обновить список сообщений;
- показать ошибку;
- обработать streaming-ответ;
- хранить `UiState`.

---

## Domain Layer

Domain layer содержит бизнес-логику.

Примеры:

- модель сообщения;
- use case отправки сообщения;
- use case получения истории;
- use case очистки чата;
- правила формирования запроса к ассистенту.

---

## Data Layer

Data layer отвечает за получение и сохранение данных.

Примеры:

- `ChatRepository`;
- remote data source;
- local data source;
- mapper DTO → domain model;
- mapper domain model → DTO.

---

## Network Layer

Network layer отвечает за HTTP-взаимодействие.

Примеры:

- Retrofit API interface;
- OkHttp client;
- interceptors;
- request/response DTO;
- обработка HTTP-ошибок.

---

# 3. MVVM

В проекте используется подход MVVM.

MVVM расшифровывается как:

- Model;
- View;
- ViewModel.

В Android Compose это обычно выглядит так:

```text
Composable Screen
        ↓ user action
ViewModel
        ↓
Repository / UseCase
        ↓
Data Source
```

---

## View

View — это Compose UI.

Пример:

```kotlin
@Composable
fun ChatScreen(
    viewModel: ChatViewModel
) {
    val state by viewModel.uiState.collectAsState()

    ChatContent(
        state = state,
        onMessageSend = viewModel::sendMessage
    )
}
```

View не должна содержать бизнес-логику.

Плохо:

```kotlin
Button(
    onClick = {
        retrofit.sendMessage(...)
    }
)
```

Хорошо:

```kotlin
Button(
    onClick = {
        viewModel.sendMessage(input)
    }
)
```

---

## ViewModel

ViewModel принимает события от UI и управляет состоянием.

Пример:

```kotlin
class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    fun sendMessage(text: String) {
        viewModelScope.launch {
            sendMessageUseCase(text)
        }
    }
}
```

---

## Model

Model — это данные приложения.

Пример:

```kotlin
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val createdAt: Long
)
```

---

# 4. Repository Pattern

Repository скрывает детали получения данных.

ViewModel не должна знать:

- откуда пришли данные;
- используется ли Retrofit;
- есть ли локальная база;
- используется ли OpenAI;
- используется ли Ollama;
- используется ли RAG.

ViewModel должна работать с понятным интерфейсом:

```kotlin
interface ChatRepository {
    suspend fun sendMessage(message: String): ChatResponse
}
```

Или для streaming:

```kotlin
interface ChatRepository {
    fun sendMessageStream(message: String): Flow<String>
}
```

---

## Почему Repository важен

Без Repository ViewModel начинает зависеть от конкретной реализации API.

Плохо:

```kotlin
class ChatViewModel(
    private val openAiApi: OpenAiApi
)
```

Хорошо:

```kotlin
class ChatViewModel(
    private val chatRepository: ChatRepository
)
```

Так можно заменить OpenAI на Ollama, Claude, Gemini или локальную модель без изменения UI.

---

# 5. Data Flow

Типичный поток отправки сообщения:

```text
User types message
        ↓
ChatScreen
        ↓
ChatViewModel.sendMessage()
        ↓
SendMessageUseCase
        ↓
ChatRepository
        ↓
OpenAiRemoteDataSource
        ↓
OpenAI API
        ↓
Response
        ↓
Repository maps DTO to Domain
        ↓
ViewModel updates UiState
        ↓
Compose recomposes UI
```

---

# 6. UI Layer

UI Layer должен быть максимально простым.

Он отвечает за:

- отображение сообщений;
- поле ввода;
- кнопку отправки;
- индикатор загрузки;
- отображение ошибок;
- навигацию.

---

## Пример состояния экрана

```kotlin
data class ChatUiState(
    val messages: List<ChatMessageUi> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
```

---

## Пример событий UI

```kotlin
sealed interface ChatEvent {
    data class InputChanged(val value: String) : ChatEvent
    data object SendClicked : ChatEvent
    data object ClearChatClicked : ChatEvent
}
```

---

# 7. ViewModel Layer

ViewModel — центральная точка управления экраном.

Она:

- принимает события;
- вызывает use cases;
- обновляет state;
- управляет coroutine scope;
- обрабатывает ошибки.

---

## Пример ViewModel

```kotlin
class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.InputChanged -> {
                _state.update { it.copy(input = event.value) }
            }

            ChatEvent.SendClicked -> {
                sendMessage()
            }

            ChatEvent.ClearChatClicked -> {
                clearChat()
            }
        }
    }

    private fun sendMessage() {
        val text = state.value.input
        if (text.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, input = "") }

            try {
                val response = chatRepository.sendMessage(text)

                _state.update {
                    it.copy(
                        isLoading = false,
                        messages = it.messages + response.toUi()
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun clearChat() {
        _state.update {
            it.copy(messages = emptyList())
        }
    }
}
```

---

# 8. Domain Layer

Domain layer полезен, когда логика проекта становится больше.

Для маленького приложения можно обойтись Repository + ViewModel.

Но для AI Assistant domain layer становится полезным, потому что появляется много правил:

- как формировать историю сообщений;
- как ограничивать контекст;
- как добавлять system prompt;
- как вставлять RAG-контекст;
- как выбирать модель;
- как управлять streaming;
- как обрабатывать tool calls.

---

## Пример UseCase

```kotlin
class SendMessageUseCase(
    private val chatRepository: ChatRepository,
    private val promptBuilder: PromptBuilder
) {
    suspend operator fun invoke(userMessage: String): ChatMessage {
        val prompt = promptBuilder.build(userMessage)
        return chatRepository.sendMessage(prompt)
    }
}
```

---

# 9. Data Layer

Data layer содержит реализации репозиториев.

Пример:

```kotlin
class ChatRepositoryImpl(
    private val remoteDataSource: ChatRemoteDataSource
) : ChatRepository {

    override suspend fun sendMessage(message: String): ChatResponse {
        return remoteDataSource.sendMessage(message).toDomain()
    }
}
```

---

## DataSource

DataSource — это низкоуровневый источник данных.

Примеры:

```kotlin
interface ChatRemoteDataSource {
    suspend fun sendMessage(message: String): ChatResponseDto
}
```

```kotlin
class OpenAiChatRemoteDataSource(
    private val api: OpenAiApi
) : ChatRemoteDataSource {

    override suspend fun sendMessage(message: String): ChatResponseDto {
        return api.createChatCompletion(
            request = ChatCompletionRequest(...)
        )
    }
}
```

---

# 10. Network Layer

Network layer отвечает за HTTP.

Обычно он содержит:

- Retrofit interface;
- OkHttpClient;
- interceptors;
- DTO;
- error parser.

---

## Пример Retrofit API

```kotlin
interface OpenAiApi {

    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatCompletionRequestDto
    ): ChatCompletionResponseDto
}
```

---

## OkHttp Interceptor

```kotlin
class AuthInterceptor(
    private val apiKeyProvider: ApiKeyProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer ${apiKeyProvider.getApiKey()}")
            .build()

        return chain.proceed(request)
    }
}
```

---

# 11. Dependency Injection

Dependency Injection нужен, чтобы не создавать зависимости вручную внутри классов.

Плохо:

```kotlin
class ChatViewModel : ViewModel() {
    private val api = Retrofit.Builder()
        .baseUrl("...")
        .build()
        .create(OpenAiApi::class.java)
}
```

Хорошо:

```kotlin
class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel()
```

---

## Пример DI-модуля

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
```

---

# 12. Работа с AI API

AI API обычно принимает список сообщений.

Пример:

```json
{
  "model": "gpt-4.1-mini",
  "messages": [
    {
      "role": "system",
      "content": "You are a helpful Android assistant."
    },
    {
      "role": "user",
      "content": "Explain Jetpack Compose."
    }
  ]
}
```

---

## Роли сообщений

Обычно используются роли:

- `system`;
- `user`;
- `assistant`;
- `tool`.

---

## System Prompt

System prompt задаёт поведение ассистента.

Пример:

```text
Ты Android AI Assistant. Отвечай кратко, технически точно и используй контекст проекта.
```

---

# 13. Потоковая генерация ответов

Streaming нужен, чтобы пользователь видел ответ постепенно.

Без streaming:

```text
Пользователь ждёт весь ответ целиком.
```

Со streaming:

```text
Ответ появляется по словам или фрагментам.
```

---

## Поток данных при streaming

```text
OpenAI API
    ↓ chunk
Repository
    ↓ chunk
Flow<String>
    ↓ chunk
ViewModel
    ↓ state update
Compose UI
```

---

## Пример интерфейса

```kotlin
interface ChatRepository {
    fun streamMessage(message: String): Flow<String>
}
```

---

## Пример ViewModel

```kotlin
fun sendStreamingMessage(text: String) {
    viewModelScope.launch {
        chatRepository.streamMessage(text)
            .collect { chunk ->
                _state.update {
                    it.appendAssistantChunk(chunk)
                }
            }
    }
}
```

---

# 14. Обработка ошибок

AI-приложение должно корректно обрабатывать ошибки.

Типичные ошибки:

- нет интернета;
- неправильный API key;
- rate limit;
- timeout;
- ошибка сервера;
- пустой ответ;
- некорректный JSON;
- отмена запроса пользователем.

---

## Рекомендуемая модель ошибки

```kotlin
sealed interface AppError {
    data object NetworkUnavailable : AppError
    data object Unauthorized : AppError
    data object RateLimited : AppError
    data object Timeout : AppError
    data class ServerError(val code: Int) : AppError
    data class Unknown(val message: String?) : AppError
}
```

---

# 15. Подготовка к RAG

RAG добавляет новый этап между сообщением пользователя и вызовом LLM.

Без RAG:

```text
User Message
    ↓
LLM
    ↓
Answer
```

С RAG:

```text
User Message
    ↓
Embedding
    ↓
Vector Search
    ↓
Relevant Chunks
    ↓
Prompt Builder
    ↓
LLM
    ↓
Answer
```

---

## Новый компонент: KnowledgeRepository

```kotlin
interface KnowledgeRepository {
    suspend fun search(query: String, limit: Int): List<KnowledgeChunk>
}
```

---

## KnowledgeChunk

```kotlin
data class KnowledgeChunk(
    val id: String,
    val source: String,
    val title: String,
    val section: String?,
    val text: String,
    val score: Float
)
```

---

## PromptBuilder с RAG

```kotlin
class PromptBuilder(
    private val knowledgeRepository: KnowledgeRepository
) {
    suspend fun build(userMessage: String): List<ChatMessage> {
        val chunks = knowledgeRepository.search(userMessage, limit = 5)

        val context = chunks.joinToString("\n\n") {
            "[${it.source} / ${it.section}]\n${it.text}"
        }

        return listOf(
            ChatMessage.system(
                """
                Ты Android AI Assistant.
                Отвечай на основе контекста ниже.
                
                Контекст:
                $context
                """.trimIndent()
            ),
            ChatMessage.user(userMessage)
        )
    }
}
```

---

# 16. Best Practices

## 1. UI не должен знать про API

Compose UI должен работать только с состоянием и событиями.

---

## 2. ViewModel не должна создавать Retrofit

Зависимости должны приходить через DI.

---

## 3. DTO не должны попадать в UI

DTO — это модели сетевого слоя.

Для UI лучше использовать отдельные модели.

---

## 4. Ошибки должны быть типизированы

Не стоит просто показывать `Exception.message`.

---

## 5. Streaming лучше делать через Flow

`Flow<String>` хорошо подходит для потоковых ответов.

---

## 6. RAG лучше внедрять через отдельный Repository

Не нужно смешивать RAG-логику с OpenAI API.

---

## 7. PromptBuilder должен быть отдельным компонентом

Формирование промпта быстро усложняется.

Лучше вынести его отдельно.

---

# Итоговая схема архитектуры

```text
Compose UI
    ↓
ViewModel
    ↓
UseCase / PromptBuilder
    ↓
Repository
    ├── OpenAI Remote Data Source
    ├── Local Chat Storage
    ├── Knowledge Repository
    └── MCP Tools
```

---

# Заключение

Архитектура Android AI Assistant должна быть расширяемой.  
Главная идея — отделить UI, бизнес-логику, сетевой слой, AI-провайдеров и будущий RAG.

Такой подход позволит постепенно развивать приложение:

- сначала обычный AI Chat;
- затем streaming;
- затем настройки;
- затем MCP;
- затем RAG;
- затем локальные модели и память.
