package com.securenet.auditor.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.ui.components.copyToClipboard
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubnetCalculatorScreen(
    viewModel: SubnetCalculatorViewModel,
    onBack: () -> Unit
) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    var ipInput by remember { mutableStateOf("192.168.1.1") }
    var maskInput by remember { mutableStateOf("24") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subnet Calculator", fontWeight = FontWeight.Bold) },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        label = { Text("IP Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = maskInput,
                        onValueChange = { maskInput = it },
                        label = { Text("Subnet Mask or CIDR (e.g. 24 or 255.255.255.0)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.calculate(ipInput, maskInput) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calculate")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            result?.let { res ->
                Text("Calculation Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                ResultItem("Network Address", res.networkAddress)
                ResultItem("Broadcast Address", res.broadcastAddress)
                ResultItem("Usable Host Range", res.hostRange)
                ResultItem("Subnet Mask", "${res.mask} (/${res.cidr})")
                ResultItem("Wildcard Mask", res.wildcardMask)
                ResultItem("Total Hosts", res.totalHosts.toString())
                ResultItem("Usable Hosts", res.usableHosts.toString())
                ResultItem("Binary Mask", res.binaryMask, isMonospace = true)
            } ?: run {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Enter valid IP and Mask to see results", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ResultItem(label: String, value: String, isMonospace: Boolean = false) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (isMonospace) MonoType else null,
                    color = if (isMonospace) TealPrimary else Color.Unspecified
                )
            }
            IconButton(onClick = { copyToClipboard(context, label, value) }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp), tint = TealPrimary)
            }
        }
    }
}
