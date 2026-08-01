package com.aiassistant.core.domain.inference

object MultiStagePrompts {
    const val MONOLITHIC = "Classify the incident and write a concise Russian user response. Return exactly one JSON object matching the schema, with no markdown or surrounding text."
    const val NORMALIZATION = "Extract only observed incident signals and a non-empty normalized summary. Do not classify or write a response. Return exactly one JSON object matching the schema, with no markdown or surrounding text."
    const val DECISION = "Select only the exact category, severity, action and confidence from normalized data. AMBIGUOUS must use REQUEST_MORE_INFORMATION and confidence at most 0.8. Return exactly one JSON object matching the schema, with no prose, markdown or surrounding text."
    const val PRESENTATION = "Write concise Russian title, message and user_action from the supplied summary and decision. Do not change or mention the decision or pipeline stages. Return exactly one JSON object matching the schema, with no markdown or surrounding text."
}
