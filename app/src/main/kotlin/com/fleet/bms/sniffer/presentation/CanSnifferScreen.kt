package com.fleet.bms.sniffer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleet.bms.sniffer.domain.model.CanIdEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanSnifferScreen(
    viewModel: CanSnifferViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CAN Sniffer - Rawsuns VCU") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("Dismiss")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            uiState.exportMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        TextButton(onClick = { viewModel.dismissExportMessage() }) {
                            Text("OK")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Frames/sec", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "%.1f".format(uiState.framesPerSecond),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Unique IDs", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${uiState.uniqueIdCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Session control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.sessionLabel,
                    onValueChange = { viewModel.setSessionLabel(it) },
                    label = { Text("Session") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.startSession() },
                    enabled = !uiState.isSniffing
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.stopSession() },
                    enabled = uiState.isSniffing,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { viewModel.exportSession() },
                    enabled = uiState.entries.isNotEmpty() && !uiState.isExporting
                ) {
                    Text(if (uiState.isExporting) "Exporting..." else "Export")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { viewModel.clearData() }) {
                    Text("Clear")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ID list
            Text(
                "CAN IDs (sorted by change frequency)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.entries) { entry ->
                    CanIdListItem(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun CanIdListItem(entry: CanIdEntry) {
    val changeRate = if (entry.frameCount > 0) {
        entry.valueChangeCount.toFloat() / entry.frameCount
    } else 0f

    val backgroundColor = when {
        changeRate > 0.3f -> Color(0xFF4CAF50).copy(alpha = 0.2f)  // Green - high change
        changeRate > 0.01f -> Color(0xFFFFEB3B).copy(alpha = 0.2f)  // Yellow - low change
        else -> Color(0xFFF44336).copy(alpha = 0.2f)                 // Red - static
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = entry.idHex,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Count: ${entry.frameCount} | Changes: ${entry.valueChangeCount}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = entry.lastValueHex(),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
