package com.securenet.auditor.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WolScreen(
    viewModel: WolViewModel,
    onBack: () -> Unit
) {
    var macInput by remember { mutableStateOf("") }
    var ipInput by remember { mutableStateOf("255.255.255.255") }
    val status by viewModel.status.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wake-on-LAN", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SuccessGreen
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Send a Magic Packet to wake up a device on your local network.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = macInput,
                onValueChange = { macInput = it.uppercase() },
                label = { Text("MAC Address (e.g. AA:BB:CC:DD:EE:FF)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("00:11:22:33:44:55") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = ipInput,
                onValueChange = { ipInput = it },
                label = { Text("Broadcast IP") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.sendMagicPacket(macInput, ipInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = macInput.isNotBlank()
            ) {
                Text("Wake Up Device")
            }

            Spacer(modifier = Modifier.height(32.dp))

            status?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (it.startsWith("Error")) MaterialTheme.colorScheme.errorContainer else SuccessGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = MonoType,
                            color = if (it.startsWith("Error")) MaterialTheme.colorScheme.onErrorContainer else SuccessGreen
                        )
                    }
                }
            }
        }
    }
}
