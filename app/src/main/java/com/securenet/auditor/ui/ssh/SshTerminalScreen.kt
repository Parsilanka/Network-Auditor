package com.securenet.auditor.ui.ssh

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshTerminalScreen(
    viewModel: SshTerminalViewModel,
    onBack: () -> Unit
) {
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnecting.collectAsStateWithLifecycle()
    val output by viewModel.terminalOutput.collectAsStateWithLifecycle()
    val host by viewModel.host.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    val savedKeys by viewModel.savedKeys.collectAsState(initial = emptyList())

    var password by remember { mutableStateOf("") }
    var commandInput by remember { mutableStateOf("") }
    var selectedKeyId by remember { mutableStateOf<Int?>(null) }
    var showKeySelector by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()

    LaunchedEffect(output.size) {
        if (output.isNotEmpty()) {
            listState.animateScrollToItem(output.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSH Terminal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isConnected) {
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(Icons.Default.LinkOff, contentDescription = "Disconnect", tint = Color.Red)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!isConnected) {
                // Connection Form
                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), 
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { viewModel.setHost(it) },
                        label = { Text("Host / IP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary)
                    )
                    OutlinedTextField(
                        value = user,
                        onValueChange = { viewModel.setUser(it) },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Text("Authentication Method", style = MaterialTheme.typography.labelMedium, color = TealPrimary)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedKeyId == null, onClick = { selectedKeyId = null })
                        Text("Password")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = selectedKeyId != null, onClick = { if (savedKeys.isNotEmpty()) showKeySelector = true })
                        Text("Private Key (Vault)")
                    }

                    if (selectedKeyId == null) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                    } else {
                        val keyName = savedKeys.find { it.id == selectedKeyId }?.title ?: "Select Key"
                        OutlinedCard(
                            onClick = { showKeySelector = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = TealPrimary)
                                Spacer(Modifier.width(12.dp))
                                Text(keyName, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    }
                    
                    Button(
                        onClick = { viewModel.connect(host, user, password.ifEmpty { null }, selectedKeyId) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = host.isNotBlank() && !isConnecting,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        if (isConnecting) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                        else Text("Connect via SSH")
                    }
                }
            } else {
                // Terminal View
                Box(modifier = Modifier.weight(1f).background(Color(0xFF0D1117)).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(output) { line ->
                            TerminalLineItem(line)
                        }
                    }
                }

                // Input Bar
                Surface(
                    color = Color(0xFF161B22),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$user@$host:~$ ", color = TealPrimary, fontFamily = MonoType, fontSize = 12.sp)
                        TextField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White
                            ),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoType),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (commandInput.isNotBlank()) {
                                    viewModel.sendCommand(commandInput)
                                    commandInput = ""
                                }
                            })
                        )
                        IconButton(onClick = {
                            if (commandInput.isNotBlank()) {
                                viewModel.sendCommand(commandInput)
                                commandInput = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = TealPrimary)
                        }
                    }
                }
            }
        }
    }

    if (showKeySelector) {
        AlertDialog(
            onDismissRequest = { showKeySelector = false },
            title = { Text("Select Key from Vault") },
            text = {
                LazyColumn {
                    items(savedKeys) { key ->
                        ListItem(
                            headlineContent = { Text(key.title) },
                            supportingContent = { Text(key.username) },
                            leadingContent = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedKeyId = key.id
                                showKeySelector = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showKeySelector = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TerminalLineItem(line: SshTerminalViewModel.TerminalLine) {
    val color = when (line.type) {
        SshTerminalViewModel.TerminalLineType.INPUT -> TealPrimary
        SshTerminalViewModel.TerminalLineType.OUTPUT -> Color(0xFFE6EDF3)
        SshTerminalViewModel.TerminalLineType.ERROR -> Color(0xFFF44336)
        SshTerminalViewModel.TerminalLineType.SYSTEM -> Color(0xFF8B949E)
    }
    
    Text(
        text = line.text,
        color = color,
        fontFamily = MonoType,
        fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
    )
}
