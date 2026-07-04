# Day 23. Reranking and Filtering Comparison

## Config

Baseline:
- topK = 5
- rewrite = false
- filter = false
- rerank = false

Improved:
- candidateTopK = 20
- finalTopK = 5
- similarityThreshold = 0.55
- rewrite = true
- filter = true
- rerank = true
- scoring = 0.70 cosine + 0.20 keyword + 0.10 metadata

## Question 1

Question:
Р§С‚Рѕ С‚Р°РєРѕРµ rememberSaveable РІ Jetpack Compose?

Rewritten query:
Jetpack Compose rememberSaveable state saving configuration changes Activity recreation Bundle process death

### Baseline RAG

Sources:
- Run with Day23 Improved Retrieval OFF to capture source / section / cosineScore.

Answer:
- Manual evaluation pending.

### Improved RAG

Sources before filter:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top candidates before filter.

Sources after filter/rerank:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top results after rerank.

Answer:
- Manual evaluation pending.

### Notes

- Expected source appeared in baseline: manual
- Expected source appeared in improved: manual
- Answer improved: manual

## Question 2

Question:
РљРѕРіРґР° РёСЃРїРѕР»СЊР·РѕРІР°С‚СЊ LaunchedEffect?

Rewritten query:
Jetpack Compose LaunchedEffect side effects coroutine composable lifecycle recomposition

### Baseline RAG

Sources:
- Run with Day23 Improved Retrieval OFF to capture source / section / cosineScore.

Answer:
- Manual evaluation pending.

### Improved RAG

Sources before filter:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top candidates before filter.

Sources after filter/rerank:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top results after rerank.

Answer:
- Manual evaluation pending.

### Notes

- Expected source appeared in baseline: manual
- Expected source appeared in improved: manual
- Answer improved: manual

## Question 3

Question:
Р§РµРј StateFlow РѕС‚Р»РёС‡Р°РµС‚СЃСЏ РѕС‚ SharedFlow?

Rewritten query:
Kotlin Coroutines StateFlow SharedFlow hot flow state replay collectors UI state events Android

### Baseline RAG

Sources:
- Run with Day23 Improved Retrieval OFF to capture source / section / cosineScore.

Answer:
- Manual evaluation pending.

### Improved RAG

Sources before filter:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top candidates before filter.

Sources after filter/rerank:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top results after rerank.

Answer:
- Manual evaluation pending.

### Notes

- Expected source appeared in baseline: manual
- Expected source appeared in improved: manual
- Answer improved: manual

## Question 4

Question:
Р—Р°С‡РµРј РЅСѓР¶РµРЅ Repository Pattern?

Rewritten query:
Repository Pattern Android clean architecture data layer domain layer ViewModel data source abstraction

### Baseline RAG

Sources:
- Run with Day23 Improved Retrieval OFF to capture source / section / cosineScore.

Answer:
- Manual evaluation pending.

### Improved RAG

Sources before filter:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top candidates before filter.

Sources after filter/rerank:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top results after rerank.

Answer:
- Manual evaluation pending.

### Notes

- Expected source appeared in baseline: manual
- Expected source appeared in improved: manual
- Answer improved: manual

## Question 5

Question:
РљР°Рє СЂР°Р±РѕС‚Р°РµС‚ streaming РѕС‚РІРµС‚РѕРІ LLM?

Rewritten query:
LLM streaming response Chat Module token stream Flow coroutine OpenRouter HTTP response UI update

### Baseline RAG

Sources:
- Run with Day23 Improved Retrieval OFF to capture source / section / cosineScore.

Answer:
- Manual evaluation pending.

### Improved RAG

Sources before filter:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top candidates before filter.

Sources after filter/rerank:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top results after rerank.

Answer:
- Manual evaluation pending.

### Notes

- Expected source appeared in baseline: manual
- Expected source appeared in improved: manual
- Answer improved: manual

## Question 6

Question:
Р”Р»СЏ С‡РµРіРѕ РёСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ Ollama nomic-embed-text?

Rewritten query:
Ollama nomic-embed-text embeddings vector search RAG local embedding model Android AI Assistant

### Baseline RAG

