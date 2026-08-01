package com.aiassistant.core.domain.inference

import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.agent.LlmRequestOptions
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import java.util.UUID

class InferenceEngine(private val llmClient: LlmClient) {
    suspend fun execute(input: String, mode: InferenceMode): Result<InferenceExecutionResult> = when (mode) {
        InferenceMode.MONOLITHIC -> monolithic(input)
        InferenceMode.MULTI_STAGE -> multiStage(input)
    }

    private suspend fun monolithic(input: String): Result<InferenceExecutionResult> {
        val start = now()
        val call = call("monolithic", InferenceConfig.MONOLITHIC_MODEL, MultiStagePrompts.MONOLITHIC, input, 300, InferenceSchemas.MONOLITHIC)
        val metadata = call.metadata
        val response = call.response ?: return Result.failure(failure(call.error ?: "Model error", InferenceMode.MONOLITHIC, listOf(metadata)))
        return when (val parsed = InferenceParsers.monolithic(response.message)) {
            is ParseResult.Failure -> Result.failure(failure(parsed.error, InferenceMode.MONOLITHIC, listOf(metadata.copy(status = parsed.status, error = parsed.error))))
            is ParseResult.Success -> {
                val (summary, decision, presentation) = parsed.value
                Result.success(InferenceExecutionResult(format(presentation), InferenceDebugMetadata(InferenceMode.MONOLITHIC, summary, decision, listOf(metadata), now()-start, 1, true)))
            }
        }
    }

    private suspend fun multiStage(input: String): Result<InferenceExecutionResult> {
        val start = now(); val stages = mutableListOf<InferenceStageMetadata>()
        val s1 = call("normalization", InferenceConfig.NORMALIZATION_MODEL, MultiStagePrompts.NORMALIZATION, input, 150, InferenceSchemas.NORMALIZATION)
        stages += s1.metadata
        if (s1.response == null) return failedWithSkipped(InferenceMode.MULTI_STAGE, start, stages, s1.error!!, null, 1)
        val normalized = when (val parsed = InferenceParsers.normalization(s1.response.message)) {
            is ParseResult.Success -> parsed.value
            is ParseResult.Failure -> { stages[0] = stages[0].copy(status=parsed.status,error=parsed.error); return failedWithSkipped(InferenceMode.MULTI_STAGE,start,stages,parsed.error,null,1) }
        }
        val s2 = call("decision", InferenceConfig.DECISION_MODEL, MultiStagePrompts.DECISION, InferenceParsers.normalizedJson(normalized), 100, InferenceSchemas.DECISION)
        stages += s2.metadata
        if (s2.response == null) return failedWithSkipped(InferenceMode.MULTI_STAGE,start,stages,s2.error!!,normalized,2)
        val decision = when (val parsed = InferenceParsers.decision(s2.response.message)) {
            is ParseResult.Success -> parsed.value
            is ParseResult.Failure -> { stages[1] = stages[1].copy(status=parsed.status,error=parsed.error); return failedWithSkipped(InferenceMode.MULTI_STAGE,start,stages,parsed.error,normalized,2) }
        }
        val presentationInput = InferenceParsers.normalizedJson(normalized).let { "{\"normalized_summary\":${com.google.gson.Gson().toJson(normalized.normalizedSummary)},\"decision\":${InferenceParsers.decisionJson(decision)}}" }
        val s3 = call("presentation", InferenceConfig.PRESENTATION_MODEL, MultiStagePrompts.PRESENTATION, presentationInput, 120, InferenceSchemas.PRESENTATION)
        stages += s3.metadata
        val presentation = if (s3.response == null) fallback(decision) else when (val parsed = InferenceParsers.presentation(s3.response.message)) {
            is ParseResult.Success -> parsed.value
            is ParseResult.Failure -> { stages[2] = stages[2].copy(status=parsed.status,error=parsed.error); fallback(decision) }
        }
        val debug = InferenceDebugMetadata(InferenceMode.MULTI_STAGE, normalized.normalizedSummary, decision, stages, now()-start, 3, stages.all { it.status == StageStatus.OK })
        return Result.success(InferenceExecutionResult(format(presentation), debug))
    }

    private suspend fun call(stage:String, model:String, system:String, input:String, tokens:Int, schema:String): Call {
        val start=now()
        val result = try { llmClient.sendChat(listOf(Message(UUID.randomUUID().toString(),system,MessageRole.SYSTEM),Message(UUID.randomUUID().toString(),input,MessageRole.USER)),tokens,model,LlmRequestOptions(temperature=0.0,numPredict=tokens,stream=false,jsonSchema=schema)) } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        val response=result.getOrNull(); val error=result.exceptionOrNull()?.message
        return Call(response, InferenceStageMetadata(stage,model,now()-start,response?.metadata?.promptTokens,response?.metadata?.completionTokens,if(response==null) StageStatus.MODEL_ERROR else StageStatus.OK,error), error)
    }
    private fun failedWithSkipped(mode:InferenceMode,start:Long,stages:MutableList<InferenceStageMetadata>,error:String,normalized:NormalizedIncident?,calls:Int):Result<InferenceExecutionResult> {
        val definitions=listOf("normalization" to InferenceConfig.NORMALIZATION_MODEL,"decision" to InferenceConfig.DECISION_MODEL,"presentation" to InferenceConfig.PRESENTATION_MODEL)
        definitions.drop(stages.size).forEach { stages += InferenceStageMetadata(it.first,it.second,0,null,null,StageStatus.SKIPPED,"Previous stage failed") }
        return Result.failure(InferencePipelineException(error,InferenceDebugMetadata(mode,normalized?.normalizedSummary,null,stages,now()-start,calls,false),normalized))
    }
    private fun failure(error:String,mode:InferenceMode,stages:List<InferenceStageMetadata>)=InferencePipelineException(error,InferenceDebugMetadata(mode,null,null,stages,stages.sumOf { it.latencyMs },1,false))
    private fun fallback(d:IncidentDecision)=when(d.action){
        IncidentAction.CHECK_NETWORK->UserFacingIncidentResult("Проверьте подключение","Не удалось выполнить запрос из-за сети.","Проверьте интернет и повторите попытку.")
        IncidentAction.RETRY_WITH_BACKOFF->UserFacingIncidentResult("Слишком много запросов","Сервис временно ограничил запросы.","Подождите немного и повторите попытку.")
        IncidentAction.RETRY_REQUEST->UserFacingIncidentResult("Время ожидания истекло","Ответ не был получен вовремя.","Повторите запрос.")
        IncidentAction.SHOW_EMPTY_RESPONSE_ERROR->UserFacingIncidentResult("Пустой ответ","Модель вернула пустой ответ.","Повторите запрос.")
        IncidentAction.RELOAD_LOCAL_HISTORY->UserFacingIncidentResult("История недоступна","Локальная история чатов не загрузилась.","Перезагрузите историю.")
        IncidentAction.REQUEST_MORE_INFORMATION->UserFacingIncidentResult("Нужно уточнение","Недостаточно данных для точного определения проблемы.","Уточните наблюдаемые симптомы.")
    }
    private fun format(p:UserFacingIncidentResult)="${p.title}\n\n${p.message}\n\n${p.userAction}"
    private fun now()=System.currentTimeMillis()
    private data class Call(val response:ChatResponse?,val metadata:InferenceStageMetadata,val error:String?)
}
