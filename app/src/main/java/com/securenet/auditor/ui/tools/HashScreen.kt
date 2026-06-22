package com.securenet.auditor.ui.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
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
import com.securenet.auditor.ui.theme.SuccessGreen
import com.securenet.auditor.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashScreen(
    viewModel: HashViewModel,
    onBack: () -> Unit
) {
    val hashResult by viewModel.hashResult.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var textInput by remember { mutableStateOf("") }
    var compareInput by remember { mutableStateOf("") }
    var selectedAlgorithm by remember { mutableStateOf(HashAlgorithm.SHA256) }
    var selectedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedFileUri = uri
        fileName = uri?.let { u ->
            context.contentResolver.query(u, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hash Generator & Verifier", fontWeight = FontWeight.Bold) },
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
            Text("Select Algorithm", style = MaterialTheme.typography.titleSmall, color = TealPrimary)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HashAlgorithm.values().forEach { algo ->
                    FilterChip(
                        selected = selectedAlgorithm == algo,
                        onClick = { selectedAlgorithm = algo },
                        label = { Text(algo.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text Input Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hash Text", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { 
                            textInput = it
                            selectedFileUri = null
                            fileName = null
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        label = { Text("Enter text to hash") },
                        minLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // File Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hash File", fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(fileName ?: "Select File")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Verification Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Verify Hash (Optional)", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = compareInput,
                        onValueChange = { compareInput = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        label = { Text("Enter known hash to compare") },
                        placeholder = { Text("Paste hash here...") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (selectedFileUri != null) {
                        viewModel.generateFileHash(context, selectedFileUri!!, selectedAlgorithm, compareInput)
                    } else if (textInput.isNotBlank()) {
                        viewModel.generateTextHash(textInput, selectedAlgorithm, compareInput)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing && (textInput.isNotBlank() || selectedFileUri != null),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("GENERATE HASH")
            }

            Spacer(modifier = Modifier.height(24.dp))

            hashResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${result.algorithm.name} Result", fontWeight = FontWeight.Bold, color = TealPrimary)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { copyToClipboard(context, "Hash", result.hash) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                            }
                        }
                        
                        Text(
                            text = result.hash,
                            fontFamily = MonoType,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        if (result.isMatch != null) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (result.isMatch) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (result.isMatch) SuccessGreen else Color.Red
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (result.isMatch) "Hash Match Confirmed" else "Hash Mismatch!",
                                    fontWeight = FontWeight.Bold,
                                    color = if (result.isMatch) SuccessGreen else Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
