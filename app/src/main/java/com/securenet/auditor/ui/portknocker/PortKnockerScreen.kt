package com.securenet.auditor.ui.portknocker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.network.PortKnocker
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortKnockerScreen(
    viewModel: PortKnockerViewModel,
    onBack: () -> Unit
) {
    var host by remember { mutableStateOf("") }
    val sequence by viewModel.sequence.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecuting.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Port Knocker", fontWeight = FontWeight.Bold)
                        Text("Stealth firewall trigger", style = MaterialTheme.typography.labelSmall)
                    }
                },
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
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Target Host
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Target Host / IP") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    focusedLabelColor = TealPrimary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Knock Sequence", fontWeight = FontWeight.Bold, color = TealPrimary)
                TextButton(onClick = { viewModel.addStep() }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Step")
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(sequence) { index, step ->
                    KnockStepRow(
                        step = step,
                        onUpdate = { viewModel.updateStep(index, it) },
                        onRemove = { viewModel.removeStep(index) },
                        isLast = index == sequence.size - 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (lastResult != null) {
                Surface(
                    color = if (lastResult!!.success) Color(0xFF1B3921) else Color(0xFF3D0C0C),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (lastResult!!.success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (lastResult!!.success) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(lastResult!!.message, color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Button(
                onClick = { viewModel.executeKnock(host) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = host.isNotBlank() && !isExecuting,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                } else {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Execute Knock Sequence", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun KnockStepRow(
    step: PortKnocker.KnockStep,
    onUpdate: (PortKnocker.KnockStep) -> Unit,
    onRemove: () -> Unit,
    isLast: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Port
            OutlinedTextField(
                value = if (step.port == 0) "" else step.port.toString(),
                onValueChange = { 
                    val newPort = it.toIntOrNull() ?: 0
                    onUpdate(step.copy(port = newPort))
                },
                label = { Text("Port") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // Protocol
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(step.protocol.name)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    PortKnocker.Protocol.values().forEach { proto ->
                        DropdownMenuItem(
                            text = { Text(proto.name) },
                            onClick = { 
                                onUpdate(step.copy(protocol = proto))
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Delay (except for last)
            if (!isLast) {
                OutlinedTextField(
                    value = step.delayMs.toString(),
                    onValueChange = { 
                        val newDelay = it.toLongOrNull() ?: 0L
                        onUpdate(step.copy(delayMs = newDelay))
                    },
                    label = { Text("Delay ms") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = Color.Red)
            }
        }
    }
}
