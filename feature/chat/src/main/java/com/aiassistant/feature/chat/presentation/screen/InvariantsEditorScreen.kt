package com.aiassistant.feature.chat.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aiassistant.feature.chat.presentation.viewmodel.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvariantsEditorScreen(
    viewModel: MemoryViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMemory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invariants") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Stack invariant")
            OutlinedTextField(
                value = state.allowedStack,
                onValueChange = {
                    viewModel.updateInvariantForm(
                        it, state.bannedStack, state.architecture,
                        state.bannedArchitectures, state.budget, state.maxDependencies
                    )
                },
                label = { Text("Allowed stack (one per line)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.bannedStack,
                onValueChange = {
                    viewModel.updateInvariantForm(
                        state.allowedStack, it, state.architecture,
                        state.bannedArchitectures, state.budget, state.maxDependencies
                    )
                },
                label = { Text("Banned stack (one per line)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.architecture,
                onValueChange = {
                    viewModel.updateInvariantForm(
                        state.allowedStack, state.bannedStack, it,
                        state.bannedArchitectures, state.budget, state.maxDependencies
                    )
                },
                label = { Text("Architecture required") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.bannedArchitectures,
                onValueChange = {
                    viewModel.updateInvariantForm(
                        state.allowedStack, state.bannedStack, state.architecture,
                        it, state.budget, state.maxDependencies
                    )
                },
                label = { Text("Architecture banned (one per line)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.budget,
                onValueChange = {
                    viewModel.updateInvariantForm(
                        state.allowedStack, state.bannedStack, state.architecture,
                        state.bannedArchitectures, it, state.maxDependencies
                    )
                },
                label = { Text("Budget") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.maxDependencies,
                onValueChange = {
                    viewModel.updateInvariantForm(
                        state.allowedStack, state.bannedStack, state.architecture,
                        state.bannedArchitectures, state.budget, it.filter(Char::isDigit)
                    )
                },
                label = { Text("Max dependencies") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            state.error?.let { Text(it) }
            state.message?.let { Text(it) }
            Button(
                onClick = viewModel::saveInvariants,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
