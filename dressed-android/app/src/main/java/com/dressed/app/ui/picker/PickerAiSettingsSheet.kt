package com.dressed.app.ui.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.dressed.app.data.picker.AiPickerPreferencesStore
import com.dressed.app.data.picker.PickerAIProvider
import com.dressed.app.data.picker.PickerAIReasoner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PickerAiSettingsSheet(
    visible: Boolean,
    store: AiPickerPreferencesStore,
    onDismissRequest: () -> Unit,
    onPrefsChanged: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        PickerAiSettingsSheetContent(
            store = store,
            onPrefsChanged = onPrefsChanged,
            modifier = Modifier.padding(bottom = 28.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickerAiSettingsSheetContent(
    store: AiPickerPreferencesStore,
    onPrefsChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var reasoningEnabled by remember { mutableStateOf(store.isReasoningEnabled) }
    var selectedProvider by remember { mutableStateOf(store.selectedProvider) }
    var keyDraft by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var statusLine by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val hasKey = store.hasStoredKey(selectedProvider)
    val navPurple = MaterialTheme.colorScheme.primary

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Psychology, contentDescription = null, tint = navPurple)
            Text("AI outfit reasoning", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            "Optional: add your API key so a model can write short explanations for each suggestion. Outfits are still chosen on-device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
            ),
        ) {
            Text(
                "API calls use your provider account and may incur charges. Turn this off or remove keys anytime.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }

        Text("Provider", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 20.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            for (p in PickerAIProvider.entries) {
                FilterChip(
                    selected = selectedProvider == p,
                    onClick = {
                        selectedProvider = p
                        store.selectedProvider = p
                        keyDraft = ""
                        statusLine = null
                        onPrefsChanged()
                    },
                    label = { Text(p.displayName) },
                )
            }
        }
        Text(
            when (selectedProvider) {
                PickerAIProvider.ANTHROPIC -> "Uses Claude via Anthropic Messages API."
                PickerAIProvider.OPENAI -> "Uses OpenAI Chat Completions with JSON output."
                PickerAIProvider.GROK -> "Grok is not connected yet — suggestions stay rule-based."
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Use AI explanations", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = reasoningEnabled,
                onCheckedChange = {
                    reasoningEnabled = it
                    store.isReasoningEnabled = it
                    onPrefsChanged()
                },
            )
        }

        if (selectedProvider == PickerAIProvider.GROK) {
            Text(
                "Grok / xAI support is planned. Choose Anthropic or OpenAI for cloud explanations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            Text(
                if (hasKey) "Key saved on this device (hidden)." else "No key saved for ${selectedProvider.displayName}.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )

            OutlinedTextField(
                value = keyDraft,
                onValueChange = { keyDraft = it; statusLine = null },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text("${selectedProvider.displayName} key") },
                placeholder = {
                    Text(
                        when (selectedProvider) {
                            PickerAIProvider.ANTHROPIC -> "sk-ant-api03-…"
                            PickerAIProvider.OPENAI -> "sk-…"
                            PickerAIProvider.GROK -> ""
                        },
                    )
                },
                visualTransformation = if (showKey) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
            TextButtonTiny(
                text = if (showKey) "Hide key" else "Show key",
                onClick = { showKey = !showKey },
            )

            Button(
                onClick = {
                    if (keyDraft.isNotBlank()) {
                        store.setStoredApiKey(selectedProvider, keyDraft.trim())
                        keyDraft = ""
                        statusLine = "Key saved"
                        onPrefsChanged()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = keyDraft.isNotBlank(),
            ) {
                Text("Save key")
            }
            OutlinedButton(
                onClick = {
                    testing = true
                    statusLine = null
                    scope.launch {
                        val raw = keyDraft.trim().takeIf { it.isNotEmpty() }
                            ?: store.getStoredApiKey(selectedProvider).orEmpty()
                        val err = PickerAIReasoner.validateApiKey(selectedProvider, raw)
                        testing = false
                        statusLine = err ?: "Connection OK."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = !testing &&
                    (
                        keyDraft.isNotBlank() ||
                            !store.getStoredApiKey(selectedProvider).isNullOrBlank()
                        ),
            ) {
                Text(if (testing) "Testing…" else "Test connection")
            }
            if (hasKey) {
                OutlinedButton(
                    onClick = {
                        store.setStoredApiKey(selectedProvider, null)
                        keyDraft = ""
                        statusLine = "Key removed"
                        onPrefsChanged()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text("Remove key from device")
                }
            }
        }

        statusLine?.let { line ->
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                color = if (line.startsWith("HTTP") || line.contains("rejected", ignoreCase = true)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TextButtonTiny(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(top = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
