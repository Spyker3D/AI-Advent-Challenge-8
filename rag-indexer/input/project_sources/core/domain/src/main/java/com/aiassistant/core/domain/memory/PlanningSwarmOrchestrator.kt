package com.aiassistant.core.domain.memory

import android.util.Log
import com.aiassistant.core.domain.repository.LongTermMemoryRepository
import javax.inject.Inject

class PlanningSwarmOrchestrator @Inject constructor(
    private val longTermMemoryRepository: LongTermMemoryRepository,
    agentFactory: PlanningSwarmAgentFactory,
    private val supervisorAgent: PlanningSupervisorAgent
) {
    private val agents = listOf(
        agentFactory.create(PlanningSwarmRole.REQUIREMENTS),
        agentFactory.create(PlanningSwarmRole.ARCHITECTURE),
        agentFactory.create(PlanningSwarmRole.RISKS),
        agentFactory.create(PlanningSwarmRole.TESTING),
        agentFactory.create(PlanningSwarmRole.IMPLEMENTATION)
    )

    suspend fun runPlanning(taskContext: TaskContext): PlanningSwarmOutput {
        Log.d("PLANNING_SWARM", "start task=${taskContext.id}")
        val longTermMemory = longTermMemoryRepository.getLongTermMemory()
        val swarmResults = agents.map { it.run(taskContext, longTermMemory) }
        val finalPlan = supervisorAgent.synthesize(
            taskContext = taskContext,
            longTermMemory = longTermMemory,
            swarmResults = swarmResults
        )
        return PlanningSwarmOutput(swarmResults, finalPlan)
    }
}
