# Day 22 RAG ON/OFF Comparison

## Setup

- Index: `app/src/main/assets/rag/structure_index.json`
- Source index: `rag-indexer/output/structure_index.json`
- Embedding model: `nomic-embed-text`
- Android emulator Ollama URL: `http://10.0.2.2:11434/api/embeddings`
- Retrieval: cosine similarity, top-5 chunks

## Evaluation Questions

The control questions are stored in:

```text
rag-indexer/evaluation/day22_questions.json
```

## Expected Behavior

| Mode | Behavior |
| --- | --- |
| RAG OFF | The user question is sent directly to the LLM with the existing chat context. |
| RAG ON | The app embeds the question, searches `structure_index.json`, builds a RAG prompt with top-5 chunks, sends it to the LLM, and shows sources under the answer. |

## Quality Comparison

| Question Type | RAG OFF | RAG ON |
| --- | --- | --- |
| Project-specific documentation | May answer generically or miss project wording. | Uses local Markdown documentation and shows matching sources. |
| Compose concepts such as `rememberSaveable` | Can answer from model knowledge. | Grounds the answer in `05_Compose.md` and reports the retrieved section. |
| Architecture questions | Can answer generally. | Uses local project sources and docs for Repository, MCP, and pipeline details. |
| Chunking questions | Can answer conceptually. | Uses `10_RAG.md` and Day 21 documentation chunks when available. |

## Demo Result To Check

Question:

```text
Что такое rememberSaveable в Jetpack Compose?
```

Expected RAG ON sources should include:

```text
input/docs/05_Compose.md / 9. rememberSaveable
```

## Conclusion

RAG OFF is useful as a baseline direct LLM mode.

RAG ON is better for this project demo because the answer is grounded in the local knowledge base and the UI shows which chunks were used.
