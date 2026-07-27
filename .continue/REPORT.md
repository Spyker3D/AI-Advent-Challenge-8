# День 4 — Local Boost

## 1. Цель работы

Цель задания — подключить локальную LLM как код-ассистент в IDE, настроить её под существующий Android-проект, проверить работу в режимах **автокомплита**, **чата** и **агентного выполнения**, а затем честно сравнить результат с облачным ассистентом.

В работе использовались:

- **Android Studio**
- **Continue 1.0.67**
- **Ollama**
- локальная модель **Llama 3.1 8B**
- дополнительно тестировалась **Qwen2.5-Coder 7B**
- облачный ассистент **Codex GPT-5.6-sol**

Основная задача для сравнения:

> Реализовать voice-to-text ввод сообщения на экране чата.

---

# 2. Используемое оборудование

Локальные модели запускались на компьютере со следующими характеристиками:

- **Оперативная память:** 32 ГБ
- **Видеокарта:** NVIDIA GeForce RTX 3080 12 ГБ
- **ОС:** Windows
- **IDE:** Android Studio
- **Локальный inference:** Ollama
- **Интеграция в IDE:** Continue 1.0.67

Такое оборудование уверенно запускает модели класса 7B–8B и позволяет использовать их в локальном чате и автокомплите.

---

# 3. Используемая связка

Для локального ассистента использовалась связка:

```text
Android Studio + Continue + Ollama + Llama 3.1 8B
```

Роли компонентов:

- **Ollama** запускает модель локально.
- **Continue** подключает модель к Android Studio.
- **Llama 3.1 8B** используется для чата, Edit/Apply и Agent mode.
- **Qwen2.5-Coder 7B** дополнительно проверялась как coding-модель.

Связка работает без передачи исходного кода во внешнее облако.

---

# 4. Установленные локальные модели

Во время эксперимента в Ollama были установлены:

```text
llama3.1:8b
qwen2.5-coder:7b
qwen2.5:7b-instruct-q5_K_M
qwen2.5:7b-instruct
llama3.2:3b
nomic-embed-text:latest
```

Основной моделью для итогового теста была выбрана (также использовалась модель qwen2.5-coder:7b):

```text
llama3.1:8b
```

Причины выбора:

- помещается в 12 ГБ VRAM;
- работает достаточно быстро;
- поддерживает чат;
- умеет формировать настоящие tool calls в Continue;
- стабильнее Qwen2.5-Coder 7B именно в Agent mode.

---

# 5. Конфигурация Continue

Пример конфигурации локальной модели:

```yaml
name: AI Assistant Local
version: 1.0.0
schema: v1

models:
  - name: Llama 3.1 8B Local
    provider: ollama
    model: llama3.1:8b
    apiBase: http://localhost:11434

    roles:
      - chat
      - edit
      - apply

    capabilities:
      - tool_use

    defaultCompletionOptions:
      contextLength: 8192
      temperature: 0.1
      top_p: 0.9
      maxTokens: 2048

context:
  - provider: file
  - provider: code
  - provider: diff
```

---

# 6. Подбор параметров генерации

## Temperature

Использовалось значение:

```yaml
temperature: 0.1
```

Для программирования низкая temperature оказалась наиболее подходящей.

При более высокой temperature модель чаще:

- придумывала несуществующие методы;
- предлагала лишние архитектурные изменения;
- отклонялась от инструкции;
- создавала более рискованные edits.

Значение `0.1` уменьшало случайность и делало ответы более повторяемыми.

## Top-p

Использовалось:

```yaml
top_p: 0.9
```

Это оставляло модели некоторую свободу выбора, но не делало ответы слишком случайными.

## Максимальная длина ответа

```yaml
maxTokens: 2048
```

Этого достаточно для:

- анализа небольшого файла;
- генерации локального изменения;
- составления краткого плана;
- объяснения кода.

## Размер контекста

Изначально использовался контекст 4096 токенов.

После включения Agent mode Continue стал добавлять в запрос:

- системный промпт;
- описание инструментов;
- правила проекта;
- AGENTS.md;
- историю чата;
- содержимое файлов;
- пользовательский запрос.

Из-за этого появилась ошибка:

