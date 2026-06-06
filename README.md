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