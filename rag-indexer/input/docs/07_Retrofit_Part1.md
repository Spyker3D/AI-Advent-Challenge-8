# 07_Retrofit_Part1.md

# Retrofit. Часть 1

> Внутренняя документация Android AI Assistant.
>
> Документ предназначен для разработчиков проекта и используется как
> источник знаний для RAG.

------------------------------------------------------------------------

# Содержание

1. Что такое Retrofit
2. Зачем Retrofit нужен в Android AI Assistant
3. Retrofit в сетевом слое приложения
4. REST API: базовые понятия
5. HTTP-методы
6. Endpoint
7. Base URL
8. DTO-модели
9. Retrofit Interface
10. Аннотации Retrofit
11. Request Body
12. Response Body
13. suspend-функции в Retrofit
14. Retrofit + Coroutines
15. Retrofit Builder
16. Converter Factory
17. OkHttpClient
18. Headers
19. Authorization Header
20. OpenRouter API как пример
21. Минимальная структура Network Layer
22. Частые ошибки
23. Best Practices

------------------------------------------------------------------------

# 1. Что такое Retrofit

Retrofit --- это библиотека для работы с HTTP API в Android и JVM-приложениях.

Она позволяет описывать сетевые запросы в виде Kotlin-интерфейсов, а затем
автоматически превращает вызовы этих интерфейсов в реальные HTTP-запросы.

Вместо ручной работы с `HttpURLConnection` разработчик описывает API так:

```kotlin
interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatCompletionRequestDto
    ): ChatCompletionResponseDto
}
```

Retrofit берёт на себя:

- построение HTTP-запроса;
- сериализацию тела запроса;
- десериализацию ответа;
- интеграцию с OkHttp;
- поддержку Coroutines;
- работу с аннотациями `@GET`, `@POST`, `@Body`, `@Header`, `@Query`.

------------------------------------------------------------------------

# 2. Зачем Retrofit нужен в Android AI Assistant

Android AI Assistant использует сетевой слой для общения с LLM API.

В проекте Retrofit нужен для:

- отправки сообщений пользователя в OpenRouter;
- получения ответа модели;
- передачи выбранной модели;
- отправки system prompt;
- передачи истории сообщений;
- обработки сетевых ошибок;
- отделения UI от деталей HTTP-запросов.

UI не должен знать, как именно отправляется запрос.

Правильная схема:

```text
Compose Screen
    │
    ▼
ViewModel
    │
    ▼
Repository
    │
    ▼
Retrofit API
    │
    ▼
OpenRouter
```

Composable-компоненты работают только с `UiState` и событиями.
Retrofit находится в data/network слое.

------------------------------------------------------------------------

# 3. Retrofit в сетевом слое приложения

Retrofit относится к data layer.

Рекомендуемая структура:

```text
core/data/src/main/java/com/aiassistant/core/data/network/
    OpenRouterApi.kt
    NetworkModule.kt
    dto/
        ChatCompletionRequestDto.kt
        ChatCompletionResponseDto.kt
        MessageDto.kt
```

Repository использует Retrofit API, но domain layer не должен зависеть от
Retrofit напрямую.

Плохо:

```kotlin
class ChatViewModel(
    private val api: OpenRouterApi
)
```

Лучше:

```kotlin
class ChatViewModel(
    private val chatRepository: ChatRepository
)
```

ViewModel не должна знать, что внутри используется Retrofit.

------------------------------------------------------------------------

# 4. REST API: базовые понятия

Retrofit чаще всего используется для REST API.

REST API работает через HTTP-запросы.

Запрос обычно состоит из:

- HTTP-метода;
- URL;
- headers;
- query parameters;
- request body;
- response body.

Пример HTTP-запроса к OpenRouter:

```text
POST https://openrouter.ai/api/v1/chat/completions
Authorization: Bearer <API_KEY>
Content-Type: application/json

{
  "model": "openai/gpt-4o-mini",
  "messages": [
    { "role": "user", "content": "Привет" }
  ]
}
```

Retrofit позволяет описать такой запрос декларативно через Kotlin-интерфейс.

------------------------------------------------------------------------

# 5. HTTP-методы

Основные HTTP-методы:

| Метод | Назначение |
|---|---|
| `GET` | Получить данные |
| `POST` | Создать ресурс или отправить действие |
| `PUT` | Полностью обновить ресурс |
| `PATCH` | Частично обновить ресурс |
| `DELETE` | Удалить ресурс |

Для LLM chat completion обычно используется `POST`, потому что клиент отправляет
тело запроса с сообщениями и настройками модели.

Пример:

```kotlin
@POST("chat/completions")
suspend fun createChatCompletion(
    @Body request: ChatCompletionRequestDto
): ChatCompletionResponseDto
```