```text
Message exceeds context limit
```

После этого контекст был увеличен до:

```yaml
contextLength: 8192
```

Рассматривалось значение 16384, но оно увеличивает расход памяти и замедляет работу. Для текущего компьютера 8192 оказалось наиболее разумным компромиссом.

---

# 7. Перенос правил из Дня 1

Правила проекта были перенесены в Continue.

Структура:

```text
AGENTS.md

.continue/
    rules/
        project-rules.md

    prompts/
        researcher.md
        implementer.md
        reviewer.md
        implementation-plan.md
        module-research.md
        android-validation.md
        bug-fix.md
        research.md
```

Дополнительно использовалось глобальное правило:

```text
~/.continue/rules/global-agent-workflow.md
```

Основные требования из правил:

- сначала читать `AGENTS.md`;
- не придумывать пути и символы;
- сначала исследовать проект;
- не считать план утверждённым без явного подтверждения;
- изменять только разрешённые файлы;
- делать минимальный patch;
- не использовать заглушки вида `... existing code ...`;
- после изменений запускать валидацию;
- отдельно сообщать изменённые файлы и выполненные команды.

Пример ограничений для implementer:

```text
- Execute only an explicitly approved plan.
- Never infer approval.
- Edit only approved files.
- Preserve existing declarations.
- Never replace the complete file unless explicitly requested.
- Do not use placeholders.
- Run focused validation after changes.
```

---

# 8. Настройка контекста проекта

Continue получал контекст через:

```yaml
context:
  - provider: file
  - provider: code
  - provider: diff
```

Модель могла видеть:

- выбранные файлы;
- открытый код;
- текущий diff;
- AGENTS.md;
- project rules;
- history чата.

На практике большой объём rules и prompts также стал проблемой.

Llama 3.1 8B иногда путала:

- prompt;
- skill;
- mode;
- реальный tool.

Например, вместо выполнения задачи модель сформировала:

```json
{
  "name": "plan",
  "parameters": {
    "description": "Implement voice-to-text message input for the chat screen"
  }
}
```

Но `plan` не являлся доступным инструментом Continue.

То есть модель выдумала tool call вместо реального чтения файлов и исследования проекта.

---

# 9. Проверка режима Chat

В Chat mode Llama 3.1 8B работала нормально на небольших задачах.

Примеры запросов:

```text
Explain how state flows from this composable to its ViewModel.
Do not modify files.
```

```text
Review this Kotlin function and suggest a minimal improvement.
```

```text
Explain what this StateFlow is used for.
```

В таких задачах модель:

- понимала Kotlin-код;
- могла объяснить существующую логику;
- давала полезные советы;
- работала полностью локально;
- не требовала подключения к интернету.

Но ответы всё равно требовали проверки, особенно когда вопрос касался Android lifecycle, Compose и архитектуры.

---

# 10. Проверка автокомплита

Autocomplete проверялся в Kotlin-файлах.

Пример:

```kotlin
fun String.isValidEmail(): Boolean {
```

После паузы Continue показывал inline-предложение, которое можно было принять клавишей `Tab`.

Также проверялся Compose-код:

```kotlin
@Composable
fun VoiceInputButton(
    isListening: Boolean,
    onClick: () -> Unit,
) {
```

На небольших фрагментах autocomplete работал удовлетворительно.

Лучшие сценарии:

- дописывание тела простой функции;
- генерация `when`;
- создание data class;
- boilerplate;
- KDoc;
- простые Compose-блоки;
- продолжение знакомого шаблона.

Хуже всего autocomplete работал, когда продолжение требовало понимания нескольких файлов или архитектуры проекта.

---

# 11. Проверка Qwen2.5-Coder 7B

Qwen2.5-Coder 7B тестировалась как специализированная coding-модель.

Плюсы:

- хорошо генерировала Kotlin;
- давала полезные объяснения;
- была пригодна для autocomplete;
- понимала локальные задачи в одном файле.

Главная проблема:

модель часто печатала tool calls как обычный JSON.

Например:

```json
{
  "name": "read_file"
}
```

или:

```json
{
  "name": "run_terminal_command"
}
```

Continue не воспринимал это как настоящий вызов инструмента.

