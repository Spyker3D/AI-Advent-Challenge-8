# 06_Coroutines.md

# Kotlin Coroutines

> Внутренняя документация Android AI Assistant.
>
> Документ предназначен для разработчиков проекта и используется как
> источник знаний для RAG.

------------------------------------------------------------------------

# Содержание

1.  Что такое Coroutines
2.  Зачем они нужны
3.  suspend-функции
4.  CoroutineScope
5.  launch и async
6.  Dispatchers
7.  Structured Concurrency
8.  Job и Cancellation
9.  Flow
10. StateFlow
11. SharedFlow
12. Каналы (Channel)
13. Coroutines + Compose
14. Coroutines + MVVM
15. Частые ошибки
16. Best Practices

------------------------------------------------------------------------

# 1. Что такое Coroutines

Kotlin Coroutines --- это механизм асинхронного программирования,
позволяющий писать неблокирующий код в последовательном стиле.

Вместо работы с потоками (`Thread`) разработчик использует лёгкие
корутины, которыми управляет Kotlin Runtime.

## Преимущества

-   простой синтаксис;
-   неблокирующий ввод-вывод;
-   структурированная конкуррентность;
-   интеграция с Compose;
-   поддержка Flow;
-   простая отмена операций.

------------------------------------------------------------------------

# 2. Почему не Thread

Плохо:

``` kotlin
Thread {
    val result = api.load()
}.start()
```

Лучше:

``` kotlin
viewModelScope.launch {
    val result = repository.load()
}
```

Корутины значительно легче потоков и позволяют запускать тысячи задач
без создания тысяч Thread.

------------------------------------------------------------------------

# 3. suspend-функции

`suspend` означает, что функция может быть приостановлена без блокировки
потока.

``` kotlin
suspend fun loadChat(): List<Message> {
    return api.getMessages()
}
```

Вызвать её можно только из другой suspend-функции или корутины.

------------------------------------------------------------------------

# 4. CoroutineScope

Scope определяет жизненный цикл корутин.

Основные Scope в Android:

-   viewModelScope
-   lifecycleScope
-   rememberCoroutineScope()

## Пример

``` kotlin
viewModelScope.launch {
    repository.sync()
}
```

После уничтожения ViewModel все дочерние корутины автоматически
отменяются.

------------------------------------------------------------------------

# 5. launch и async

## launch

Используется, когда результат не нужен.

``` kotlin
viewModelScope.launch {
    repository.save()
}
```

## async

Возвращает Deferred.

``` kotlin
val user = async { repository.loadUser() }
val posts = async { repository.loadPosts() }

awaitAll(user, posts)
```

Используйте async только тогда, когда действительно требуется
параллельное выполнение.

------------------------------------------------------------------------

# 6. Dispatchers

Основные диспетчеры:

-   Dispatchers.Main --- UI;
-   Dispatchers.IO --- сеть, БД, файлы;
-   Dispatchers.Default --- вычисления;
-   Dispatchers.Unconfined --- специальные случаи.

Пример:

``` kotlin
withContext(Dispatchers.IO) {
    repository.download()
}
```

------------------------------------------------------------------------

# 7. Structured Concurrency

Все дочерние корутины принадлежат родительскому Scope.

``` text
ViewModelScope
 ├── loadHistory()
 ├── streamResponse()
 └── preloadModels()
```

При отмене родителя автоматически отменяются все дочерние задачи.

Это предотвращает утечки памяти.

------------------------------------------------------------------------

# 8. Job и Cancellation

Каждая корутина имеет Job.

``` kotlin
val job = viewModelScope.launch {
    repository.sync()
}

job.cancel()
```

При работе с длинными операциями рекомендуется проверять отмену:

``` kotlin
while (isActive) {
    // работа
}
```

------------------------------------------------------------------------

# 9. Flow

Flow представляет поток данных.

``` kotlin
fun observeMessages(): Flow<List<Message>>
```

Коллектор:

``` kotlin
viewModelScope.launch {
    repository.observeMessages()
        .collect { messages ->
            update(messages)
        }
}
```

Flow идеально подходит для стриминга ответов LLM.

------------------------------------------------------------------------

# 10. StateFlow

StateFlow всегда содержит текущее состояние.

``` kotlin
private val _uiState = MutableStateFlow(ChatUiState())

val uiState = _uiState.asStateFlow()
```

Compose автоматически обновляет UI при изменении значения.

------------------------------------------------------------------------

# 11. SharedFlow

SharedFlow используется для событий.

Например:

-   Snackbar;
-   Navigation;
-   Toast;
-   One-shot события.

``` kotlin
private val _events = MutableSharedFlow<UiEvent>()
```

------------------------------------------------------------------------

# 12. Channel

Channel позволяет обмениваться сообщениями между корутинами.

Используется реже, чем Flow, но полезен для очередей задач.

------------------------------------------------------------------------

# 13. Coroutines + Compose

Compose отлично интегрирован с корутинами.

Основные API:

-   LaunchedEffect
-   rememberCoroutineScope
-   produceState
-   snapshotFlow

Не запускайте корутины напрямую внутри Composable без этих механизмов.

------------------------------------------------------------------------

# 14. Coroutines + MVVM

Рекомендуемая схема:

``` text
Compose
    │
    ▼
ViewModel
    │ viewModelScope
    ▼
Repository
    │
    ▼
Remote / Local
```

UI не должен работать с Dispatchers или создавать собственные Scope.

------------------------------------------------------------------------

# 15. Частые ошибки

❌ GlobalScope.launch

❌ Thread.sleep()

❌ Блокировка Main Dispatcher

❌ launch внутри launch без необходимости

❌ Хранение CoroutineScope в Singleton

❌ Использование runBlocking в production-коде Android

------------------------------------------------------------------------

# 16. Best Practices

✅ Используйте viewModelScope.

✅ Для сети используйте Dispatchers.IO.

✅ Для UI используйте StateFlow.

✅ Для одноразовых событий --- SharedFlow.

✅ Отменяйте длительные операции.

✅ Не используйте GlobalScope.

✅ Repository должен скрывать детали асинхронной реализации.

------------------------------------------------------------------------

# FAQ

## Когда использовать Flow?

Когда данные могут изменяться во времени.

## Когда использовать suspend?

Когда требуется одно асинхронное действие.

## Что выбрать: StateFlow или SharedFlow?

StateFlow --- состояние.

SharedFlow --- события.

------------------------------------------------------------------------

# Заключение

Coroutines являются фундаментом современной Android-разработки. В
Android AI Assistant они используются для работы с сетью, потоковой
генерации ответов LLM, хранения состояния через StateFlow и интеграции с
Jetpack Compose. Правильное применение Coroutines обеспечивает
отзывчивый интерфейс, отсутствие блокировок и масштабируемую
архитектуру.
