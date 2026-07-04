# Jetpack Compose

> Версия документа: 1.0  
> Документ является внутренней документацией Android AI Assistant и используется как база знаний для RAG.

---

# Содержание

1. Что такое Jetpack Compose
2. Почему появился Compose
3. Declarative UI
4. Composable Functions
5. Composition
6. Recomposition
7. State
8. remember
9. rememberSaveable
10. MutableState
11. State Hoisting
12. Best Practices

---

# 1. Что такое Jetpack Compose

Jetpack Compose — современный декларативный UI Toolkit для Android.

Compose позволяет описывать интерфейс как функцию от состояния.

Вместо создания XML-разметки и поиска View через `findViewById()` разработчик пишет Kotlin-код, который непосредственно описывает внешний вид интерфейса.

Compose входит в состав Android Jetpack и активно развивается Google.

---

## Основные преимущества

- меньше шаблонного кода;
- отсутствие XML;
- тесная интеграция с Kotlin;
- простая работа с состоянием;
- естественная интеграция с Coroutines;
- отличная поддержка Material 3;
- удобное тестирование.

---

## Сравнение с View System

### Старый подход

```
XML

↓

Activity

↓

findViewById()

↓

setText()

↓

notifyDataSetChanged()
```

---

### Compose

```
State

↓

Composable

↓

UI
```

Compose автоматически обновляет интерфейс после изменения состояния.

---

# 2. Декларативный подход

Compose использует декларативный стиль.

Разработчик не говорит:

> "Измени TextView"

Он говорит:

> "Вот текущее состояние экрана."

Compose сам решает, что необходимо перерисовать.

Например.

Вместо

```kotlin
textView.text = "Hello"
```

мы пишем

```kotlin
Text(
    text = "Hello"
)
```

Или

```kotlin
Text(
    text = uiState.title
)
```

---

# 3. Первое Composable

Любая функция интерфейса отмечается аннотацией

```kotlin
@Composable
```

Например

```kotlin
@Composable
fun Greeting() {

    Text("Hello Compose")

}
```

---

Composable не возвращает View.

Он описывает интерфейс.

---

Более сложный пример.

```kotlin
@Composable
fun UserCard(

    name: String,

    age: Int

) {

    Column {

        Text(name)

        Text("$age years")

    }

}
```

Использование

```kotlin
UserCard(

    name = "Alex",

    age = 28

)
```

---

# 4. Что такое Composition

Compose строит дерево интерфейса.

Например

```kotlin
Column {

    Text("Header")

    Button()

    LazyColumn()

}
```

Получается дерево.

```
Column

├── Text

├── Button

└── LazyColumn
```

Это дерево называется Composition.

---

Compose хранит его в памяти.

При изменении состояния он сравнивает старое дерево с новым.

Перерисовывается только изменившаяся часть.

---

# 5. Recomposition

Одна из самых важных тем Compose.

Recomposition — повторный вызов Composable после изменения состояния.

Например

```kotlin
var counter by remember {

    mutableStateOf(0)

}
```

При изменении

```kotlin
counter++
```

Compose автоматически вызывает

```kotlin
CounterScreen()
```

ещё раз.

---

Важно понимать.

Compose **не перерисовывает весь экран**.

Он вычисляет минимальную область изменений.

---

Пример.

```
Column

├── Header

├── Counter

└── Footer
```

Если изменился Counter

Compose не будет заново рисовать Header.

---

# 6. State

Compose построен вокруг State.

UI всегда должен зависеть только от состояния.

Например

```kotlin
data class ChatUiState(

    val messages: List<Message>,

    val isLoading: Boolean,

    val error: String?

)
```

Экран отображает только этот объект.

---

Плохо

```kotlin
var loading

var text

var error

var counter
```

---

Хорошо

```kotlin
ChatUiState
```

---

# 7. mutableStateOf

Compose отслеживает изменения только специальных объектов.

Самый простой —

```kotlin
mutableStateOf()
```

Пример

```kotlin
var count by remember {

    mutableStateOf(0)

}
```

Теперь

```kotlin
count++
```

автоматически вызывает Recomposition.

---

Если использовать

```kotlin
var count = 0
```

Compose ничего не узнает.

UI не обновится.

---

# 8. remember

remember сохраняет значение между recomposition.

Например

```kotlin
@Composable
fun Counter() {

    var count by remember {

        mutableStateOf(0)

    }

}
```

При каждом recomposition значение не потеряется.

---

Без remember

```kotlin
var count = 0
```

После любого обновления

```
count == 0
```

---

remember живёт пока Composable находится в Composition.

---

# 9. rememberSaveable

remember не переживает поворот экрана.

После rotation

```
Activity recreated
```

