package com.aiassistant.core.domain.rag

import javax.inject.Inject

class RagPromptBuilder @Inject constructor() {
    fun build(
        question: String,
        results: List<RagSearchResult>
    ): String {
        val context = results.mapIndexed { index, result ->
            val chunk = result.chunk
            """
            |[${index + 1}]
            |
            |Source:
            |${chunk.source}
            |
            |Section:
            |${chunk.section ?: "N/A"}
            |
            |Chunk:
            |${chunk.chunkId}
            |
            |Quote:
            |${chunk.toPromptQuote()}
            |""".trimMargin()
        }.joinToString(separator = "\n\n")

        return """
            |You are Android AI Assistant.
            |
            |Use only the provided context.
            |Do not invent facts.
            |Do not invent sources.
            |If the context does not contain the answer, say that the knowledge base does not contain enough information.
            |In the Answer section, synthesize all directly relevant facts from the selected quotes.
            |Do not leave important facts only in Sources or Quotes.
            |If a quote names a concrete API, provider, class, module, setting, or file, include that name in the Answer when it is relevant to the question.
            |Keep the Answer concise but complete; usually write 3-6 sentences.
            |Quotes must be copied only from the provided context.
            |Do not change the meaning of quotes.
            |
            |Context:
            |$context
            |
            |Question:
            |$question
            |
            |Return the answer strictly in this structure:
            |
            |Answer:
            |...
            |
            |Sources:
            |[1]
            |source
            |section
            |chunk
            |
            |[2]
            |source
            |section
            |chunk
            |
            |Quotes:
            |[1]
            |"..."
            |
            |[2]
            |"..."
        """.trimMargin()
    }
}