------------------------------------------------------------------------

# 6. Endpoint

Endpoint --- это конкретный путь API.

Например:

```text
chat/completions
```

Если base URL равен:

```text
https://openrouter.ai/api/v1/
```

то полный URL будет:

```text
https://openrouter.ai/api/v1/chat/completions
```

В Retrofit endpoint указывается в аннотации:

```kotlin
@POST("chat/completions")
```

Важно: endpoint не должен начинаться с `/`, если base URL уже заканчивается на `/`.

------------------------------------------------------------------------

# 7. Base URL

Base URL --- это базовый адрес API.

Для OpenRouter:

```kotlin
private const val BASE_URL = "https://openrouter.ai/api/v1/"
```

Retrofit требует, чтобы `baseUrl` заканчивался слэшем `/`.

Плохо:

```kotlin
.baseUrl("https://openrouter.ai/api/v1")
```

Хорошо:

```kotlin
.baseUrl("https://openrouter.ai/api/v1/")
```

Если забыть завершающий `/`, Retrofit выбросит ошибку при создании клиента.

------------------------------------------------------------------------

# 8. DTO-модели

DTO означает Data Transfer Object.

DTO используется для передачи данных между приложением и внешним API.

DTO не равен domain model.

Пример DTO для сообщения:

```kotlin
data class MessageDto(
    val role: String,
    val content: String
)
```

Пример DTO для запроса:

```kotlin
data class ChatCompletionRequestDto(
    val model: String,
    val messages: List<MessageDto>,
    val temperature: Double? = null
)
```

Пример DTO для ответа:

```kotlin
data class ChatCompletionResponseDto(
    val choices: List<ChoiceDto>
)

data class ChoiceDto(
    val message: MessageDto
)
```

DTO должны соответствовать JSON-структуре API.

------------------------------------------------------------------------

# 9. Retrofit Interface

Retrofit API описывается через interface.

```kotlin
interface OpenRouterApi {

    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatCompletionRequestDto
    ): ChatCompletionResponseDto
}
```

Retrofit сам создаёт реализацию этого интерфейса:

```kotlin
val api = retrofit.create(OpenRouterApi::class.java)
```

Разработчик не пишет реализацию вручную.

------------------------------------------------------------------------

# 10. Аннотации Retrofit

Основные аннотации:

| Аннотация | Назначение |
|---|---|
| `@GET` | GET-запрос |
| `@POST` | POST-запрос |
| `@PUT` | PUT-запрос |
| `@PATCH` | PATCH-запрос |
| `@DELETE` | DELETE-запрос |
| `@Body` | Тело запроса |
| `@Header` | Один header |
| `@Headers` | Статические headers |
| `@Query` | Query parameter |
| `@Path` | Переменная часть URL |

Пример с query parameter:

```kotlin
@GET("models")
suspend fun getModels(
    @Query("category") category: String
): ModelsResponseDto
```

Пример с path parameter:

```kotlin
@GET("models/{modelId}")
suspend fun getModel(
    @Path("modelId") modelId: String
): ModelDto
```

------------------------------------------------------------------------

# 11. Request Body

`@Body` используется для JSON-тела запроса.

```kotlin
@POST("chat/completions")
suspend fun createChatCompletion(
    @Body request: ChatCompletionRequestDto
): ChatCompletionResponseDto
```

Retrofit передаёт объект `request` в converter.
Converter превращает Kotlin-объект в JSON.

Например:

```kotlin
val request = ChatCompletionRequestDto(
    model = "openai/gpt-4o-mini",
    messages = listOf(
        MessageDto(
            role = "user",
            content = "Объясни Retrofit"
        )
    )
)
```

Будет отправлен JSON:

```json
{
  "model": "openai/gpt-4o-mini",
  "messages": [
    {
      "role": "user",
      "content": "Объясни Retrofit"
    }
  ]
}
```

------------------------------------------------------------------------

# 12. Response Body

Response body --- это тело ответа сервера.

Retrofit может вернуть уже готовую DTO-модель:

```kotlin
suspend fun createChatCompletion(
    @Body request: ChatCompletionRequestDto
): ChatCompletionResponseDto
```

Тогда при успешном ответе приложение получит объект:

```kotlin
val response = api.createChatCompletion(request)
val assistantMessage = response.choices.firstOrNull()?.message?.content
```

Но если нужно обрабатывать HTTP-код вручную, можно использовать `Response<T>`:

```kotlin
@POST("chat/completions")
suspend fun createChatCompletion(
    @Body request: ChatCompletionRequestDto
): Response<ChatCompletionResponseDto>
```

Тогда можно проверить:

