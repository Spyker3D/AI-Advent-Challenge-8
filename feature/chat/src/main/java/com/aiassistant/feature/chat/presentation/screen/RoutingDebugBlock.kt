package com.aiassistant.feature.chat.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiassistant.core.domain.routing.RoutingDebugMetadata

@Composable
internal fun RoutingDebugBlock(metadata: RoutingDebugMetadata, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(10.dp)) {
            Text("Routing Debug", style = MaterialTheme.typography.labelLarge)
            Text("Routing enabled: ${metadata.routingEnabled}")
            Text("Context strategy: ${metadata.contextStrategy ?: "not available"}")
            Text("Structured format: ${metadata.structuredFormatEnabled}")
            Text("Parse failure: ${metadata.parseFailure ?: "NONE"}")
            Text("First model: ${metadata.firstModel ?: "not called"}")
            Text("Final model: ${metadata.finalModel}")
            Text("Escalated: ${metadata.escalated}")
            Text("Confidence: ${metadata.confidence ?: "not evaluated"}")
            Text("Reason: ${metadata.reason ?: "NONE"}")
            Text("Small latency: ${metadata.smallLatencyMs?.let { "$it ms" } ?: "not called"}")
            Text("Large latency: ${metadata.largeLatencyMs?.let { "$it ms" } ?: "not called"}")
            Text("Total latency: ${metadata.totalLatencyMs?.let { "$it ms" } ?: "not available"}")
        }
    }
}
