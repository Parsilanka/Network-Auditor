package com.securenet.auditor.ui.portknocker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortKnockerScreen(
    viewModel: PortKnockerViewModel,
    onBack: () -> Unit
) {
    var targetHost by remember { mutableStateOf("") }
    val knockSteps by viewModel.sequence.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecuting.collectAsStateWithLifecycle()
    val knockResults by viewModel.knockResults.collectAsStateWithLifecycle()

    val canExecute = targetHost.isNotBlank() &&
            knockSteps.isNotEmpty() &&
            knockSteps.all { it.port.isNotBlank() && 
                    it.port.toIntOrNull() != null &&
                    it.port.toIntOrNull() in 1..65535 }

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
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = targetHost,
                        onValueChange = { targetHost = it },
                        label = { Text("Target Host / IP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            focusedLabelColor = TealPrimary,
                            focusedTextColor = Color(0xFFE6EDF3),
                            unfocusedTextColor = Color(0xFFE6EDF3)
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
                }

                itemsIndexed(knockSteps) { index, step ->
                    KnockStepCard(
                        step = step,
                        stepNumber = index + 1,
                        onPortChange = { viewModel.updateStep(index, step.copy(port = it)) },
                        onProtocolChange = { viewModel.updateStep(index, step.copy(protocol = it)) },
                        onDelayChange = { viewModel.updateStep(index, step.copy(delayMs = it)) },
                        onDelete = { viewModel.removeStep(index) }
                    )
                }

                if (knockResults.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Execution Results", fontWeight = FontWeight.Bold, color = TealPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    items(knockResults) { result ->
                        ExecutionResultRow(result)
                    }

                    if (!isExecuting && knockResults.size == knockSteps.size) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C3A2E)),
                                border = BorderStroke(1.dp, Color(0xFF00BFA5))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00BFA5))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Sequence fully executed. Target ports should now be accessible if the knocker daemon is correctly configured.", 
                                        color = Color(0xFFE6EDF3), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = { viewModel.executeKnockSequence(targetHost) },
                    enabled = canExecute && !isExecuting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00BFA5),
                        contentColor = Color(0xFF003D36),
                        disabledContainerColor = Color(0xFF21262D),
                        disabledContentColor = Color(0xFF8B949E)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF003D36))
                    } else {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Execute Knock Sequence", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun KnockStepCard(
    step: PortKnocker.UIKnockStep,
    stepNumber: Int,
    onPortChange: (String) -> Unit,
    onProtocolChange: (String) -> Unit,
    onDelayChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Surface(
                    color = Color(0xFF1C3A2E),
                    shape = CircleShape
                ) {
                    Text(
                        "$stepNumber",
                        color = Color(0xFF00BFA5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Knock Step",
                    color = Color(0xFF8B949E),
                    fontSize = 12.sp
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = step.port,
                    onValueChange = onPortChange,
                    label = { Text("Port") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00BFA5),
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedLabelColor = Color(0xFF00BFA5),
                        unfocusedLabelColor = Color(0xFF8B949E),
                        focusedTextColor = Color(0xFFE6EDF3),
                        unfocusedTextColor = Color(0xFFE6EDF3)
                    )
                )

                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00BFA5)),
                        border = BorderStroke(1.dp, Color(0xFF30363D))
                    ) {
                        Text(step.protocol)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("TCP", "UDP").forEach { protocol ->
                            DropdownMenuItem(
                                text = { Text(protocol) },
                                onClick = {
                                    onProtocolChange(protocol)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = step.delayMs,
                    onValueChange = onDelayChange,
                    label = { Text("Delay ms") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00BFA5),
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedLabelColor = Color(0xFF00BFA5),
                        unfocusedLabelColor = Color(0xFF8B949E),
                        focusedTextColor = Color(0xFFE6EDF3),
                        unfocusedTextColor = Color(0xFFE6EDF3)
                    )
                )

                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Remove step", tint = Color(0xFFF44336))
                }
            }
        }
    }
}

@Composable
fun ExecutionResultRow(result: PortKnocker.KnockResult) {
    val time = remember(result.timestamp) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(result.timestamp))
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (result.success) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Step ${result.step}: ${result.protocol} ${result.port}", 
                color = Color(0xFFE6EDF3), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(time, color = Color(0xFF8B949E), fontSize = 11.sp, fontFamily = MonoType)
        }
    }
}
