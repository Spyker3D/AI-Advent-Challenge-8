package com.aiassistant.feature.chat.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiassistant.core.domain.inference.InferenceDebugMetadata
import com.aiassistant.core.domain.inference.InferenceMode

@Composable
internal fun InferenceDebugBlock(mode: InferenceMode?, metadata: InferenceDebugMetadata?, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(10.dp)) {
            Text("Inference Debug", style = MaterialTheme.typography.labelLarge)
            Text("Inference mode: " + mode.displayName())
            metadata?.let { debug ->
                debug.stageMetadata.firstOrNull()?.let { stage -> Text("Model: " + stage.model) }
                Text("Calls: " + debug.totalModelCalls)
                Text("Total latency: " + debug.totalLatencyMs + " ms")
                Text("Format compliant: " + debug.formatCompliant)
                debug.normalizedSummary?.let { summary -> Text("Normalized: " + summary) }
                debug.decision?.let { decision -> Text("Decision: " + decision.category + " / " + decision.action) }
                debug.stageMetadata.forEachIndexed { index, stage ->
                    Text("Stage " + (index + 1) + ": " + stage.stage)
                    Text("Model: " + stage.model)
                    Text("Status: " + stage.status)
                    Text("Latency: " + stage.latencyMs + " ms")
                    stage.error?.let { error -> Text("Error: " + error) }
                }
            }
        }
    }
}

private fun InferenceMode?.displayName(): String = when (this) {
    null -> "Ordinary"
    InferenceMode.MONOLITHIC -> "Monolithic"
    InferenceMode.MULTI_STAGE -> "Multi-stage"
}
