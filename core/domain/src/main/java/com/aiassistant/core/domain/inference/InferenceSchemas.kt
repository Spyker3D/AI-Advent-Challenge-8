package com.aiassistant.core.domain.inference

object InferenceSchemas {
    val MONOLITHIC = schema("normalized_summary","category","severity","action","confidence","evidence_state","supporting_evidence","contradicting_evidence","title","message","user_action", properties = """"normalized_summary":{"type":"string"},${decisionProperties()},"title":{"type":"string"},"message":{"type":"string"},${userActionProperty()}""")
    val NORMALIZATION = schema("observed_facts","normalized_summary", properties = """"observed_facts":{"type":"array","items":{"type":"string"}},"normalized_summary":{"type":"string"}""")
    val DECISION = schema("category","severity","action","confidence","evidence_state","supporting_evidence","contradicting_evidence", properties = decisionProperties())
    val PRESENTATION = schema("title","message","user_action", properties = """"title":{"type":"string"},"message":{"type":"string"},${userActionProperty()}""")

    private fun schema(vararg required: String, properties: String) = """{"type":"object","properties":{$properties},"required":[${required.joinToString { "${34.toChar()}$it${34.toChar()}" }}],"additionalProperties":false}"""
    private fun decisionProperties() = """"category":{"type":"string","enum":[${enumValues<IncidentCategory>()}]},"severity":{"type":"string","enum":[${enumValues<IncidentSeverity>()}]},"action":{"type":"string","enum":[${enumValues<IncidentAction>()}]},"confidence":{"type":"number","minimum":0,"maximum":1},"evidence_state":{"type":"string","enum":[${enumValues<EvidenceState>()}]},"supporting_evidence":{"type":"array","items":{"type":"string"}},"contradicting_evidence":{"type":"array","items":{"type":"string"}}"""
    private fun userActionProperty() = """"user_action":{"type":"string","enum":[${IncidentAction.entries.joinToString { "${34.toChar()}${it.userFacingText()}${34.toChar()}" }}]}"""
    private inline fun <reified T : Enum<T>> enumValues(): String =
        kotlin.enumValues<T>().joinToString { "${34.toChar()}${it.name}${34.toChar()}" }
}
