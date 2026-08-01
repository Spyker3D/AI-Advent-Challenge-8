package com.aiassistant.core.domain.inference

import com.aiassistant.core.domain.agent.ChatResponse
import com.aiassistant.core.domain.agent.LlmClient
import com.aiassistant.core.domain.agent.LlmRequestOptions
import com.aiassistant.core.domain.entity.Message
import com.aiassistant.core.domain.entity.MessageRole
import com.aiassistant.core.domain.entity.AiProvider
import java.util.UUID
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class InferenceEngine @Inject constructor(private val llmClient: LlmClient) {
    suspend fun execute(input: String, mode: InferenceMode): Result<InferenceExecutionResult> = when (mode) {
        InferenceMode.MONOLITHIC -> monolithic(input)
        InferenceMode.MULTI_STAGE -> multiStage(input)
    }

    private suspend fun monolithic(input: String): Result<InferenceExecutionResult> {
        val start = now()
        val attempt = parsedCall("monolithic", InferenceConfig.MONOLITHIC_MODEL, MultiStagePrompts.MONOLITHIC, input, 300, InferenceSchemas.MONOLITHIC, InferenceParsers::monolithic)
        val parsed = attempt.value ?: return Result.failure(failure(attempt.error, InferenceMode.MONOLITHIC, listOf(attempt.metadata), attempt.calls))
        val (summary, decision, presentation) = parsed
        return Result.success(InferenceExecutionResult(format(presentation), InferenceDebugMetadata(InferenceMode.MONOLITHIC, summary, decision, listOf(attempt.metadata), now()-start, attempt.calls, true)))
    }

    private suspend fun multiStage(input: String): Result<InferenceExecutionResult> {
        val start = now(); val stages = mutableListOf<InferenceStageMetadata>()
        var calls = 0
        val s1 = parsedCall("normalization", InferenceConfig.NORMALIZATION_MODEL, MultiStagePrompts.NORMALIZATION, input, 150, InferenceSchemas.NORMALIZATION, InferenceParsers::normalization)
        calls += s1.calls; stages += s1.metadata
        val normalized = s1.value ?: return failedWithSkipped(InferenceMode.MULTI_STAGE,start,stages,s1.error,null,calls)
        val decisionInput = InferenceParsers.normalizedJson(normalized)
        val s2 = parsedCall("decision", InferenceConfig.DECISION_MODEL, MultiStagePrompts.DECISION, decisionInput, 100, InferenceSchemas.DECISION, InferenceParsers::decision)
        calls += s2.calls; stages += s2.metadata
        val decision = s2.value ?: return failedWithSkipped(InferenceMode.MULTI_STAGE,start,stages,s2.error,normalized,calls)
        val presentationInput = "{\"normalized_summary\":${com.google.gson.Gson().toJson(normalized.normalizedSummary)},\"decision\":${InferenceParsers.decisionJson(decision)}}"
        val s3 = parsedCall("presentation", InferenceConfig.PRESENTATION_MODEL, MultiStagePrompts.PRESENTATION, presentationInput, 120, InferenceSchemas.PRESENTATION) { raw ->
            InferenceParsers.presentation(raw, decision.action)
        }
        calls += s3.calls; stages += s3.metadata
        val presentation = s3.value ?: fallback(decision)
        val debug = InferenceDebugMetadata(InferenceMode.MULTI_STAGE, normalized.normalizedSummary, decision, stages, now()-start, calls, stages.all { it.status == StageStatus.OK })
        return Result.success(InferenceExecutionResult(format(presentation), debug))
    }

    private suspend fun call(stage:String, model:String, system:String, input:String, tokens:Int, schema:String): Call {
        val start=now()
        val result = try {
            llmClient.sendChat(listOf(Message(UUID.randomUUID().toString(),system,MessageRole.SYSTEM),Message(UUID.randomUUID().toString(),input,MessageRole.USER)),tokens,model,LlmRequestOptions(temperature=0.0,numPredict=tokens,stream=false,jsonSchema=schema,requiredProvider=AiProvider.LOCAL_OLLAMA))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        val exception = result.exceptionOrNull()
        if (exception is CancellationException) throw exception
        val response=result.getOrNull()
        val error=exception?.message ?: exception?.javaClass?.simpleName ?: "Model error"
        return Call(response, InferenceStageMetadata(stage,model,now()-start,response?.metadata?.promptTokens,response?.metadata?.completionTokens,if(response==null) StageStatus.MODEL_ERROR else StageStatus.OK,if(response==null) error else null), error)
    }
    private suspend fun <T> parsedCall(stage:String,model:String,system:String,input:String,tokens:Int,schema:String,parser:(String)->ParseResult<T>):ParsedCall<T> {
        val first = call(stage,model,system,input,tokens,schema)
        if (first.response == null) return ParsedCall(null,first.metadata,1,first.error)
        return when (val parsed = parser(first.response.message)) {
            is ParseResult.Success -> ParsedCall(parsed.value,first.metadata,1,"")
            is ParseResult.Failure -> {
                if (parsed.status != StageStatus.VALIDATION_ERROR) {
                    ParsedCall(null,first.metadata.copy(status=parsed.status,error=parsed.error),1,parsed.error)
                } else {
                    val retry = call(stage,model,"$system\n\n${MultiStagePrompts.VALIDATION_CORRECTION}",input,tokens,schema)
                    val combined = combine(first.metadata,retry.metadata)
                    if (retry.response == null) ParsedCall(null,combined,2,retry.error)
                    else when (val corrected = parser(retry.response.message)) {
                        is ParseResult.Success -> ParsedCall(corrected.value,combined.copy(status=StageStatus.OK,error=null),2,"")
                        is ParseResult.Failure -> ParsedCall(null,combined.copy(status=corrected.status,error=corrected.error),2,corrected.error)
                    }
                }
            }
        }
    }
    private fun combine(first:InferenceStageMetadata,second:InferenceStageMetadata)=second.copy(
        latencyMs=first.latencyMs+second.latencyMs,
        promptTokens=listOfNotNull(first.promptTokens,second.promptTokens).takeIf { it.isNotEmpty() }?.sum(),
        completionTokens=listOfNotNull(first.completionTokens,second.completionTokens).takeIf { it.isNotEmpty() }?.sum()
    )
    private fun failedWithSkipped(mode:InferenceMode,start:Long,stages:MutableList<InferenceStageMetadata>,error:String,normalized:NormalizedIncident?,calls:Int):Result<InferenceExecutionResult> {
        val definitions=listOf("normalization" to InferenceConfig.NORMALIZATION_MODEL,"decision" to InferenceConfig.DECISION_MODEL,"presentation" to InferenceConfig.PRESENTATION_MODEL)
        definitions.drop(stages.size).forEach { stages += InferenceStageMetadata(it.first,it.second,0,null,null,StageStatus.SKIPPED,"Previous stage failed") }
        return Result.failure(InferencePipelineException(error,InferenceDebugMetadata(mode,normalized?.normalizedSummary,null,stages,now()-start,calls,false),normalized))
    }
    private fun failure(error:String,mode:InferenceMode,stages:List<InferenceStageMetadata>,calls:Int)=InferencePipelineException(error,InferenceDebugMetadata(mode,null,null,stages,stages.sumOf { it.latencyMs },calls,false))
    private fun fallback(d:IncidentDecision)=when(d.action){
        IncidentAction.CHECK_NETWORK->UserFacingIncidentResult("Проверьте подключение","Не удалось выполнить запрос из-за сети.","Проверьте интернет и повторите попытку.")
        IncidentAction.RETRY_WITH_BACKOFF->UserFacingIncidentResult("Слишком много запросов","Сервис временно ограничил запросы.","Подождите немного и повторите попытку.")
        IncidentAction.RETRY_REQUEST->UserFacingIncidentResult("Время ожидания истекло","Ответ не был получен вовремя.","Повторите запрос.")
        IncidentAction.SHOW_EMPTY_RESPONSE_ERROR->UserFacingIncidentResult("Пустой ответ","Модель вернула пустой ответ.","Повторите запрос.")
        IncidentAction.RELOAD_LOCAL_HISTORY->UserFacingIncidentResult("История недоступна","Локальная история чатов не загрузилась.","Перезагрузите историю.")
        IncidentAction.REQUEST_MORE_INFORMATION->UserFacingIncidentResult("Нужно уточнение","Недостаточно данных для точного определения проблемы.","Уточните наблюдаемые симптомы.")
    }.copy(userAction=d.action.userFacingText())
    private fun format(p:UserFacingIncidentResult)="${p.title}\n\n${p.message}\n\n${p.userAction}"
    private fun now()=System.currentTimeMillis()
    private data class Call(val response:ChatResponse?,val metadata:InferenceStageMetadata,val error:String)
    private data class ParsedCall<T>(val value:T?,val metadata:InferenceStageMetadata,val calls:Int,val error:String)
}
