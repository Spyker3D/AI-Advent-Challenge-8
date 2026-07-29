# Критерии оценки модели

## 1. Accuracy

Доля правильно классифицированных примеров:

accuracy = correct_predictions / total_predictions

Основной критерий качества.

## 2. Exact match

Ответ считается корректным только если модель вернула ровно одну допустимую метку.

Корректно:

OPENAI_TIMEOUT

Некорректно:

Метка: OPENAI_TIMEOUT

Некорректно:

OPENAI_TIMEOUT, потому что запрос выполнялся слишком долго.

## 3. Format compliance

Доля ответов, которые содержат только одну метку без пояснений.

## 4. Per-class accuracy

Точность отдельно по каждой категории:

- NETWORK_UNAVAILABLE
- OPENAI_RATE_LIMIT
- OPENAI_TIMEOUT
- EMPTY_AI_RESPONSE
- LOCAL_HISTORY_UNAVAILABLE

## Цель fine-tuning

Fine-tuned модель должна:

- повысить accuracy;
- чаще соблюдать точный формат;
- реже путать похожие категории;
- возвращать только одну допустимую метку.