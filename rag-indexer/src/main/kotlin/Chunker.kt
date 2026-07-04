import java.nio.file.Path

data class LoadedDocument(
    val path: Path,
    val source: String,
    val title: String,
    val extension: String,
    val text: String
)

interface Chunker {
    fun chunk(documents: List<LoadedDocument>): List<Chunk>
}
