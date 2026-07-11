# AI Assistant

Android приложение чат-ассистента с поддержкой различных AI моделей через OpenRouter API.

## 🏗️ Архитектура

Приложение использует Clean Architecture с модульной структурой:

### Модули:
- **app** - Главный модуль приложения
- **core:domain** - Бизнес-логика и интерфейсы
- **core:data** - Реализация repositories и работа с API
- **core:network** - Сетевые компоненты
- **core:ui** - Переиспользуемые UI компоненты
- **feature:chat** - Экран чата
- **feature:settings** - Экран настроек

### Технологический стек:
- **Kotlin** - Язык программирования
- **Jetpack Compose** - UI framework
- **MVVM** - Архитектурный паттерн
- **Clean Architecture** - Архитектурный подход
- **Dagger 2** - Dependency Injection (без Hilt)
- **Coroutines & StateFlow** - Асинхронность и state management
- **Retrofit + OkHttp** - Сетевые запросы
- **Gson** - JSON сериализация
- **Navigation Compose** - Навигация
- **DataStore** - Сохранение настроек

## 🚀 Функциональность

### Главный экран (ChatScreen):
- ✅ LazyColumn с сообщениями
- ✅ TextField для ввода сообщений
- ✅ Кнопка Send
- ✅ Индикатор загрузки
- ✅ Поддержка различных AI моделей

### Экран настроек (SettingsScreen):
- ✅ Выбор AI модели (GPT-4o Mini, DeepSeek, Kimi K2)
- ✅ Слайдер Temperature (0.0-2.0)
- ✅ Настройка Max Tokens
- ✅ Редактирование System Prompt
- ✅ Сохранение настроек между запусками

### API интеграция:
- ✅ OpenRouter API интеграция
- ✅ POST `/chat/completions` endpoint
- ✅ Поддержка Bearer токен авторизации
- ✅ Обработка ошибок сети и API

## 🛠️ Настройка

