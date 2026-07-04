package com.aiassistant.core.domain.rag

fun RagChunk.toPromptQuote(): String {
    val compact = text
        .replace(Regex("\\s+"), " ")
        .trim()
    if (compact.length <= MAX_PROMPT_QUOTE_CHARS) return compact

    val sentenceLimited = SENTENCE_REGEX
        .findAll(compact)
        .take(MAX_PROMPT_QUOTE_SENTENCES)
        .joinToString(separator = " ") { it.value.trim() }

    val limited = sentenceLimited.ifBlank { compact.take(MAX_PROMPT_QUOTE_CHARS) }
    return if (limited.length <= MAX_PROMPT_QUOTE_CHARS) {
        limited
    } else {
        limited.take(MAX_PROMPT_QUOTE_CHARS).trimEnd()
    }
}

private const val MAX_PROMPT_QUOTE_CHARS = 800
private const val MAX_PROMPT_QUOTE_SENTENCES = 10
private val SENTENCE_REGEX = Regex("[^.!?。！？]+[.!?。！？]?")
