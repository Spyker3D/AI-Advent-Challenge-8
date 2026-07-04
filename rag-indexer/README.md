# RAG Indexer

Local JVM/Kotlin dev-tool for generating JSON indexes for the AI Assistant RAG pipeline.

The indexer runs on the developer machine. Embeddings are not generated inside the Android app.

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

## Input Sources

The loader reads:

- Markdown documentation
- Kotlin project code
- Gradle files
- TOML files
- plain text files

Input directories:

```text
rag-indexer/input/docs/
rag-indexer/input/project_sources/
```

Supported file types:

```text
.md, .txt, .kt, .kts, .gradle, .toml
```

Ignored directories:

```text
build/, .gradle/, .git/, .idea/
```

## Chunking Strategies

### Strategy A: Fixed Size

- chunk size: 1200 chars
- overlap: 0 chars
- chunk id format: `fixed_000001`
- section: `fixed`

This strategy is simple and predictable, but it can split meaning across chunk boundaries.

### Strategy B: Structure-based

Markdown sections are split by headings:

```text
#
##
###
```

Kotlin sections are split by declarations:

```text
class
interface
object
fun
```

If a section is larger than the limit, it is split into smaller chunks with 200 chars overlap.

This strategy preserves meaning better, but chunk sizes are less uniform.

## Install Ollama

Install Ollama from:

```text
https://ollama.com/download
```

Start Ollama and download the embedding model:

```bash
ollama pull nomic-embed-text
```

The indexer sends embedding requests to:

```text
http://localhost:11434/api/embeddings
```

Request model:

```text
nomic-embed-text
```

## Run Indexer

From the repository root:

```bash
./gradlew :rag-indexer:run
```

On Windows:

```powershell
.\gradlew.bat :rag-indexer:run
```

If the local Gradle wrapper has a classpath issue, run through the wrapper jar:

```powershell
java "-Dkotlin.compiler.execution.strategy=in-process" -jar gradle\wrapper\gradle-wrapper.jar :rag-indexer:run
```

The indexer prints:

- number of loaded documents
- number of chunks
- embedding size
- first two chunks with metadata and preview

It also validates that each JSON chunk contains:

- `chunk_id`
- `source`
- `title`
- `section`
- `strategy`
- `text`
- `embedding`

## Output

Generated files:

```text
rag-indexer/output/fixed_index.json
rag-indexer/output/structure_index.json
rag-indexer/output/comparison.md
```

## Search Demo

The search demo loads:

```text
rag-indexer/output/structure_index.json
```

It embeds the query with Ollama, computes cosine similarity against each chunk embedding, sorts by score, and prints TOP-5 results.

Run with a query:

```powershell
.\gradlew.bat :rag-indexer:searchDemo -Pquery="rememberSaveable"
```

Wrapper jar fallback:

```powershell
java "-Dkotlin.compiler.execution.strategy=in-process" -jar gradle\wrapper\gradle-wrapper.jar :rag-indexer:searchDemo -Pquery="rememberSaveable"
```

## Validate Existing Index

To validate metadata in already generated indexes without regenerating embeddings:

```powershell
.\gradlew.bat :rag-indexer:validateIndex
```

Wrapper jar fallback:

```powershell
java "-Dkotlin.compiler.execution.strategy=in-process" -jar gradle\wrapper\gradle-wrapper.jar :rag-indexer:validateIndex
```