```kotlin
if (response.isSuccessful) {
    val body = response.body()
} else {
    val errorBody = response.errorBody()?.string()
}
```

Для production-кода часто удобнее возвращать `Response<T>` на уровне API и
преобразовывать результат в `Result` или собственный sealed class в Repository.

------------------------------------------------------------------------

# 13. suspend-функции в Retrofit

Retrofit поддерживает Kotlin Coroutines.

Сетевой метод можно объявить как `suspend`:

```kotlin
@POST("chat/completions")
suspend fun createChatCompletion(
    @Body request: ChatCompletionRequestDto
): ChatCompletionResponseDto
```

Такой метод:

- не блокирует Main Thread;
- может вызываться из корутины;
- автоматически приостанавливает выполнение до получения ответа;
- выбрасывает исключение при сетевой ошибке.

Пример вызова из Repository:

```kotlin
class OpenRouterRepository(
    private val api: OpenRouterApi
) {
    suspend fun sendMessage(text: String): String {
        val response = api.createChatCompletion(
            ChatCompletionRequestDto(
                model = "openai/gpt-4o-mini",
                messages = listOf(
                    MessageDto(role = "user", content = text)
                )
            )
        )

        return response.choices.first().message.content
    }
}
```

------------------------------------------------------------------------

# 14. Retrofit + Coroutines

Retrofit хорошо сочетается с Coroutines и `viewModelScope`.

ViewModel вызывает Repository:

```kotlin
fun sendMessage(text: String) {
    viewModelScope.launch {
        val answer = chatRepository.sendMessage(text)
        // update UiState
    }
}
```

Repository вызывает Retrofit API:

```kotlin
suspend fun sendMessage(text: String): String {
    return openRouterApi.createChatCompletion(
        request = buildRequest(text)
    ).choices.first().message.content
}
```

UI не должен вызывать Retrofit напрямую.

------------------------------------------------------------------------

# 15. Retrofit Builder

Retrofit создаётся через `Retrofit.Builder()`.

Минимальный пример:

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://openrouter.ai/api/v1/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val api = retrofit.create(OpenRouterApi::class.java)
```

В реальном проекте Retrofit обычно создаётся через DI-модуль.

Например:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenRouterApi(
        retrofit: Retrofit
    ): OpenRouterApi {
        return retrofit.create(OpenRouterApi::class.java)
    }
}
```

------------------------------------------------------------------------

# 16. Converter Factory

Converter Factory отвечает за преобразование Kotlin-объектов в JSON и JSON в
Kotlin-объекты.

Популярные варианты:

- Gson;
- Moshi;
- Kotlinx Serialization.

Пример с Gson:

```kotlin
.addConverterFactory(GsonConverterFactory.create())
```

Зависимости:

```kotlin
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
```

Если в проекте уже используется Gson для локального JSON-хранилища, можно
использовать Gson и для Retrofit, чтобы не добавлять второй JSON-стек без
необходимости.

------------------------------------------------------------------------

# 17. OkHttpClient

Retrofit использует OkHttp для реального выполнения HTTP-запросов.

OkHttpClient позволяет настроить:

- headers;
- interceptors;
- timeout;
- logging;
- кеширование;
- retry-поведение.

Пример:

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()
```

Для LLM API read timeout часто делают больше, потому что генерация ответа может
занимать несколько секунд.

------------------------------------------------------------------------

# 18. Headers

Headers передают служебную информацию вместе с запросом.

Типичные headers:

```text
Authorization: Bearer <API_KEY>
Content-Type: application/json
```

Header можно передать прямо в метод:

```kotlin
@POST("chat/completions")
suspend fun createChatCompletion(
    @Header("Authorization") authorization: String,
    @Body request: ChatCompletionRequestDto
): ChatCompletionResponseDto
```

Но если header нужен для всех запросов, лучше использовать OkHttp Interceptor.

------------------------------------------------------------------------

# 19. Authorization Header

API-ключ не должен храниться в Composable, ViewModel или Repository как строка,
зашитая в код.

Плохо:

```kotlin
private const val API_KEY = "sk-..."
```

Лучше:

```kotlin
class AuthInterceptor(
    private val apiKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        return chain.proceed(request)
    }
}
```

Подключение:

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor(apiKey))
    .build()
```

Источник API-ключа может быть предоставлен через `BuildConfig`, encrypted storage
или другой безопасный механизм проекта.

------------------------------------------------------------------------

# 20. OpenRouter API как пример

Для Android AI Assistant основной пример Retrofit-интеграции --- OpenRouter.

Минимальный API-интерфейс:

```kotlin
interface OpenRouterApi {

    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatCompletionRequestDto
    ): ChatCompletionResponseDto
}
```

Request DTO:

