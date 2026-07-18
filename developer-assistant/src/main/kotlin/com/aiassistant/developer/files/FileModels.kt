package com.aiassistant.developer.files

data class SearchMatch(val path: String, val line: Int, val text: String)
data class ProposedChange(val path: String, val original: String?, val content: String)
data class ToolResult<T>(val value: T? = null, val error: String? = null)

class PatchConflict(message: String) : IllegalArgumentException(message)
