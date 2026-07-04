data class Chunk(
    val chunkId: String,
    val source: String,
    val title: String,
    val section: String?,
    val strategy: String,
    val text: String,
    val embedding: List<Float>? = null
)
