# Day 25. Mini-chat with RAG + Task Memory Report

## Config

- Retrieval pipeline: Day24 pipeline is preserved.
- RAG runs on every RAG-enabled user turn.
- Query rewrite receives original question, Task Memory summary, and recent history.
- Prompt includes Task Memory, recent conversation, retrieved RAG context, original question.
- Low confidence / ambiguous retrieval still returns the local unknown answer without calling OpenRouter.
- Task Memory update runs only after successful LLM answers, not after local unknown answers.
- Clarifications and terms are stored inside `currentState` sections.

## Scenario A. Day25 Implementation

Goal:
Check that the assistant keeps the implementation goal and constraints during a long mini-chat.

Messages:

1. Хочу сделать Day25.
2. Используем существующий Working Memory.
3. Не хочу отдельную память.
4. Нужно, чтобы TaskContext обновлялся автоматически.
5. А как это встроить в pipeline?
6. А RAG должен запускаться каждый раз?
7. А что хранить в constraints?
8. А что хранить в decisions?
9. А как потом показать источники?
10. Сформируй итоговую архитектуру.

Expected memory:

- Goal: реализовать Day25 mini-chat with RAG + memory.
- Constraint: использовать существующий Working Memory.
- Constraint: не создавать отдельную память.
- Decision: TaskContext обновляется автоматически после успешного ответа ассистента.
- Decision: RAG retrieval выполняется на каждом новом RAG-вопросе.
- Term: TaskContext.
- Term: Sources and Quotes.

Expected final answer:

- Mentions Task Memory + recent history before RAG context.
- Keeps RAG as required retrieval source.
- Explains automatic TaskMemoryUpdater after assistant answer.
- Includes Sources and Quotes from RAG context.

Checks:

| Check | Result |
|---|---|
| Recent history included | ✔ |
| Task Memory included | ✔ |
| RAG retrieval every turn | ✔ |
| Existing Working Memory reused | ✔ |
| Separate memory avoided | ✔ |
| Sources present | ✔ |
| Quotes present | ✔ |

## Scenario B. RAG Quality / Anti-hallucination

Goal:
Check that the assistant remembers Day23/Day24 retrieval decisions and keeps anti-hallucination behavior.

Messages:

1. Объясни Day23.
2. Запомним: используем query rewrite.
3. Запомним: rerank через cosine/keyword/metadata.
4. Для Day24 нужны цитаты.
5. Если confidence низкий — не ходим в LLM.
6. А как проверить на 10 вопросах?
7. А что делать с общим вопросом?
8. А как выводить sources?
9. А как сохранить это в отчете?
10. Подведи итог.

Expected memory:

- Decision: Day23 improved retrieval uses query rewrite.
- Decision: hybrid rerank uses cosine, keyword, metadata.
- Decision: Day24 answers require sources and quotes.
- Constraint: low confidence must return local unknown answer and skip OpenRouter.
- Term: confidence threshold.
- Term: similarity threshold.

Expected final answer:

- Summarizes Day23 rewrite/filter/rerank.
- Summarizes Day24 confidence, sources, quotes, local unknown answer.
- Explains evaluation report expectations.
- Includes Sources and Quotes from RAG context.

Checks:

| Check | Result |
|---|---|
| Query rewrite remembered | ✔ |
| Hybrid rerank remembered | ✔ |
| Confidence threshold remembered | ✔ |
| Low-confidence local answer preserved | ✔ |
| Sources present | ✔ |
| Quotes present | ✔ |
| Report path documented | ✔ |

## Implementation Notes

- `RecentHistoryFormatter` limits history to recent messages and a bounded character budget.
- `TaskMemoryPromptFormatter` serializes existing TaskContext into the RAG prompt.
- `TaskMemoryUpdater` extracts stable memory as JSON after successful LLM answers.
- `TaskMemoryMerger` deduplicates goals, constraints, decisions, clarifications, and terms.
- `RagPromptBuilder` now receives Task Memory and recent history without changing retrieval scoring.
