package com.aiassistant.feature.chat.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiassistant.core.domain.microfirst.MicroFirstConfig
import com.aiassistant.core.domain.microfirst.MicroFirstResult

@Composable
internal fun MicroFirstDebugBlock(result: MicroFirstResult, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(10.dp)) {
            Text("Micro-first Debug", style = MaterialTheme.typography.labelLarge)
            Text("Mode: Micro first")
            Text("Micro model: ${MicroFirstConfig.MICRO_MODEL}")
            Text("Status: ${result.microResult?.status ?: "unavailable"}")
            Text("Label: ${result.microResult?.label ?: "none"}")
            Text("Score: ${result.microResult?.score ?: "n/a"}")
            Text("Margin: ${result.microResult?.margin ?: "n/a"}")
            Text("Handled by micro: ${result.handledByMicro}")
            Text("Fallback used: ${result.fallbackUsed}")
            Text("Fallback reason: ${result.fallbackReason ?: "none"}")
            Text("Fallback model: ${result.fallbackModel ?: "none"}")
            Text("Large model calls: ${result.largeLlmCalls}")
            Text("Micro latency: ${result.microLatencyMs} ms")
            Text("Fallback latency: ${result.fallbackLatencyMs?.let { "$it ms" } ?: "n/a"}")
            Text("Total latency: ${result.totalLatencyMs} ms")
        }
    }
}
