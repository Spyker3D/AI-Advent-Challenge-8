import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class ComparisonReportWriter {
    fun write(path: Path, fixedChunks: List<Chunk>, structureChunks: List<Chunk>) {
        Files.createDirectories(path.parent)

        val fixed = Stats.from(fixedChunks)
        val structure = Stats.from(structureChunks)

        path.writeText(
            """
            |# RAG Chunking Strategy Comparison
            |
            |## Metrics
            |
            || Strategy | Chunks | Average size | Minimum size | Maximum size | Chunks without section |
            || --- | ---: | ---: | ---: | ---: | ---: |
            || Fixed-size | ${fixed.count} | ${fixed.averageSize} | ${fixed.minSize} | ${fixed.maxSize} | ${fixed.withoutSection} |
            || Structure-based | ${structure.count} | ${structure.averageSize} | ${structure.minSize} | ${structure.maxSize} | ${structure.withoutSection} |
            |
            |## Fixed-size
            |
            |Pros:
            |- Predictable chunk size.
            |- Simple and fast to generate.
            |- Easy to tune by changing one size parameter.
            |
            |Cons:
            |- Can split related context in the middle of a paragraph, class, or function.
            |- Does not preserve document structure in the section metadata.
            |
            |## Structure-based
            |
            |Pros:
            |- Preserves Markdown headings and Kotlin declarations in section metadata.
            |- Keeps related content together more often.
            |- Large sections are still bounded by a maximum chunk size.
            |
            |Cons:
            |- Chunk sizes are less uniform.
            |- Parsing is heuristic and may miss unusual source formatting.
            |
            |## Summary
            |
            |Fixed-size проще, но может резать смысл.
            |
            |Structure-based лучше сохраняет смысл, но чанки получаются разного размера.
            |""".trimMargin() + "\n"
        )
    }

    private data class Stats(
        val count: Int,
        val averageSize: Int,
        val minSize: Int,
        val maxSize: Int,
        val withoutSection: Int
    ) {
        companion object {
            fun from(chunks: List<Chunk>): Stats {
                val sizes = chunks.map { it.text.length }
                return Stats(
                    count = chunks.size,
                    averageSize = if (sizes.isEmpty()) 0 else sizes.average().toInt(),
                    minSize = sizes.minOrNull() ?: 0,
                    maxSize = sizes.maxOrNull() ?: 0,
                    withoutSection = chunks.count { it.section.isNullOrBlank() }
                )
            }
        }
    }
}
