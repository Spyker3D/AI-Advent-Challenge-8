package com.aiassistant.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
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
    Column(
        modifier = modifier.fillMaxWidth().semantics { selectableGroup() },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InferenceModeButton(
                label = "Ordinary",
                selected = !microFirstEnabled && selectedMode == null,
                onClick = { onModeSelected(null) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            InferenceModeButton(
                label = "Monolithic",
                selected = !microFirstEnabled && selectedMode == InferenceMode.MONOLITHIC,
                onClick = { onModeSelected(InferenceMode.MONOLITHIC) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InferenceModeButton(
                label = "Multi-stage",
                selected = !microFirstEnabled && selectedMode == InferenceMode.MULTI_STAGE,
                onClick = { onModeSelected(InferenceMode.MULTI_STAGE) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            InferenceModeButton(
                label = "Micro first",
                selected = microFirstEnabled,
                onClick = onMicroFirstSelected,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InferenceModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.semantics {
            this.selected = selected
            role = Role.RadioButton
        }
    ) { Text(label) }
}