Все remember исчезнут.

---

rememberSaveable сохраняет данные.

```kotlin
var name by rememberSaveable {

    mutableStateOf("")

}
```

Теперь после поворота

```
Alex
```

останется.

---

Поддерживаются:

- String

- Int

- Boolean

- Parcelable

- Serializable

- Saver

---

# 10. MutableState

MutableState состоит из одного значения.

```kotlin
val state = mutableStateOf(0)
```

или

```kotlin
state.value++
```

С использованием делегатов

```kotlin
var count by remember {

    mutableStateOf(0)

}
```

---

Compose автоматически подписывается на изменения.

---

# 11. State Hoisting

Очень важная практика.

Composable желательно делать Stateless.

Плохо

```kotlin
@Composable
fun ChatInput() {

    var text by remember {

        mutableStateOf("")

    }

}
```

Лучше

```kotlin
@Composable
fun ChatInput(

    text: String,

    onTextChange: (String) -> Unit

)
```

Теперь управление состоянием находится выше.

Например

```kotlin
ChatScreen

↓

ChatInput
```

---

Это делает компонент:

- проще;

- переиспользуемым;

- тестируемым.

---

# 12. Stateless и Stateful компоненты

Stateful

```
Сам хранит состояние
```

Stateless

```
Получает состояние извне
```

Google рекомендует делать большинство компонентов Stateless.

---

# 13. Пример ChatScreen

```kotlin
@Composable
fun ChatScreen(

    state: ChatUiState,

    onSend: (String) -> Unit

) {

    Column {

        MessageList(

            state.messages

        )

        ChatInput(

            onSend = onSend

        )

    }

}
```

Обрати внимание.

ChatScreen не знает

- Retrofit

- Repository

- ViewModel

Он получает только данные.

---

# 14. Основные ошибки новичков

❌ Хранить состояние внутри каждого Composable.

---

❌ Использовать mutableListOf вместо SnapshotStateList.

---

❌ Изменять обычные List.

---

❌ Передавать ViewModel глубоко внутрь дерева.

---

❌ Создавать состояние без remember.

---

❌ Хранить бизнес-логику внутри Composable.

---

# 15. Best Practices

✅ UI — это функция от State.

---

✅ Используй immutable UiState.

---

✅ Делай компоненты Stateless.

---

✅ Используй remember только для локального UI.

---

✅ Бизнес-логика должна находиться во ViewModel.

---

✅ Один источник истины (Single Source of Truth).

---

# Краткое резюме

В первой части мы рассмотрели:

- декларативный UI;
- отличие Compose от View System;
- Composable Functions;
- Composition;
- Recomposition;
- State;
- mutableStateOf;
- remember;
- rememberSaveable;
- State Hoisting;
- Stateless компоненты.

Во второй части будут рассмотрены Side Effects, LaunchedEffect, DisposableEffect, produceState, derivedStateOf, CompositionLocal и жизненный цикл Compose.
> Часть 2. Side Effects, жизненный цикл и продвинутые возможности
> Compose.

------------------------------------------------------------------------

# Содержание

13. Side Effects
14. LaunchedEffect
15. rememberCoroutineScope
16. DisposableEffect
17. SideEffect
18. produceState
19. derivedStateOf
20. snapshotFlow
21. CompositionLocal
22. Жизненный цикл Compose
23. Оптимизация Recomposition
24. Частые ошибки

------------------------------------------------------------------------

# 13. Side Effects

В Compose Composable-функции должны быть **чистыми**: они описывают UI и
не должны выполнять сетевые запросы, записывать данные в БД или
запускать корутины непосредственно во время композиции.

Для выполнения подобных действий используются специальные Side Effect
API.

Основные API:

-   LaunchedEffect
-   DisposableEffect
-   SideEffect
-   produceState
-   rememberCoroutineScope
-   snapshotFlow

------------------------------------------------------------------------

# 14. LaunchedEffect

`LaunchedEffect` запускает корутину, привязанную к жизненному циклу
Composable.

``` kotlin
@Composable
fun ChatScreen(viewModel: ChatViewModel) {

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

}
```

Короутина автоматически отменяется, когда Composable покидает
Composition.

## Ключи

``` kotlin
LaunchedEffect(chatId) {
    viewModel.loadChat(chatId)
}
```

Если `chatId` изменится, предыдущая корутина отменится и запустится
новая.

### Когда использовать

-   первоначальная загрузка данных;
-   запросы к Repository;
-   запуск анимаций;
-   работа с Flow.

------------------------------------------------------------------------

# 15. rememberCoroutineScope

Если необходимо запускать корутины по действию пользователя (например,
по нажатию кнопки), используется `rememberCoroutineScope`.