В результате Qwen2.5-Coder 7B оказалась полезна для чата и генерации кода, но не подошла для полноценного Agent mode.

---

# 12. Проверка Llama 3.1 8B

Llama 3.1 8B показала себя лучше именно в формате tool calling.

Для проверки был выполнен минимальный тест:

```text
This is an explicitly authorized write-tool test.

Call the create_new_file tool now.

Create one new file in the workspace root:

continue-write-test.txt

The complete contents:

CONTINUE_WRITE_TEST

Do not explain.
Do not show JSON.
Do not output a code block.
Do not propose a plan.
Do not ask questions.
Use create_new_file now.
```

Результат:

- Continue показал настоящий tool card;
- появился action `Create file`;
- модель не просто напечатала JSON;
- tool call был распознан.

Это подтвердило, что Llama 3.1 8B технически умеет работать с инструментами Continue.

Однако это сработало только на очень простой и однозначной задаче.

---

# 13. Задача на генерацию фичи

Локальной модели была поставлена задача:

```text
Implement voice-to-text message input for the chat screen
```

Ожидалось, что модель:

1. прочитает AGENTS.md;
2. исследует проект;
3. найдёт реальный экран чата;
4. найдёт ViewModel;
5. найдёт UI state;
6. составит план;
7. внесёт изменения;
8. запустит проверку.

С первого раза модель не справилась.

---

# 14. Проблемы при реализации voice-to-text

## 14.1 Модель не удержала агентный workflow

Вместо последовательности:

```text
read rules
→ search files
→ read screen
→ read ViewModel
→ plan
→ edit
→ validate
```

модель:

- пыталась прочитать задачу как skill;
- выдумывала tool `plan`;
- не начинала нормальное исследование;
- теряла инструкцию;
- переходила сразу к генерации кода;
- придумывала архитектуру.

Пример:

```json
{
  "name": "plan",
  "parameters": {
    "description": "Implement voice-to-text message input for the chat screen"
  }
}
```

---

## 14.2 Архитектурно неверное решение

Модель предложила добавить в `ChatViewModel.kt`:

```kotlin
fun startVoiceRecognition() {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    intent.putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
    )
    intent.putExtra(
        RecognizerIntent.EXTRA_PROMPT,
        "Speak now"
    )
    startActivityForResult(
        intent,
        REQUEST_CODE_VOICE_RECOGNITION
    )
}

override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
) {
    super.onActivityResult(
        requestCode,
        resultCode,
        data
    )

    if (
        requestCode == REQUEST_CODE_VOICE_RECOGNITION &&
        resultCode == Activity.RESULT_OK
    ) {
        val speechResult =
            data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )

        if (speechResult != null) {
            messageInputField.setText(
                speechResult[0]
            )
        }
    }
}
```

Проблемы:

- `ViewModel` не должен запускать Activity;
- у `ViewModel` нет `startActivityForResult`;
- у `ViewModel` нет `onActivityResult`;
- нельзя вызывать `super.onActivityResult`;
- `messageInputField` не существует в Compose UI;
- UI не должен передаваться во ViewModel;
- использован устаревший API;
- не учтён Activity Result API;
- не учтено управление state.

Правильная архитектура должна быть такой:

```text
Compose UI
→ запускает RecognizerIntent
→ получает строку
→ передаёт текст во ViewModel
→ ViewModel обновляет UiState
```

---

## 14.3 Модель перезаписывала файл

Самая опасная проблема возникла при применении предложенного изменения.

Вместо минимального patch модель сформировала замену всего файла.

То есть ожидаемое изменение:

```diff
+ добавить две функции
```

фактически выглядело как:

```diff
- весь ChatViewModel.kt
+ две новые функции
```

После Apply удалялись:

- package;
- imports;
- поля класса;
- StateFlow;
- существующие методы;
- бизнес-логика;
- обработчики событий.

Оставался только сгенерированный фрагмент.

Это означало, что автоматическое применение edits небезопасно.

---

# 15. Защита от разрушительных изменений

После этого был введён безопасный workflow:

