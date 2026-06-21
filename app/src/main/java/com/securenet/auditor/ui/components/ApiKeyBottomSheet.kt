package com.securenet.auditor.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.securenet.auditor.data.prefs.EncryptedPrefsManager
import com.securenet.auditor.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyBottomSheet(
    onDismiss: () -> Unit,
    onSaveKey: (service: String, key: String) -> Unit,
    onClearKey: (service: String) -> Unit,
    hibpKeySet: Boolean,
    hunterKeySet: Boolean
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                text = "API Configuration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            ApiKeySection(
                title = "HaveIBeenPwned",
                link = "https://haveibeenpwned.com/API/Key",
                isSet = hibpKeySet,
                onSave = { onSaveKey(EncryptedPrefsManager.HIBP_KEY, it) },
                onClear = { onClearKey(EncryptedPrefsManager.HIBP_KEY) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ApiKeySection(
                title = "Hunter.io",
                link = "https://hunter.io/api-keys",
                isSet = hunterKeySet,
                onSave = { onSaveKey(EncryptedPrefsManager.HUNTER_KEY, it) },
                onClear = { onClearKey(EncryptedPrefsManager.HUNTER_KEY) }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ApiKeySection(
    title: String,
    link: String,
    isSet: Boolean,
    onSave: (String) -> Unit,
    onClear: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var key by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isSet) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Set",
                        tint = SuccessGreen,
                        modifier = Modifier.padding(start = 8.dp).size(16.dp)
                    )
                }
            }
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                context.startActivity(intent)
            }) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = "Open Link",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter API Key") },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Visibility"
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onClear != null && isSet) {
                TextButton(
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear & Reset")
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            FilledTonalButton(
                onClick = { onSave(key); key = "" },
                enabled = key.isNotBlank()
            ) {
                Text("Save Key")
            }
        }
    }
}

@Composable
fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size)
    )
}
