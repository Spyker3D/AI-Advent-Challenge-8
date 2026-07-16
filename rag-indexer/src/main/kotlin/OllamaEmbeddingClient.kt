/** Compatibility adapter for the original rag-indexer entry points. */
class OllamaEmbeddingClient(
    baseUrl: String = "http://localhost:11434",
    model: String = "nomic-embed-text:latest"
) {
    private val delegate = com.aiassistant.rag.OllamaEmbeddingClient(com.aiassistant.rag.OllamaEmbeddingConfig(baseUrl, model))
    fun embed(text: String): List<Float> = kotlinx.coroutines.runBlocking { delegate.embed(listOf(text)).single() }
}
