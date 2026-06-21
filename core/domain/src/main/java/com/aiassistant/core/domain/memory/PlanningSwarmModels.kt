package com.aiassistant.core.domain.memory

enum class PlanningSwarmRole {
    REQUIREMENTS,
    ARCHITECTURE,
    RISKS,
    TESTING,
    IMPLEMENTATION,
    SUPERVISOR
}

data class PlanningSwarmResult(
    val role: PlanningSwarmRole,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlanningSwarmOutput(
    val swarmResults: List<PlanningSwarmResult>,
    val finalPlanningResult: String
)