Sources:
- Run with Day23 Improved Retrieval OFF to capture source / section / cosineScore.

Answer:
- Manual evaluation pending.

### Improved RAG

Sources before filter:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top candidates before filter.

Sources after filter/rerank:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top results after rerank.

Answer:
- Manual evaluation pending.

### Notes

- Expected source appeared in baseline: manual
- Expected source appeared in improved: manual
- Answer improved: manual

## Question 7

Question:
Р§РµРј structure-based chunking Р»СѓС‡С€Рµ fixed-size chunking?

Rewritten query:
RAG structure-based chunking fixed-size chunking sections semantic boundaries document structure retrieval quality

### Baseline RAG

Sources:
- Run with Day23 Improved Retrieval OFF to capture source / section / cosineScore.

Answer:
- Manual evaluation pending.

### Improved RAG

Sources before filter:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top candidates before filter.

Sources after filter/rerank:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top results after rerank.

Answer:
- Manual evaluation pending.

### Notes

- Expected source appeared in baseline: manual
- Expected source appeared in improved: manual
- Answer improved: manual

## Question 8

Question:
Р”Р»СЏ С‡РµРіРѕ РЅСѓР¶РµРЅ Retrofit Interceptor?

Rewritten query:
Retrofit OkHttp Interceptor Authorization header API key logging request response Android network layer

### Baseline RAG

Sources:
- Run with Day23 Improved Retrieval OFF to capture source / section / cosineScore.

Answer:
- Manual evaluation pending.

### Improved RAG

Sources before filter:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top candidates before filter.

Sources after filter/rerank:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top results after rerank.

Answer:
- Manual evaluation pending.

### Notes

- Expected source appeared in baseline: manual
- Expected source appeared in improved: manual
- Answer improved: manual

## Question 9

Question:
Р§С‚Рѕ С‚Р°РєРѕРµ MCP?

Rewritten query:
MCP Model Context Protocol tools resources agent orchestration Android AI Assistant

### Baseline RAG

Sources:
- Run with Day23 Improved Retrieval OFF to capture source / section / cosineScore.

Answer:
- Manual evaluation pending.

### Improved RAG

Sources before filter:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top candidates before filter.

Sources after filter/rerank:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top results after rerank.

Answer:
- Manual evaluation pending.

### Notes

- Expected source appeared in baseline: manual
- Expected source appeared in improved: manual
- Answer improved: manual

## Question 10

Question:
РљР°Рє СЂР°Р±РѕС‚Р°РµС‚ RAG pipeline?

Rewritten query:
RAG pipeline query embedding vector search retrieval chunks prompt builder LLM answer sources Android AI Assistant

### Baseline RAG

Sources:
- Run with Day23 Improved Retrieval OFF to capture source / section / cosineScore.

Answer:
- Manual evaluation pending.

### Improved RAG

Sources before filter:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top candidates before filter.

Sources after filter/rerank:
- Run with Day23 Improved Retrieval ON and inspect `RAG_DAY23` top results after rerank.

Answer:
- Manual evaluation pending.

### Notes

- Expected source appeared in baseline: manual
- Expected source appeared in improved: manual
- Answer improved: manual

## Required Day 23 Spot Check

Question:
Как устроен поток отправки сообщения в Android AI Assistant?

Rewritten query:
message sending flow Android AI Assistant Chat Module ChatScreen ChatViewModel ChatRepository OpenRouter HTTP request LLM response prompt builder

### Baseline RAG

Sources:
- Run with Day23 Improved Retrieval OFF to capture source / section / cosineScore.

Answer:
- Manual evaluation pending.

### Improved RAG

Sources before filter:
- Expected to include candidates from `03_ChatModule.md`, `02_Architecture.md`, and project source chunks around `ChatViewModel`.

Sources after filter/rerank:
- Expected relevant ChatModule/Architecture chunks should rank higher than baseline because rewrite adds `ChatScreen`, `ChatViewModel`, `ChatRepository`, `HTTP Request`, `LLM`, and `Response` terms.

Answer:
- Manual evaluation pending.

### Notes

- Expected source appeared in baseline: manual
- Expected source appeared in improved: manual
- Answer improved: manual