``` kotlin
val scope = rememberCoroutineScope()

Button(onClick = {
    scope.launch {
        snackbarHostState.showSnackbar("Saved")
    }
}) {
    Text("Save")
}
```

------------------------------------------------------------------------

# 16. DisposableEffect

Используется для регистрации и освобождения ресурсов.

``` kotlin
DisposableEffect(lifecycleOwner) {

    val observer = LifecycleEventObserver { _, _ -> }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

Применяется при работе с:

-   Lifecycle;
-   BroadcastReceiver;
-   SensorManager;
-   Camera;
-   Location.

------------------------------------------------------------------------

# 17. SideEffect

Позволяет выполнять код после успешной композиции.

``` kotlin
SideEffect {
    analytics.setCurrentScreen("Chat")
}
```

Используется для синхронизации Compose с внешними API.

------------------------------------------------------------------------

# 18. produceState

Позволяет преобразовать внешний источник данных в `State`.

``` kotlin
val user by produceState<User?>(null, userId) {
    value = repository.loadUser(userId)
}
```

UI автоматически обновится после изменения `value`.

------------------------------------------------------------------------

# 19. derivedStateOf

Используется для вычисляемого состояния.

``` kotlin
val canSend by remember {
    derivedStateOf {
        input.isNotBlank()
    }
}
```

Вычисление произойдет только при изменении зависимостей.

------------------------------------------------------------------------

# 20. snapshotFlow

Преобразует Compose State в Flow.

``` kotlin
LaunchedEffect(Unit) {
    snapshotFlow { listState.firstVisibleItemIndex }
        .collect {
            println(it)
        }
}
```

Полезно для аналитики и реакций на изменения состояния UI.

------------------------------------------------------------------------

# 21. CompositionLocal

Позволяет передавать зависимости без явной передачи параметров.

``` kotlin
val LocalSpacing = compositionLocalOf {
    Spacing()
}
```

Использование:

``` kotlin
val spacing = LocalSpacing.current
```

Подходит для:

-   темы;
-   размеров;
-   навигации;
-   сервисов.

------------------------------------------------------------------------

# 22. Жизненный цикл Compose

``` text
Composable появляется
        │
        ▼
Composition
        │
        ▼
Recomposition (много раз)
        │
        ▼
Dispose
```

Важно понимать, что Recomposition может происходить десятки раз в
секунду.

------------------------------------------------------------------------

# 23. Оптимизация Recomposition

Рекомендуется:

-   использовать immutable модели;
-   избегать создания объектов внутри Composable;
-   использовать `remember`;
-   применять `derivedStateOf` для дорогих вычислений;
-   не хранить бизнес-логику в UI.

------------------------------------------------------------------------

# 24. Частые ошибки

❌ Запуск сетевого запроса прямо в Composable.

❌ Использование `GlobalScope`.

❌ Изменение обычного `List` вместо `SnapshotStateList`.

❌ Создание нового объекта при каждой Recomposition.

❌ Передача ViewModel глубоко в дерево компонентов.

------------------------------------------------------------------------

# Краткое резюме

Во второй части рассмотрены:

-   Side Effect API;
-   LaunchedEffect;
-   rememberCoroutineScope;
-   DisposableEffect;
-   SideEffect;
-   produceState;
-   derivedStateOf;
-   snapshotFlow;
-   CompositionLocal;
-   жизненный цикл Compose;
-   оптимизация Recomposition.

В третьей части будут рассмотрены Material 3, Navigation Compose, Lazy
layouts, производительность, архитектурные рекомендации и полноценный
пример экрана чата.

> Часть 3. Material 3, Navigation, производительность и лучшие практики.

------------------------------------------------------------------------

# Содержание

25. Material 3
26. Scaffold
27. Lazy Layouts
28. Navigation Compose
29. Темизация
30. Производительность
31. Stable и Immutable
32. Архитектура Compose
33. Пример экрана AI Chat
34. FAQ
35. Best Practices

------------------------------------------------------------------------

# 25. Material 3

Material 3 --- актуальная дизайн-система Google.

Основные компоненты:

-   Button
-   FilledButton
-   OutlinedButton
-   ElevatedCard
-   Card
-   TextField
-   TopAppBar
-   BottomAppBar
-   NavigationBar
-   NavigationRail
-   FloatingActionButton

Пример:

``` kotlin
Button(
    onClick = onSend
) {
    Text("Send")
}
```

------------------------------------------------------------------------

# 26. Scaffold

Scaffold задает каркас экрана.

``` kotlin
Scaffold(
    topBar = { ChatTopBar() },
    snackbarHost = { SnackbarHost(hostState) },
    floatingActionButton = { NewChatFab() }
) { padding ->
    ChatContent(
        modifier = Modifier.padding(padding)
    )
}
```

Обычно внутри Scaffold располагается основной контент экрана.

------------------------------------------------------------------------

# 27. Lazy Layouts

Для больших списков используются Lazy-компоненты.

## LazyColumn

``` kotlin
LazyColumn {
    items(messages) { message ->
        MessageBubble(message)
    }
}
```

## LazyRow

Используется для горизонтальных списков.

## LazyVerticalGrid

Подходит для галерей и сеток.

``` kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2)
) {
    items(images) {
        ImageCard(it)
    }
}
```

------------------------------------------------------------------------

# 28. Navigation Compose

Навигация строится вокруг NavController.

``` kotlin
NavHost(
    navController = navController,
    startDestination = "chat"
) {
    composable("chat") {
        ChatScreen()
    }

    composable("settings") {
        SettingsScreen()
    }
}
```

Рекомендуется передавать только идентификаторы, а не большие объекты.

------------------------------------------------------------------------

# 29. Темизация

Compose использует MaterialTheme.

``` kotlin
MaterialTheme(
    colorScheme = darkColorScheme()
) {
    App()
}
```

Рекомендуется хранить:

-   Color
-   Typography
-   Shapes

в отдельных файлах темы.

------------------------------------------------------------------------

# 30. Производительность

Основные причины медленной работы Compose:

-   лишние Recomposition;
-   создание объектов внутри Composable;
-   тяжелые вычисления в UI;
-   нестабильные модели.

Рекомендации:

-   использовать remember;
-   использовать immutable data class;
-   не выполнять сетевые запросы в UI;
-   разбивать большие Composable на маленькие.

------------------------------------------------------------------------

# 31. Stable и Immutable

Compose может оптимизировать Recomposition, если знает, что объект
стабилен.

``` kotlin
@Immutable
data class UserUi(
    val id: String,
    val name: String
)
```

или

``` kotlin
@Stable
class ChatState(...)
```

Использовать аннотации следует только тогда, когда объект действительно
соответствует требованиям стабильности.

------------------------------------------------------------------------

# 32. Архитектура Compose

Рекомендуемая схема:

``` text
Screen
   │
   ▼
