# 08_Retrofit_Part2

# Retrofit. Часть 2. Repository, DI и обработка ошибок

## Содержание

1. Repository
2. Result<T>
3. Обработка HTTP-ошибок
4. Timeout и IOException
5. Dependency Injection
6. Использование в ViewModel
7. Best Practices
8. Итоги

---

## Repository

Repository скрывает детали работы сети от ViewModel.

```kotlin
class ChatRepository(
    private val api: OpenRouterApi
) {
    suspend fun sendMessage(request: ChatRequest): Result<ChatResponse> {
        return runCatching {
            api.chatCompletion(request)
        }
    }
}
```

ViewModel ничего не знает о Retrofit.

---

## Result<T>

Использование `Result` позволяет централизовать обработку ошибок.

```kotlin
viewModelScope.launch {
    repository.sendMessage(request)
        .onSuccess {
            _response.value = it
        }
        .onFailure {
            _error.value = it.message ?: "Unknown error"
        }
}
```

---

## HTTP-ошибки

```kotlin
try {
    api.chatCompletion(request)
} catch (e: HttpException) {
    when (e.code()) {
        401 -> "Invalid API key"
        429 -> "Rate limit exceeded"
        500 -> "Server error"
        else -> "HTTP ${e.code()}"
    }
}
```

---

## Timeout и IOException

```kotlin
catch (e: SocketTimeoutException) {
    "Request timeout"
}

catch (e: IOException) {
    "No internet connection"
}
```

---

## Dependency Injection

```kotlin
val repository = ChatRepository(api)
```

При использовании Hilt:

```kotlin
@Provides
fun provideRepository(
    api: OpenRouterApi
): ChatRepository = ChatRepository(api)
```

---

## Использование в ViewModel

```kotlin
class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel()
```

ViewModel получает только Repository.

---

## Best Practices

- Не вызывать Retrofit напрямую из UI.
- Использовать Repository как слой доступа к данным.
- Централизовать обработку ошибок.
- Возвращать `Result<T>` или собственный sealed-класс.

---

# Итоги

После этой части архитектура становится разделённой на UI, ViewModel и Repository, что упрощает тестирование и сопровождение проекта.
