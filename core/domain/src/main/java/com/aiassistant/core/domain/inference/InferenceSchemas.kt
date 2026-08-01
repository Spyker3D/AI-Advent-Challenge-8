package com.aiassistant.core.domain.inference

object InferenceSchemas {
    val MONOLITHIC = schema("normalized_summary","category","severity","action","confidence","title","message","user_action", properties = """"normalized_summary":{"type":"string"},"category":{"type":"string","enum":[${enumValues<IncidentCategory>()}]},"severity":{"type":"string","enum":[${enumValues<IncidentSeverity>()}]},"action":{"type":"string","enum":[${enumValues<IncidentAction>()}]},"confidence":{"type":"number","minimum":0,"maximum":1},"title":{"type":"string"},"message":{"type":"string"},"user_action":{"type":"string"}""")
    val NORMALIZATION = schema("network_available","http_status","timeout_observed","empty_response","local_history_problem","multiple_signals","normalized_summary", properties = """"network_available":{"type":["boolean","null"]},"http_status":{"type":["integer","null"]},"timeout_observed":{"type":"boolean"},"empty_response":{"type":"boolean"},"local_history_problem":{"type":"boolean"},"multiple_signals":{"type":"boolean"},"normalized_summary":{"type":"string"}""")
    val DECISION = schema("category","severity","action","confidence", properties = """"category":{"type":"string","enum":[${enumValues<IncidentCategory>()}]},"severity":{"type":"string","enum":[${enumValues<IncidentSeverity>()}]},"action":{"type":"string","enum":[${enumValues<IncidentAction>()}]},"confidence":{"type":"number","minimum":0,"maximum":1}""")
    val PRESENTATION = schema("title","message","user_action", properties = """"title":{"type":"string"},"message":{"type":"string"},"user_action":{"type":"string"}""")

    private fun schema(vararg required: String, properties: String) = """{"type":"object","properties":{$properties},"required":[${required.joinToString { "\"$it\"" }}],"additionalProperties":false}"""
    private inline fun <reified T : Enum<T>> enumValues() = enumValues<T>().joinToString { "\"${it.name}\"" }
}
