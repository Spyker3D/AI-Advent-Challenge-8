package com.aiassistant.core.domain.memory

import com.google.gson.annotations.SerializedName

data class TaskContextUpdate(
    @SerializedName("goals_add")
    val goalsAdd: List<String> = emptyList(),
    @SerializedName("constraints_add")
    val constraintsAdd: List<String> = emptyList(),
    @SerializedName("decisions_add")
    val decisionsAdd: List<String> = emptyList(),
    @SerializedName("clarifications_add")
    val clarificationsAdd: List<String> = emptyList(),
    @SerializedName("terms_add")
    val termsAdd: List<String> = emptyList(),
    @SerializedName("current_state")
    val currentState: String? = null
)
