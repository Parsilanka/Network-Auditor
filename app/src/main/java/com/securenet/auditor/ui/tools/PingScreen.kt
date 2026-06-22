package com.securenet.auditor.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScreen(viewModel: PingViewModel, onBack: () -> Unit) {
    var host by remember { mutableStateOf("") }
    val results by viewModel.pingResults.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ping Tool", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter IP or domain") },
                placeholder = { Text("e.g. 8.8.8.8 or google.com") },
                singleLine = true,
                enabled = !isRunning
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickPingButton("Router", "192.168.1.1", isRunning) { host = it }
                QuickPingButton("Google", "8.8.8.8", isRunning) { host = it }
                QuickPingButton("Cloudflare", "1.1.1.1", isRunning) { host = it }
            }

            Button(
                onClick = { viewModel.startPing(host) },
                modifier = Modifier.fillMaxWidth(),
                enabled = host.isNotBlank() && !isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("PING")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(results) { result ->
                    Text(
                        text = if (result.isSuccess) {
                            "64 bytes from ${result.host}: seq=${result.sequenceNumber} time=${result.responseTimeMs}ms"
                        } else {
                            "Request timeout for seq ${result.sequenceNumber}"
                        },
                        fontFamily = MonoType,
                        fontSize = 12.sp,
                        color = if (result.isSuccess) TealPrimary else Color.Red,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            if (results.isNotEmpty() && !isRunning) {
                PingSummaryCard(results)
            }
        }
    }
}

@Composable
fun QuickPingButton(label: String, value: String, isRunning: Boolean, onClick: (String) -> Unit) {
    AssistChip(
        onClick = { onClick(value) },
        label = { Text(label) },
        enabled = !isRunning
    )
}

@Composable
fun PingSummaryCard(results: List<PingResult>) {
    val successful = results.filter { it.isSuccess }
    val min = successful.minOfOrNull { it.responseTimeMs ?: Long.MAX_VALUE }
    val max = successful.maxOfOrNull { it.responseTimeMs ?: 0 }
    val avg = if (successful.isNotEmpty()) successful.map { it.responseTimeMs ?: 0 }.average().toInt() else 0
    val packetLoss = ((results.size - successful.size).toFloat() / results.size * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat("Min", "${min ?: 0}ms")
                SummaryStat("Avg", "${avg}ms")
                SummaryStat("Max", "${max ?: 0}ms")
                SummaryStat("Loss", "$packetLoss%")
            }
        }
    }
}

@Composable
fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TealPrimary)
    }
}
