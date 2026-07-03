import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class IndexWriter {
    fun write(path: Path, chunks: List<Chunk>) {
        Files.createDirectories(path.parent)
        path.writeText(toJson(chunks))
    }

    private fun toJson(chunks: List<Chunk>): String {
        return chunks.joinToString(
            separator = ",\n",
            prefix = "[\n",
            postfix = "\n]\n"
        ) { chunk ->
            buildString {
                appendLine("  {")
                appendLine("    \"chunk_id\": \"${jsonEscape(chunk.chunkId)}\",")
                appendLine("    \"source\": \"${jsonEscape(chunk.source)}\",")
                appendLine("    \"title\": \"${jsonEscape(chunk.title)}\",")
                appendLine("    \"section\": ${nullableString(chunk.section)},")
                appendLine("    \"strategy\": \"${jsonEscape(chunk.strategy)}\",")
                appendLine("    \"text\": \"${jsonEscape(chunk.text)}\",")
                append("    \"embedding\": ${embeddingJson(chunk.embedding)}\n")
                append("  }")
            }
        }
    }

    private fun nullableString(value: String?): String {
        return value?.let { "\"${jsonEscape(it)}\"" } ?: "null"
    }

    private fun embeddingJson(embedding: List<Float>?): String {
        return embedding?.joinToString(prefix = "[", postfix = "]") { it.toString() } ?: "null"
    }

    private fun jsonEscape(value: String): String {
        return buildString(value.length) {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (char.code < 0x20) {
                            append("\\u")
                            append(char.code.toString(16).padStart(4, '0'))
                        } else {
                            append(char)
                        }
                    }
                }
            }
        }
    }
}