```kotlin
data class ChatCompletionRequestDto(
    val model: String,
    val messages: List<MessageDto>,
    val temperature: Double? = null
)

data class MessageDto(
    val role: String,
    val content: String
)
```

Response DTO:

```kotlin
data class ChatCompletionResponseDto(
    val choices: List<ChoiceDto>
)

data class ChoiceDto(
    val message: MessageDto
)
```

Repository:

```kotlin
class OpenRouterChatRepository(
    private val api: OpenRouterApi
) : ChatRepository {

    override suspend fun sendMessage(
        model: String,
        messages: List<Message>
    ): Message {
        val request = ChatCompletionRequestDto(
            model = model,
            messages = messages.map { message ->
                MessageDto(
                    role = message.role.value,
                    content = message.content
                )
            }
        )

        val response = api.createChatCompletion(request)
        val content = response.choices.firstOrNull()?.message?.content.orEmpty()

        return Message(
            role = MessageRole.ASSISTANT,
            content = content
        )
    }
}
```

DTO-маппинг лучше держать в data layer, чтобы domain layer не зависел от
формата OpenRouter JSON.

------------------------------------------------------------------------

# 21. Минимальная структура Network Layer

Минимальная структура для проекта:

```text
core/data/network/
    OpenRouterApi.kt
    AuthInterceptor.kt
    NetworkModule.kt
    dto/
        ChatCompletionRequestDto.kt
        ChatCompletionResponseDto.kt
        MessageDto.kt
```

Зависимости направлены внутрь:

```text
ViewModel -> Repository interface -> Repository implementation -> Retrofit API
```

Domain layer может содержать интерфейс:

```kotlin
interface ChatRepository {
    suspend fun sendMessage(
        model: String,
        messages: List<Message>
    ): Message
}
```

Data layer содержит реализацию:

```kotlin
class OpenRouterChatRepository(
    private val api: OpenRouterApi
) : ChatRepository
```

Такой подход упрощает тестирование и позволяет заменить API без изменения UI.

------------------------------------------------------------------------

# 22. Частые ошибки

❌ Вызывать Retrofit из Composable.

❌ Хранить API-ключ прямо в исходном коде.

❌ Использовать неправильный `baseUrl` без завершающего `/`.

❌ Смешивать DTO и domain model.

❌ Не обрабатывать сетевые ошибки.

❌ Не настраивать timeout для LLM API.

❌ Создавать новый Retrofit instance на каждый запрос.

❌ Возвращать Retrofit `Response<T>` прямо в UI.

❌ Делать сетевые запросы через callback API, если проект уже использует Coroutines.

------------------------------------------------------------------------

# 23. Best Practices

✅ Держите Retrofit только в data layer.

✅ Описывайте API через interface.

✅ Используйте `suspend`-функции.

✅ Используйте Repository как границу между domain и network.

✅ Разделяйте DTO и domain models.

✅ Добавляйте headers через OkHttp Interceptor.

✅ Храните API-ключ вне исходного кода.

✅ Используйте один Singleton Retrofit instance.

✅ Настраивайте timeout под LLM-запросы.

✅ Обрабатывайте ошибки в Repository.

✅ Не показывайте сырые network errors напрямую пользователю.

✅ Логируйте запросы только в debug-сборках и не логируйте секреты.

------------------------------------------------------------------------

# FAQ

## Нужно ли использовать Retrofit в Composable?

Нет. Composable должен работать с состоянием и событиями, а не с сетью.

## Где создавать Retrofit instance?

В DI-модуле data layer. Обычно как Singleton.

## Что лучше: callback или suspend?

Для проекта с Coroutines лучше использовать `suspend`.

## Можно ли использовать Gson?

Да. Если проект уже использует Gson, это простой и практичный выбор.

## Нужно ли делать DTO отдельно от domain model?

Да. DTO отражает внешний API, а domain model отражает внутреннюю модель
приложения.

------------------------------------------------------------------------

# Итоговое резюме

В первой части документа рассмотрены основы Retrofit:

- что такое Retrofit;
- зачем он нужен Android AI Assistant;
- как устроены HTTP-запросы;
- что такое endpoint и base URL;
- как описывать API через interface;
- как использовать `@POST`, `@Body`, `@Header`, `@Query`, `@Path`;
- как устроены request и response DTO;
- как Retrofit работает с Coroutines;
- как создать Retrofit через Builder;
- зачем нужен OkHttpClient;
- как добавлять authorization headers;
- как применить Retrofit для OpenRouter;
- какие ошибки чаще всего встречаются.

Retrofit является основой сетевого слоя Android AI Assistant. Он должен быть
изолирован в data layer, вызываться через Repository и не попадать напрямую в UI
или domain logic.
