package com.securenet.auditor.ui.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.data.db.ScanResultEntity
import com.securenet.auditor.ui.components.EmptyStateView
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(viewModel: VaultViewModel) {
    val history by viewModel.scanHistory.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (!isAuthenticated) {
        LockedVault(isAuthenticating) {
            viewModel.authenticate(context as FragmentActivity)
        }
    } else {
        UnlockedVault(viewModel, history)
    }
}

@Composable
fun LockedVault(isAuthenticating: Boolean, onUnlock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = TealPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("SecureNet Vault", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Scan history is encrypted with AES-256", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        if (isAuthenticating) {
            CircularProgressIndicator(color = TealPrimary)
        } else {
            FilledTonalButton(onClick = onUnlock) {
                Text("Unlock Vault")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockedVault(viewModel: VaultViewModel, history: List<ScanResultEntity>) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Vault", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.lockVault() }) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Lock")
                    }
                }
            )
        },
        floatingActionButton = {
            if (history.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showDeleteAllDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                }
            }
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.padding(padding)) {
                EmptyStateView(
                    icon = Icons.Outlined.Lock,
                    title = "Vault Empty",
                    subtitle = "No scans saved yet. Run a network scan and tap Save to Vault."
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
                item {
                    StatsRow(history)
                }
                items(history) { entry ->
                    VaultEntryCard(entry, onDelete = { viewModel.deleteEntry(entry.id) }, onUpdateTag = { viewModel.updateTag(entry.id, it) })
                }
            }
        }

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Clear Vault") },
                text = { Text("Are you sure you want to delete all saved scan sessions? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteAll(); showDeleteAllDialog = false }) {
                        Text("Delete All", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun StatsRow(history: List<ScanResultEntity>) {
    val totalHosts = remember(history) { history.sumOf { it.hostCount } }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = history.size.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TealPrimary)
            Text(text = "Sessions", style = MaterialTheme.typography.labelSmall)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = totalHosts.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Total Hosts", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultEntryCard(entry: ScanResultEntity, onDelete: () -> Unit, onUpdateTag: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var tagText by remember { mutableStateOf(entry.tag ?: "") }
    
    val date = remember(entry.timestamp) {
        SimpleDateFormat("MMM dd yyyy, HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(date, fontWeight = FontWeight.Bold)
                    Text("${entry.hostCount} hosts discovered", color = TealPrimary, fontSize = 12.sp)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (entry.tag != null) {
                    SuggestionChip(
                        onClick = { showTagDialog = true },
                        label = { Text(entry.tag) },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                } else {
                    OutlinedIconButton(onClick = { showTagDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Tag", modifier = Modifier.size(16.dp))
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Session Details:", style = MaterialTheme.typography.labelMedium)
                    Text("IPs: ${entry.ipAddress}", style = MaterialTheme.typography.bodySmall, fontFamily = MonoType)
                    Text("Aggregated Ports: ${if (entry.openPorts.isEmpty()) "None" else entry.openPorts}", style = MaterialTheme.typography.bodySmall, fontFamily = MonoType)
                    Text("Avg Response Time: ${entry.responseTimeMs}ms", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Edit Tag") },
            text = {
                OutlinedTextField(
                    value = tagText,
                    onValueChange = { tagText = it },
                    label = { Text("Tag name") }
                )
            },
            confirmButton = {
                TextButton(onClick = { onUpdateTag(tagText); showTagDialog = false }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
