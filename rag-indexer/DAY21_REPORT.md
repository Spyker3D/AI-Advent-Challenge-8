# Day 21 Report: Local RAG Indexing Pipeline

## Task

Implement a local JVM/Kotlin RAG indexing pipeline for the AI Assistant project.

The indexer is a developer tool. It runs on the local computer and does not generate embeddings inside the Android application.

## Used Documents

Input directories:

- `rag-indexer/input/docs/`
- `rag-indexer/input/project_sources/`

Used source types:

- Markdown documentation
- Kotlin source code
- Gradle files
- TOML files
- plain text files

Ignored directories:

- `build/`
- `.gradle/`
- `.git/`
- `.idea/`

## Pipeline

```text
Document Loader
        |
        v
Chunking
        |
        v
Embeddings
        |
        v
JSON Index
```

1. `DocumentLoader` recursively loads supported files from input directories.
2. `FixedSizeChunker` and `StructureChunker` generate two independent chunk sets.
3. `OllamaEmbeddingClient` calls `http://localhost:11434/api/embeddings` with model `nomic-embed-text`.
4. `IndexWriter` writes JSON indexes with chunk metadata and embeddings.
5. `ComparisonReportWriter` writes the strategy comparison report.
6. `SearchDemo` loads `structure_index.json` and performs cosine-similarity search.

## Strategy A: Fixed Size

- chunk size: 1200 chars
- overlap: 0 chars
- chunk id format: `fixed_000001`
- section: `fixed`

Pros:

- predictable chunk size
- simple implementation
- easy to tune

Cons:

- can split related context in the middle of a paragraph, class, or function
- does not preserve document structure in section metadata

## Strategy B: Structure-based

Markdown split points:

- `#`
- `##`
- `###`

Kotlin split points:

- `class`
- `interface`
- `object`
- `fun`

Large sections are split into smaller chunks with 200 chars overlap.

Pros:

- preserves Markdown headings and Kotlin declarations
- keeps related content together more often
- still bounds large sections by chunk size

Cons:

- chunk sizes are less uniform
- source parsing is heuristic

## Comparison

Current metrics from `rag-indexer/output/comparison.md`:

| Strategy | Chunks | Average size | Minimum size | Maximum size | Chunks without section |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fixed-size | 576 | 1017 | 4 | 1200 | 0 |
| Structure-based | 1108 | 582 | 5 | 1200 | 2 |

## Output

Generated files:

- `rag-indexer/output/fixed_index.json`
- `rag-indexer/output/structure_index.json`
- `rag-indexer/output/comparison.md`

Each chunk contains:

- `chunk_id`
- `source`
- `title`
- `section`
- `strategy`
- `text`
- `embedding`

## Conclusion

Fixed-size is simpler and produces predictable chunk sizes, but it can cut semantic meaning across boundaries.

Structure-based chunking preserves meaning better by using Markdown and Kotlin structure, but the chunks have more variable sizes.