1. Перед Apply обязательно смотреть diff.
2. Отклонять изменение, если удаляется большая часть файла.
3. Давать модели только маленький выделенный участок.
4. Запрещать полную замену файла.
5. Использовать git checkpoint перед экспериментом.
6. Не разрешать automatic apply для сложных файлов.

Дополнительное правило:

```text
When editing an existing file:

- Never replace the complete file unless explicitly requested.
- Preserve every existing declaration not mentioned in the task.
- Never use placeholders such as "... existing code ...".
- Read the current file before proposing an edit.
- Produce the smallest possible patch.
- If the exact insertion point is unknown, stop.
```

Даже с этим правилом гарантий нет, потому что локальная модель может его нарушить.

---

# 16. Agent mode в Continue

## Что получилось

Llama 3.1 8B смогла:

- вызвать `create_new_file`;
- сформировать настоящий tool card;
- выполнить простой write-test;
- предложить edit;
- прочитать конкретный файл при точной инструкции.

## Что не получилось

Модель не смогла надёжно:

- исследовать большой проект;
- выполнить несколько tool calls подряд;
- удержать длинную задачу;
- отличать mode от tool;
- построить корректный implementation plan;
- изменить несколько файлов;
- запустить Gradle validation;
- исправить ошибки;
- полностью реализовать feature.

Итог:

```text
Простой tool call — работает.
Полный агентный workflow — не работает стабильно.
```

---

# 17. Ограничение Continue в Android Studio

В Continue были включены автоматические разрешения для:

```text
create_new_file
edit_existing_file
```

Но после tool call Android Studio всё равно показывала:

```text
Create file
```

или:

```text
Apply
```

То есть финальное применение требовало ручного подтверждения.

Фактически использовался режим:

```text
human-in-the-loop
```

Это снизило автономность, но одновременно защитило проект от разрушительных изменений.

---

# 18. Сравнение локальных моделей

| Модель | Chat | Autocomplete | Tool calls | Сложная feature | Скорость | Итог |
|---|---:|---:|---:|---:|---:|---|
| Llama 3.2 3B | слабый | базовый | практически нет | не справляется | очень высокая | слишком слабая |
| Qwen2.5-Coder 7B | хороший | хороший | печатает JSON вместо вызова | не справляется | высокая | хороша для кода, слаба как agent |


---

# 19. Сравнение Codex и локальной модели

| Критерий | Codex GPT-5.6-sol | Llama 3.1 8B |
|---|---|---|
| Качество кода | высокое | среднее |
| Понимание Android-архитектуры | хорошее | часто ошибается |
| Понимание проекта | хорошее | ограниченное |
| Работа с несколькими файлами | уверенная | нестабильная |
| Tool calling | стабильное | только на простых задачах |
| Agent mode | полноценный | ломается на длинной цепочке |
| Минимальные patches | обычно корректные | может заменить весь файл |
| Работа с Compose/ViewModel | хорошая | предложила Activity API во ViewModel |
| Современность API | чаще актуальные решения | предложила deprecated API |
| Скорость | быстрая, зависит от сети | локальная, но генерация медленнее |
| Работа без интернета | нет | да |
| Конфиденциальность | код отправляется в облако | код остаётся локально |
| Стоимость | зависит от сервиса | после загрузки модели бесплатно |
| Требования к железу | минимальные | нужны RAM и VRAM |
| Надёжность сложных edits | высокая | низкая |
| Необходимость ручной проверки | нужна | обязательна |

---

# 20. Справилась ли локальная модель с первого раза

Нет.

Итог по задаче voice-to-text:

```text
Llama 3.1 8B не смогла реализовать фичу.
```

Она не смогла:

- корректно исследовать проект;
- найти реальные точки интеграции;
- удержать план;
- вызвать нужную цепочку инструментов;
- предложить правильную Android-архитектуру;
- безопасно применить изменения.

Кроме того, модель предложила код, который не скомпилировался бы в ViewModel.

---

# 21. Справился ли облачный ассистент

Codex GPT-5.6-sol показал значительно лучший результат.

Он лучше:

- понимал AGENTS.md;
- исследовал проект;
- находил реальные файлы;
- учитывал архитектуру;
- работал с несколькими файлами;
- составлял связный план;
- делал более точные patches;
- удерживал агентный workflow;
- исправлял ошибки после проверки.