UiState
   │
   ▼
Composable Components
```

Компоненты должны быть максимально независимыми.

Например:

``` text
ChatScreen
 ├── TopBar
 ├── MessageList
 ├── MessageBubble
 ├── TypingIndicator
 └── ChatInput
```

Такую структуру проще тестировать и переиспользовать.

------------------------------------------------------------------------

# 33. Пример экрана AI Chat

``` kotlin
@Composable
fun ChatScreen(
    state: ChatUiState,
    onSend: (String) -> Unit
) {
    Scaffold(
        topBar = {
            ChatTopBar()
        }
    ) { padding ->

        Column(
            modifier = Modifier.padding(padding)
        ) {

            MessageList(
                messages = state.messages,
                modifier = Modifier.weight(1f)
            )

            ChatInput(
                value = state.input,
                onValueChange = {},
                onSend = onSend
            )
        }
    }
}
```

Такой экран:

-   ничего не знает про Retrofit;
-   ничего не знает про Repository;
-   ничего не знает про OpenAI.

Он работает только с UiState.

------------------------------------------------------------------------

# 34. FAQ

## Нужно ли использовать XML?

Нет. Для новых экранов рекомендуется использовать только Compose.

## Где должна находиться бизнес-логика?

Во ViewModel или UseCase.

## Можно ли запускать корутины в Composable?

Да, но через:

-   LaunchedEffect
-   rememberCoroutineScope

## Нужно ли использовать remember для всего?

Нет. Только для локального UI-состояния.

------------------------------------------------------------------------

# 35. Best Practices

✅ Один источник истины (Single Source of Truth).

✅ Stateless-компоненты по умолчанию.

✅ UiState --- immutable.

✅ Repository не должен зависеть от UI.

✅ ViewModel не должна содержать Android View.

✅ Использовать Material 3.

✅ Использовать Navigation Compose.

✅ Для больших списков использовать LazyColumn.

✅ Минимизировать Recomposition.

✅ Следить за производительностью через Layout Inspector и Compose
Tracing.

------------------------------------------------------------------------

# Итоговое резюме

В трех частях документа рассмотрены:

-   основы декларативного UI;
-   State и Recomposition;
-   remember и rememberSaveable;
-   Side Effect API;
-   жизненный цикл Compose;
-   Material 3;
-   Navigation Compose;
-   Lazy Layouts;
-   архитектура Compose-приложений;
-   производительность;
-   рекомендации по разработке.

Эти знания являются фундаментом современной Android-разработки и
используются практически в каждом экране Android AI Assistant.
