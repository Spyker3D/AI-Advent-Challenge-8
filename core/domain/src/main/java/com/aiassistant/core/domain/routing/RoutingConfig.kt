package com.aiassistant.core.domain.routing

object RoutingConfig {
    const val SMALL_MODEL = "llama3.2:3b"
    const val LARGE_MODEL = "qwen2.5:7b-instruct"
    const val CONFIDENCE_THRESHOLD = 0.80
    const val MIN_ANSWER_LENGTH = 20
    const val LONG_REQUEST_THRESHOLD = 400
    const val SMALL_MODEL_MAX_TOKENS = 700
    const val LARGE_MODEL_MAX_TOKENS = 1000

    val COMPLEXITY_MARKERS = listOf(
        "проанализируй архитектуру", "подробный план", "сравни несколько", "противоречив",
        "миграция", "race condition", "выбери оптимальный", "предложи стратегию"
    )

    const val SMALL_MODEL_SYSTEM_PROMPT = """Ты модель первого уровня в системе маршрутизации.
Ответь на запрос пользователя и оцени, способен ли ты дать надёжный и достаточный ответ.
Верни только JSON:
{"answer":"ответ пользователю","confidence":0.0,"needs_escalation":false,"reason":"краткое объяснение"}
Confidence: 0.90–1.00 — простой однозначный запрос; 0.75–0.89 — небольшая неопределённость;
0.50–0.74 — нужен анализ или не хватает контекста; 0.00–0.49 — надёжный ответ невозможен.
Установи needs_escalation=true для анализа архитектуры, сравнения сложных решений, противоречий,
нехватки контекста, неоднозначности, длинного плана, глубокого рассуждения, неуверенности в фактах
или риска неполного ответа. Не завышай confidence за правдоподобную формулировку.
confidence должен быть числом 0.0..1.0, needs_escalation — boolean, answer и reason — непустыми.
Не используй Markdown и не добавляй текст до или после JSON."""

    const val LARGE_MODEL_SYSTEM_PROMPT = """Ты модель второго уровня в системе маршрутизации.
Сформируй точный, полный и понятный ответ пользователю.
Не упоминай внутренний routing, confidence, fallback, названия моделей или причину эскалации."""
}
