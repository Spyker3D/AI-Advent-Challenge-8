fun Chunk.embeddingContent(): String {
    return buildString {
        appendLine("Title: $title")
        appendLine("Source: $source")
        appendLine("Section: ${section.orEmpty()}")
        appendLine()
        append(text)
    }
}