Для сложной feature облачный ассистент оказался практически незаменим.

---

# 22. Где локальной модели достаточно

Llama 3.1 8B подходит для:

- inline autocomplete;
- объяснения кода;
- генерации boilerplate;
- KDoc;
- небольших функций;
- data classes;
- простых unit test skeletons;
- локального code review;
- рефакторинга одного небольшого участка;
- поиска очевидной ошибки;
- работы без интернета;
- приватной работы с кодом.

Лучше всего модель работает, если:

- задача ограничена одним файлом;
- указан точный путь;
- указано точное имя функции;
- не требуется архитектурное решение;
- не требуется несколько tool calls;
- изменение небольшое;
- пользователь проверяет diff вручную.

---

# 23. Где облако незаменимо

Codex нужен для:

- реализации feature через несколько слоёв;
- сложной Android-архитектуры;
- Compose + ViewModel + navigation;
- Gradle;
- migrations;
- изменения нескольких файлов;
- анализа большого проекта;
- длительного agent workflow;
- запуска тестов;
- исправления ошибок;
- сложного планирования;
- безопасных patches;
- работы с большим контекстом.

---

# 24. Лучший workflow для локальной модели

## Шаг 1. Research only

```text
Research only.
Do not modify files.
Read AGENTS.md.
Locate the real chat screen, ViewModel and input state.
Report exact paths and symbols.
Do not invent missing details.
```

## Шаг 2. План без инструментов

```text
Using only confirmed repository findings,
write a minimal implementation plan.

Do not call tools.
Do not modify files.
Use exact existing paths.
```

## Шаг 3. Маленькое изменение

```text
Modify only the selected function.
Preserve all other declarations.
Return the smallest possible patch.
Do not rewrite the whole file.
```

## Шаг 4. Проверка diff

Перед применением нужно проверить:

- не удаляется ли большая часть файла;
- не исчезают ли imports;
- не исчезают ли существующие методы;
- нет ли несуществующих symbols;
- не нарушается ли архитектура;
- не используется ли deprecated API.

---

# 25. Рекомендуемые параметры

На текущем железе наиболее стабильной оказалась конфигурация:

```yaml
model: llama3.1:8b
contextLength: 8192
temperature: 0.1
top_p: 0.9
maxTokens: 2048
```

Преимущества:

- высокая скорость;
- помещается в видеопамять;
- работает без интернета;
- годится для chat;
- годится для autocomplete;
- умеет простой tool use.

Недостатки:

- слабый agent reasoning;
- маленький эффективный контекст;
- нестабильный tool workflow;
- риск полной замены файла;
- низкая надёжность архитектурных решений.

---

# 26. Итоговая рекомендация

Оптимальная схема использования:

```text
Autocomplete:
локальная coding-модель

Chat и объяснение кода:
Llama 3.1 8B

Маленькие edits:
локальная модель + ручная проверка diff

Сложные feature:
Codex GPT-5.6-sol

Agent mode:
облачный ассистент
```

---

# 27. Итоговый вывод

В рамках задания была настроена локальная связка:

```text
Ollama + Continue + Android Studio
```

Были выполнены основные требования:

- локальная LLM подключена к IDE;
- работает Chat mode;
- работает autocomplete;
- правила проекта перенесены в Continue;
- настроен контекст;
- подобраны параметры генерации;
- протестирована генерация feature;
- протестирован Agent mode;
- проведено сравнение с облачным ассистентом.

Главный вывод:

> Llama 3.1 8B достаточна как локальный код-помощник для автокомплита, объяснения кода, генерации небольших функций и локальных edits. Она может вызвать простой инструмент Continue, но не справляется надёжно с полной автономной реализацией сложной Android-фичи. На большой задаче модель теряет workflow, выдумывает tools, предлагает архитектурно неверный код и может перезаписать существующий файл целиком. Для сложных feature и agent mode Codex GPT-5.6-sol оказался значительно сильнее.

Финальная рекомендация:

```text
Локальную модель использовать для небольших,
быстрых и приватных задач.

Облачный ассистент использовать для сложной архитектуры,
многофайловых изменений и агентного режима.
```
