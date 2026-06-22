package com.securenet.auditor.ui.osint

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.domain.model.CombinedEmailCheckResult
import com.securenet.auditor.domain.model.OsintResult
import com.securenet.auditor.ui.components.ApiKeyBottomSheet
import com.securenet.auditor.ui.components.BreachCard
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.SuccessGreen
import com.securenet.auditor.ui.theme.TealPrimary

import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.ui.platform.LocalContext
import com.securenet.auditor.ui.components.copyToClipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OsintScreen(viewModel: OsintViewModel) {
    val emailResult by viewModel.emailResult.collectAsStateWithLifecycle()
    val domainResult by viewModel.domainResult.collectAsStateWithLifecycle()
    val emailRepResult by viewModel.emailRepResult.collectAsStateWithLifecycle()
    val ipGeoResult by viewModel.ipGeoResult.collectAsStateWithLifecycle()
    val whoisResult by viewModel.whoisResult.collectAsStateWithLifecycle()
    val malwareResult by viewModel.malwareResult.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val hibpKeySet by viewModel.hibpKeySet.collectAsStateWithLifecycle()
    val hunterKeySet by viewModel.hunterKeySet.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OSINT Intelligence", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                        }
                        if (!hibpKeySet || !hunterKeySet) {
                            Surface(
                                modifier = Modifier.size(8.dp).align(Alignment.TopEnd).padding(end = 4.dp, top = 4.dp),
                                color = Color.Red,
                                shape = CircleShape
                            ) {}
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                Tab(selected = selectedTab == 0, onClick = { viewModel.selectTab(0) }, text = { Text("Breach") })
                Tab(selected = selectedTab == 1, onClick = { viewModel.selectTab(1) }, text = { Text("Domain") })
                Tab(selected = selectedTab == 2, onClick = { viewModel.selectTab(2) }, text = { Text("Reputation") })
                Tab(selected = selectedTab == 3, onClick = { viewModel.selectTab(3) }, text = { Text("IP Geo") })
                Tab(selected = selectedTab == 4, onClick = { viewModel.selectTab(4) }, text = { Text("Whois") })
                Tab(selected = selectedTab == 5, onClick = { viewModel.selectTab(5) }, text = { Text("Malware") })
            }

            when (selectedTab) {
                0 -> EmailTab(viewModel, emailResult)
                1 -> DomainTab(viewModel, domainResult)
                2 -> ReputationTab(viewModel, emailRepResult)
                3 -> IpGeoTab(viewModel, ipGeoResult)
                4 -> WhoisTab(viewModel, whoisResult)
                5 -> MalwareTab(viewModel, malwareResult)
            }
        }
        
        if (showSettings) {
            ApiKeyBottomSheet(
                onDismiss = { showSettings = false },
                onSaveKey = { service, key -> viewModel.saveApiKey(service, key) },
                onClearKey = { viewModel.clearApiKey(it) },
                hibpKeySet = hibpKeySet,
                hunterKeySet = hunterKeySet
            )
        }
    }
}

@Composable
fun IpGeoTab(viewModel: OsintViewModel, result: OsintResult<com.securenet.auditor.data.remote.IpGeoDto>) {
    var ip by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter IP Address") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(onClick = { viewModel.checkIpGeo(ip) }, modifier = Modifier.fillMaxWidth(), enabled = result !is OsintResult.Loading && ip.isNotBlank()) {
            Text("Track IP Location")
        }
        Spacer(modifier = Modifier.height(24.dp))
        when (result) {
            is OsintResult.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            is OsintResult.Found -> {
                val data = result.data
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Location: ${data.city ?: "Unknown"}, ${data.country ?: "Unknown"}", fontWeight = FontWeight.Bold)
                        Text("ISP: ${data.isp ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                        Text("Org: ${data.org ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            StatusChip("Proxy/VPN", data.proxy == true)
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusChip("Hosting", data.hosting == true)
                        }
                    }
                }
            }
            is OsintResult.Error -> Text(result.message, color = Color.Red)
            else -> {}
        }
    }
}

@Composable
fun WhoisTab(viewModel: OsintViewModel, result: OsintResult<String>) {
    var query by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter Domain or IP") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(onClick = { viewModel.performWhois(query) }, modifier = Modifier.fillMaxWidth(), enabled = result !is OsintResult.Loading && query.isNotBlank()) {
            Text("Lookup WHOIS")
        }
        Spacer(modifier = Modifier.height(24.dp))
        when (result) {
            is OsintResult.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            is OsintResult.Found -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    LazyColumn(modifier = Modifier.padding(16.dp).heightIn(max = 400.dp)) {
                        item {
                            Text(result.data, fontFamily = MonoType, fontSize = 12.sp)
                        }
                    }
                }
            }
            is OsintResult.Error -> Text(result.message, color = Color.Red)
            else -> {}
        }
    }
}

