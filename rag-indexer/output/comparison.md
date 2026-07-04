# RAG Chunking Strategy Comparison

## Metrics

| Strategy | Chunks | Average size | Minimum size | Maximum size | Chunks without section |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fixed-size | 581 | 1018 | 4 | 1200 | 0 |
| Structure-based | 1094 | 587 | 11 | 1200 | 2 |

## Fixed-size

Pros:
- Predictable chunk size.
- Simple and fast to generate.
- Easy to tune by changing one size parameter.

Cons:
- Can split related context in the middle of a paragraph, class, or function.
- Does not preserve document structure in the section metadata.

## Structure-based

Pros:
- Preserves Markdown headings and Kotlin declarations in section metadata.
- Keeps related content together more often.
- Large sections are still bounded by a maximum chunk size.

Cons:
- Chunk sizes are less uniform.
- Parsing is heuristic and may miss unusual source formatting.

## Summary

Fixed-size проще, но может резать смысл.

Structure-based лучше сохраняет смысл, но чанки получаются разного размера.