### API Key:
1. Получите API ключ на [OpenRouter](https://openrouter.ai/)
2. Откройте файл `core/data/src/main/java/com/aiassistant/core/data/repository/ChatRepositoryImpl.kt`
3. Замените `YOUR_OPENROUTER_API_KEY` на ваш ключ

### Сборка:
```bash
./gradlew build
```

## 📱 UI/UX

- **Material 3** дизайн система
- **Адаптивный интерфейс** с поддержкой различных размеров экранов
- **Пузыри сообщений** - пользователь справа, ассистент слева
- **Индикаторы состояния** - loading, ошибки через Snackbar
- **Навигация** между экранами чата и настроек

## 🏛️ Слои архитектуры

### Domain Layer:
- `AiModel` - enum с поддерживаемыми моделями
- `Message`, `ChatSettings` - основные entity
- `ChatRepository`, `SettingsRepository` - интерфейсы
- `SendMessageUseCase`, `GetChatSettingsUseCase` - use cases

### Data Layer:
- `ChatRequestDto`, `ChatResponseDto` - DTO для API
- `OpenRouterApi` - Retrofit интерфейс
- `ChatRepositoryImpl` - реализация репозитория
- `SettingsDataStore` - работа с DataStore

### Presentation Layer:
- `ChatViewModel`, `SettingsViewModel` - ViewModels с StateFlow
- `ChatUiState`, `SettingsUiState` - состояния UI
- `ChatScreen`, `SettingsScreen` - Compose экраны

## 🔧 Dependency Injection

Использует **Dagger 2** (без Hilt):
- `NetworkModule` - сетевые компоненты
- `DataModule` - repositories
- `ViewModelModule` - ViewModels
- `AppComponent` - главный компонент

## ✨ Особенности

- **Production-ready код** с соблюдением SOLID принципов
- **Single Source of Truth** для состояний
- **Immutable UiState** для предсказуемости
- **Обработка ошибок** с пользовательскими сообщениями
- **Dependency Inversion** для тестируемости
- **Модульная архитектура** для масштабируемости

## 📋 Поддерживаемые модели

1. **OpenAI GPT-4o Mini** (`openai/gpt-4o-mini`)
2. **DeepSeek Chat** (`deepseek/deepseek-chat`) 
3. **Moonshot AI Kimi K2** (`moonshotai/kimi-k2`)

## 📝 Структура проекта

```
app/
├── di/                 # Dagger модули
├── navigation/         # Navigation Compose
└── ui/theme/          # Тема приложения

core/
├── domain/            # Бизнес логика
├── data/              # Реализация репозиториев
├── network/           # Сетевые компоненты
└── ui/                # UI компоненты

feature/
├── chat/              # Чат функциональность
└── settings/          # Настройки приложения
```

## Day 17: MCP tool demo

Реализована демонстрация вызова локального MCP tool из Android-приложения.

Android-приложение:
- получает список tools через `tools/list`;
- вызывает tool `get_task_status` через `tools/call`;
- отображает результат на экране.

Локальный MCP endpoint для Android Emulator:

```text
http://10.0.2.2:3000/mcp
```

Перед запуском Android-приложения нужно запустить локальный MCP server:

```bash
cd mcp-server
npm install
npm start
```

## Day 18: Periodic MCP tool

Реализован MCP-инструмент с периодическим выполнением.

Что делает:

- MCP server на VPS раз в 60 секунд собирает текущую погоду через Open-Meteo API;
- данные сохраняются в `mcp-server/data/weather-history.json`;
- tool `get_weather_summary` возвращает агрегированную сводку;
- Android-приложение вызывает MCP tool и отображает результат.

MCP endpoint:

```text
http://31.129.110.10:3000/mcp
```

Tools:

- `collect_weather_now`
- `get_weather_history`
- `get_weather_summary`

## Day 18: 24/7 Agent Monitoring

Day 18 реализован в двух частях:

1. MCP server на VPS работает через PM2 и сам собирает weather data по расписанию.
2. Android agent может запускать auto refresh mode и каждые 10 секунд получать агрегированную сводку через MCP tool `get_weather_summary`.

Для демонстрации используется интервал 10 секунд. В реальном режиме интервал можно изменить через константу `AUTO_REFRESH_INTERVAL_MS`.

Проверка:

- в логах VPS видно `Weather collected`;
- в Android-приложении можно нажать `Запустить авто-сводку`;
- summary обновляется автоматически каждые 10 секунд.

## Day 19: MCP Tool Composition Pipeline

Реализован автоматический pipeline из нескольких MCP tools.

Основной сценарий:
Пользователь пишет в чат:

`Подготовь отчет о погоде в Санкт-Петербурге`

Android agent:

1. Определяет, что запрос относится к weather pipeline.
2. Строит цепочку MCP tools через LLM planner или fallback planner.
3. Последовательно вызывает MCP tools.
4. Передает результат одного tool в следующий через `$previous`.
5. Показывает финальный ответ в чате.

MCP tools:

- `get_weather_by_city` — получает текущую погоду по городу.
- `create_weather_report` — формирует отчет по weather JSON.
- `save_report_to_file` — сохраняет отчет в файл и возвращает URL.

Pipeline:

`get_weather_by_city → create_weather_report → save_report_to_file`

MCP server не вызывает LLM. Все tools находятся на MCP server.

Сохраненные отчеты доступны по URL:

`http://31.129.110.10:3000/reports/<fileName>`

## Day 20: Orchestration MCP

Реализована оркестрация нескольких MCP-серверов.

MCP servers:

- Weather MCP: `http://31.129.110.10:3000/mcp`
- Notes MCP: `http://31.129.110.10:3001/mcp`
- Tasks MCP: `http://31.129.110.10:3002/mcp`

Example request:

`Подготовь отчет о погоде в Санкт-Петербурге, сохрани его в заметки и создай задачу проверить погоду завтра`

Pipeline:

1. `weather.get_weather_by_city`
2. `weather.create_weather_report`
3. `notes.save_note`
4. `tasks.create_task`

Android agent:

- gets `tools/list` from all MCP servers;
- sends tools + user request to LLM planner;
- receives JSON pipeline with `serverId` + `toolName`;
- routes each tool call to the correct MCP server;
- passes `$previous` between steps;
- shows final result in chat.

MCP servers do not call LLM.

New server folders:

- `mcp-notes-server`
- `mcp-tasks-server`

Run on VPS:

```bash
cd mcp-server && npm start
cd mcp-notes-server && npm start
cd mcp-tasks-server && npm start
```

## Day 26: Local LLM via Ollama

Implemented local LLM support via Ollama.

### Run local model

```bash
ollama pull qwen2.5:7b-instruct
ollama run qwen2.5:7b-instruct
ollama list
```

### API check

```bash
curl http://localhost:11434/api/generate -d '{
  "model": "qwen2.5:7b-instruct",
  "prompt": "Привет! Ответь одним предложением.",
  "stream": false
}'
```

### Android Emulator URL

In Android app settings:

```text
Local model: qwen2.5:7b-instruct
```

```text
http://10.0.2.2:11434
```

For a real phone on the same Wi-Fi network, use:

```text
http://COMPUTER_IP:11434
```
