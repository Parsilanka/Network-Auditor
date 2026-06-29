package com.securenet.auditor.ui.subdomain

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.securenet.auditor.network.SubdomainEnumerator
import com.securenet.auditor.ui.navigation.Screen
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubdomainScreen(
    navController: NavController,
    viewModel: SubdomainViewModel,
    onBack: () -> Unit
) {
    var domain by remember { mutableStateOf("") }
    val results by viewModel.results.collectAsStateWithLifecycle()
    val isEnumerating by viewModel.isEnumerating.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val currentChecking by viewModel.currentChecking.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Subdomain Enumerator", fontWeight = FontWeight.Bold)
                        Text("DNS reconnaissance tool", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // Input
            OutlinedTextField(
                value = domain,
                onValueChange = { 
                    domain = it.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore("/")
                },
                label = { Text("Enter domain (e.g. example.com)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("github.com", "shopify.com").forEach { d ->
                    AssistChip(onClick = { domain = d }, label = { Text(d) })
                }
            }

            Button(
                onClick = { if (isEnumerating) viewModel.stopEnumeration() else viewModel.startEnumeration(domain) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isEnumerating) Color.Red else TealPrimary),
                enabled = domain.isNotBlank()
            ) {
                Text(if (isEnumerating) "Stop" else "Start Enumeration")
            }

            if (isEnumerating) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth(), color = TealPrimary)
                Text(
                    "Checking: $currentChecking (${(progress * 200).toInt()}/200)",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = MonoType,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Stats
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${results.size} found", fontWeight = FontWeight.Bold, color = TealPrimary)
                Text("${(progress * 200).toInt()} checked", color = Color.Gray)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { res ->
                    SubdomainCard(res, navController)
                }
            }
        }
    }
}

@Composable
fun SubdomainCard(res: SubdomainEnumerator.SubdomainResult, navController: NavController) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(res.fullDomain, fontWeight = FontWeight.Bold, fontFamily = MonoType, color = TealPrimary)
                val statusColor = when(res.httpStatus) {
                    200 -> Color(0xFF4CAF50)
                    301, 302 -> Color(0xFF2196F3)
                    403 -> Color(0xFFFFC107)
                    500 -> Color.Red
                    else -> Color.Gray
                }
                Surface(color = statusColor.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small) {
                    Text(
                        res.httpStatus?.let { "$it OK" } ?: "DNS Only",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text("IPs: ${res.ipAddresses.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${res.responseTimeMs}ms", style = MaterialTheme.typography.labelSmall, fontFamily = MonoType)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { navController.navigate(Screen.Scanner.route + "?ip=${res.ipAddresses.firstOrNull() ?: ""}") }) {
                        Text("Scan Ports", fontSize = 12.sp)
                    }
                    TextButton(onClick = { navController.navigate(Screen.GeoLocation.route + "?ip=${res.ipAddresses.firstOrNull() ?: ""}") }) {
                        Text("Geolocate", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
