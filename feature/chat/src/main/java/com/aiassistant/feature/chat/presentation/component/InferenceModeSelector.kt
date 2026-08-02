package com.aiassistant.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selectableGroup
import com.aiassistant.core.domain.inference.InferenceMode

@Composable
internal fun InferenceModeSelector(
    selectedMode: InferenceMode?,
    microFirstEnabled: Boolean,
    onModeSelected: (InferenceMode?) -> Unit,
    onMicroFirstSelected: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().semantics { selectableGroup() },
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        inferenceModeOptions.forEach { option ->
            val isSelected = !microFirstEnabled && selectedMode == option.mode
            OutlinedButton(
                onClick = { onModeSelected(option.mode) },
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.semantics {
                    selected = isSelected
                    role = Role.RadioButton
                }
            ) { Text(option.label) }
        }
        OutlinedButton(
            onClick = onMicroFirstSelected,
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (microFirstEnabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.semantics { selected = microFirstEnabled; role = Role.RadioButton }
        ) { Text("Micro first") }
    }
}

private data class InferenceModeOption(val label: String, val mode: InferenceMode?)

private val inferenceModeOptions = listOf(
    InferenceModeOption("Ordinary", null),
    InferenceModeOption("Monolithic", InferenceMode.MONOLITHIC),
    InferenceModeOption("Multi-stage", InferenceMode.MULTI_STAGE)
)
