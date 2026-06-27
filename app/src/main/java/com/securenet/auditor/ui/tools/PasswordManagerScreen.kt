package com.securenet.auditor.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.securenet.auditor.data.db.PasswordEntity
import com.securenet.auditor.ui.navigation.Screen
import com.securenet.auditor.ui.theme.TealPrimary
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.outlined.Lock
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerScreen(navController: NavController, viewModel: PasswordViewModel) {
    val passwords by viewModel.allPasswords.collectAsState(initial = emptyList())
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showAddDialog by remember { mutableStateOf(false) }

    if (!isAuthenticated) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TealPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Password Manager Locked", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Authentication required to access saved credentials", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (isAuthenticating) {
                CircularProgressIndicator(color = TealPrimary)
            } else {
                Button(
                    onClick = { viewModel.authenticate(context as FragmentActivity) },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Unlock Manager")
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Password Manager", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.lock() }) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock")
                        }
                        TextButton(onClick = { navController.navigate(Screen.PasswordAuditor.route) }) {
                            Text("Audit")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }, containerColor = TealPrimary, contentColor = Color.White) {
                    Icon(Icons.Default.Add, contentDescription = "Add Password")
                }
            }
        ) { padding ->
            if (passwords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No passwords saved yet.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(passwords) { password ->
                        PasswordItem(password, onDelete = { viewModel.deletePassword(password) })
                    }
                }
            }

            if (showAddDialog) {
                AddPasswordDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { title, user, pass, site ->
                        viewModel.addPassword(PasswordEntity(title = title, username = user, encryptedPassword = pass, website = site))
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun PasswordItem(password: PasswordEntity, onDelete: () -> Unit) {
    var showPassword by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(password.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(password.username, style = MaterialTheme.typography.bodyMedium)
                    if (!password.website.isNullOrBlank()) {
                        Text(password.website, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Row {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle Visibility")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (showPassword) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Password: ${password.encryptedPassword}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun AddPasswordDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title (e.g. Google)") })
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username/Email") })
                OutlinedTextField(
                    value = password, 
                    onValueChange = { password = it }, 
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { password = generateRandomPassword() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Generate")
                        }
                    }
                )
                OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website (Optional)") })
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank() && password.isNotBlank()) onConfirm(title, username, password, website) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun generateRandomPassword(length: Int = 16): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
    return (1..length)
        .map { chars[Random.nextInt(chars.length)] }
        .joinToString("")
}