@Composable
fun MalwareTab(viewModel: OsintViewModel, result: OsintResult<com.securenet.auditor.data.remote.UrlHausResponse>) {
    var url by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter URL or Domain") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(onClick = { viewModel.checkMalware(url) }, modifier = Modifier.fillMaxWidth(), enabled = result !is OsintResult.Loading && url.isNotBlank()) {
            Text("Check Threat Intel")
        }
        Spacer(modifier = Modifier.height(24.dp))
        when (result) {
            is OsintResult.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            is OsintResult.Found -> {
                val data = result.data
                val isMalicious = data.query_status == "ok" && data.url_status == "online"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isMalicious) Color.Red else SuccessGreen,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        if (isMalicious) "⚠ THREAT DETECTED: ${data.threat}" else "✓ NO ACTIVE THREATS FOUND",
                        modifier = Modifier.padding(16.dp), color = Color.White, fontWeight = FontWeight.Bold
                    )
                }
            }
            is OsintResult.Error -> Text(result.message, color = Color.Red)
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusChip(label: String, isActive: Boolean) {
    SuggestionChip(
        onClick = {},
        label = { Text(label, fontSize = 10.sp) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = (if (isActive) Color.Red else SuccessGreen).copy(alpha = 0.1f),
            labelColor = if (isActive) Color.Red else SuccessGreen
        )
    )
}

@Composable
fun ReputationTab(viewModel: OsintViewModel, result: OsintResult<CombinedEmailCheckResult>) {
    var email by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter email address") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(
            onClick = { viewModel.checkEmailReputation(email) },
            modifier = Modifier.fillMaxWidth(),
            enabled = result !is OsintResult.Loading && email.isNotBlank()
        ) {
            Text("Check Reputation")
        }
        Spacer(modifier = Modifier.height(24.dp))
        when (result) {
            is OsintResult.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            is OsintResult.Found -> EmailReputationCombinedCard(result.data)
            is OsintResult.NotFound -> Text("No reputation data found")
            is OsintResult.Error -> Text(result.message, color = Color.Red)
            else -> {}
        }
    }
}

@Composable
fun EmailReputationCombinedCard(data: CombinedEmailCheckResult) {
    Column {
        val bannerColor = if (data.isDisposable || !data.isFormatValid) Color.Red else SuccessGreen
        val bannerText = if (data.isDisposable) "DISPOSABLE EMAIL DETECTED" else if (!data.isFormatValid) "INVALID EMAIL FORMAT" else "EMAIL LOOKS LEGITIMATE"
        Surface(modifier = Modifier.fillMaxWidth(), color = bannerColor, shape = MaterialTheme.shapes.medium) {
            Text(bannerText, modifier = Modifier.padding(16.dp), color = Color.White, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                ReputationRow("Domain", data.domain ?: "N/A", isMonospace = true)
                Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                ReputationRowWithChip("Format Valid", if (data.isFormatValid) "VALID" else "INVALID", !data.isFormatValid)
                ReputationRowWithChip("DNS Records", if (data.hasDns) "FOUND" else "NOT FOUND", !data.hasDns)
                ReputationRowWithChip("MX Records", if (data.hasMx) "FOUND" else "NOT FOUND", !data.hasMx)
                ReputationRowWithChip("Disposable", if (data.isDisposable) "YES - DISPOSABLE" else "NO", data.isDisposable)
                ReputationRowWithChip("Alias", if (data.isAlias) "YES - ALIAS" else "NO", data.isAlias, isAmber = data.isAlias)
            }
        }
    }
}

@Composable
fun ReputationRow(label: String, value: String, isMonospace: Boolean = false) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, fontFamily = if (isMonospace) MonoType else null, color = if (isMonospace) TealPrimary else Color.Unspecified)
            if (value != "N/A" && value.isNotBlank()) {
                IconButton(onClick = { copyToClipboard(context, label, value) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = TealPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReputationRowWithChip(label: String, value: String, isError: Boolean, isAmber: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        val chipColor = if (isError) Color.Red else if (isAmber) Color(0xFFFFA000) else SuccessGreen
        SuggestionChip(onClick = {}, label = { Text(value, fontSize = 10.sp) }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = chipColor.copy(alpha = 0.1f), labelColor = chipColor))
    }
}

@Composable
fun EmailTab(viewModel: OsintViewModel, result: OsintResult<List<com.securenet.auditor.domain.model.BreachResult>>) {
    var email by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Enter email address") }, singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(onClick = { viewModel.checkEmail(email) }, modifier = Modifier.fillMaxWidth(), enabled = result !is OsintResult.Loading && email.isNotBlank()) {
            Text("Check Breaches")
        }
        Spacer(modifier = Modifier.height(24.dp))
        when (result) {
            is OsintResult.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            is OsintResult.Found -> LazyColumn { items(result.data) { breach -> BreachCard(breach) } }
            is OsintResult.NotFound -> Text("✓ No breaches found")
            is OsintResult.Error -> Text(result.message, color = Color.Red)
            else -> {}
        }
    }
}

@Composable
fun DomainTab(viewModel: OsintViewModel, result: OsintResult<com.securenet.auditor.data.remote.dto.HunterResponseDto>) {
    var domain by remember { mutableStateOf("") }
    val context = LocalContext.current
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(value = domain, onValueChange = { domain = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Enter domain") }, singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(onClick = { viewModel.searchDomain(domain) }, modifier = Modifier.fillMaxWidth(), enabled = result !is OsintResult.Loading && domain.isNotBlank()) {
            Text("Search Emails")
        }
        Spacer(modifier = Modifier.height(24.dp))
        when (result) {
            is OsintResult.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            is OsintResult.Found -> {
                val data = result.data.data
                Text(text = data?.organization ?: "Organization Unknown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LazyColumn {
                    items(data?.emails ?: emptyList()) { emailData ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(emailData.value, fontFamily = MonoType)
                                    IconButton(onClick = { copyToClipboard(context, "Email", emailData.value) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = TealPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is OsintResult.Error -> Text(result.message, color = Color.Red)
            else -> {}
        }
    }
}
