package com.securenet.auditor.ui.ssl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.network.SslTlsScanner
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SslScannerScreen(
    viewModel: SslScannerViewModel,
    onBack: () -> Unit
) {
    var domain by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }
    val result by viewModel.scanResult.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("SSL/TLS Scanner", fontWeight = FontWeight.Bold)
                        Text("Security certificate analyzer", style = MaterialTheme.typography.labelSmall)
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF161B22)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp, Color(0xFF30363D))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // FIX 1 — Domain input field
                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text("Enter domain or IP") },
                        placeholder = { 
                            Text(
                                "e.g. google.com",
                                color = Color(0xFF8B949E)
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Language,
                                contentDescription = null,
                                tint = Color(0xFF00BFA5)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00BFA5),
                            unfocusedBorderColor = Color(0xFF30363D),
                            focusedLabelColor = Color(0xFF00BFA5),
                            unfocusedLabelColor = Color(0xFF8B949E),
                            cursorColor = Color(0xFF00BFA5),
                            focusedTextColor = Color(0xFFE6EDF3),
                            unfocusedTextColor = Color(0xFFE6EDF3)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        )
                    )

                    // FIX 2 — Port input field
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        placeholder = { 
                            Text("443", color = Color(0xFF8B949E)) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00BFA5),
                            unfocusedBorderColor = Color(0xFF30363D),
                            focusedLabelColor = Color(0xFF00BFA5),
                            unfocusedLabelColor = Color(0xFF8B949E),
                            cursorColor = Color(0xFF00BFA5),
                            focusedTextColor = Color(0xFFE6EDF3),
                            unfocusedTextColor = Color(0xFFE6EDF3)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )

                    // FIX 3 — Port quick-select chips
                    val portOptions = listOf("443", "8443", "465", "993")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        portOptions.forEach { portOption ->
                            FilterChip(
                                selected = port == portOption,
                                onClick = { port = portOption },
                                label = {
                                    Text(
                                        text = portOption,
                                        fontFamily = MonoType,
                                        fontSize = 13.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00BFA5),
                                    selectedLabelColor = Color(0xFF003D36),
                                    containerColor = Color(0xFF21262D),
                                    labelColor = Color(0xFF8B949E)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = port == portOption,
                                    borderColor = Color(0xFF30363D),
                                    selectedBorderColor = Color(0xFF00BFA5)
                                )
                            )
                        }
                    }

                    // FIX 5 — Add quick domain buttons
                    Text(
                        text = "Quick test domains:",
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "google.com",
                            "github.com", 
                            "expired.badssl.com",
                            "self-signed.badssl.com"
                        ).forEach { quickDomain ->
                            SuggestionChip(
                                onClick = { 
                                    domain = quickDomain
                                    port = "443"
                                },
                                label = {
                                    Text(
                                        text = quickDomain,
                                        fontSize = 11.sp,
                                        fontFamily = MonoType
                                    )
                                },
                                colors = SuggestionChipDefaults
                                    .suggestionChipColors(
                                    containerColor = Color(0xFF161B22),
                                    labelColor = Color(0xFF8B949E)
                                ),
                                border = SuggestionChipDefaults
                                    .suggestionChipBorder(
                                    enabled = true,
                                    borderColor = Color(0xFF30363D)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // FIX 4 — Analyze SSL button
                    Button(
                        onClick = {
                            if (domain.isNotBlank()) {
                                viewModel.scan(
                                    domain.trim(),
                                    port.toIntOrNull() ?: 443
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = domain.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BFA5),
                            contentColor = Color(0xFF003D36),
                            disabledContainerColor = Color(0xFF21262D),
                            disabledContentColor = Color(0xFF8B949E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF003D36),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Analyzing SSL/TLS...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Analyze SSL/TLS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            if (error != null) {
                Text(error!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }

            result?.let { res ->
                GradeCard(res.grade)
                CertificateCard(res.certificate)
                ProtocolTable(res.supportedProtocols)
                VulnerabilitiesList(res.vulnerabilities)
                SecurityHeadersTable(res.securityHeaders)
                
                Button(
                    onClick = { /* Export PDF logic */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export SSL Report")
                }
            }
        }
    }
}

@Composable
fun GradeCard(grade: SslTlsScanner.SslGrade) {
    val (color, text) = when (grade) {
        SslTlsScanner.SslGrade.A_PLUS -> Color(0xFF2E7D32) to "A+"
        SslTlsScanner.SslGrade.A -> Color(0xFF4CAF50) to "A"
        SslTlsScanner.SslGrade.B -> TealPrimary to "B"
        SslTlsScanner.SslGrade.C -> Color(0xFFFFC107) to "C"
        SslTlsScanner.SslGrade.D -> Color(0xFFFF9800) to "D"
        SslTlsScanner.SslGrade.F -> Color(0xFFF44336) to "F"
        SslTlsScanner.SslGrade.T -> Color(0xFF9C27B0) to "T"
        SslTlsScanner.SslGrade.M -> Color.Gray to "M"
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = color
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Overall Rating", fontWeight = FontWeight.Bold, color = Color(0xFFE6EDF3))
                Text(getGradeExplanation(grade), style = MaterialTheme.typography.bodySmall, color = Color(0xFF8B949E))
            }
        }
    }
}

fun getGradeExplanation(grade: SslTlsScanner.SslGrade) = when(grade) {
    SslTlsScanner.SslGrade.A_PLUS -> "Exceptional configuration with TLS 1.3 support."
    SslTlsScanner.SslGrade.A -> "Good security configuration."
    SslTlsScanner.SslGrade.B -> "Solid configuration with minor issues."
    SslTlsScanner.SslGrade.C -> "Weak configuration, contains deprecated features."
    SslTlsScanner.SslGrade.D -> "Insecure configuration, high risk of attack."
    SslTlsScanner.SslGrade.F -> "Critically insecure, vulnerable to known exploits."
    SslTlsScanner.SslGrade.T -> "Certificate is trusted but currently expired."
    SslTlsScanner.SslGrade.M -> "Certificate is not trusted (Self-signed or invalid CA)."
}

@Composable
fun CertificateCard(cert: SslTlsScanner.CertificateInfo) {
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Certificate Details", fontWeight = FontWeight.Bold, color = TealPrimary)
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            
            if (cert.subject.startsWith("Error:")) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF3D0C0C))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFF44336)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            cert.subject,
                            color = Color(0xFFFF7B72),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                // Timeline
                val total = cert.validUntil.time - cert.validFrom.time
                val elapsed = Date().time - cert.validFrom.time
                val progress = (elapsed.toFloat() / total).coerceIn(0f, 1f)
                
                Column {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (cert.isExpired) Color.Red else TealPrimary,
                        trackColor = Color.Gray.copy(alpha = 0.2f)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(df.format(cert.validFrom), fontSize = 10.sp, color = Color(0xFF8B949E))
                        Text(if (cert.isExpired) "EXPIRED" else "${cert.daysUntilExpiry} days left", fontSize = 10.sp, color = if (cert.isExpired) Color.Red else TealPrimary)
                        Text(df.format(cert.validUntil), fontSize = 10.sp, color = Color(0xFF8B949E))
                    }
                }

                CertRow("Subject", cert.subject)
                CertRow("Issuer", cert.issuer)
                CertRow("Key Size", "${cert.keySize} bits")
                CertRow("Algorithm", cert.signatureAlgorithm)
                CertRow("Serial", cert.serialNumber, isMono = true)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cert.isWildcard) SuggestionChip(onClick = {}, label = { Text("Wildcard", fontSize = 10.sp, color = Color(0xFFE6EDF3)) })
                    if (cert.isSelfSigned) SuggestionChip(onClick = {}, label = { Text("Self-Signed", fontSize = 10.sp, color = Color.Red) }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.Red.copy(alpha = 0.1f)))
                }
            }
        }
    }
}

@Composable
fun CertRow(label: String, value: String, isMono: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8B949E))
        Text(
            value.ifBlank { "Unable to retrieve" },
            fontSize = 14.sp,
            color = Color(0xFFE6EDF3),
            fontFamily = if (isMono) MonoType else null
        )
    }
}

@Composable
fun ProtocolTable(protocols: List<SslTlsScanner.ProtocolResult>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Protocol Support", fontWeight = FontWeight.Bold, color = TealPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            protocols.forEach { p ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        p.version,
                        color = Color(0xFFE6EDF3),
                        fontFamily = MonoType,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (p.isSupported) "✓ Enabled" else "✗ Disabled",
                        color = if (p.isSupported) {
                            if (p.riskLevel == SslTlsScanner.RiskLevel.CRITICAL) Color(0xFFF44336) else Color(0xFF4CAF50)
                        } else Color(0xFF8B949E),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = (if (p.riskLevel == SslTlsScanner.RiskLevel.CRITICAL) Color.Red else Color(0xFF4CAF50)).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            if (p.riskLevel == SslTlsScanner.RiskLevel.CRITICAL) "RISKY" else "SECURE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (p.riskLevel == SslTlsScanner.RiskLevel.CRITICAL) Color.Red else Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VulnerabilitiesList(vulns: List<SslTlsScanner.SslVulnerability>) {
    Text("Vulnerabilities", fontWeight = FontWeight.Bold, color = TealPrimary)
    vulns.filter { it.isVulnerable }.forEach { v ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val color = when (v.severity) {
                        SslTlsScanner.RiskLevel.CRITICAL -> Color.Red
                        SslTlsScanner.RiskLevel.HIGH -> Color(0xFFFF9800)
                        SslTlsScanner.RiskLevel.MEDIUM -> Color(0xFFFFC107)
                        else -> Color.Gray
                    }
                    Surface(color = color, shape = MaterialTheme.shapes.small) {
                        Text(v.severity.name, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        v.name,
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                if (v.cveId != null) {
                    Text(v.cveId, fontFamily = MonoType, fontSize = 10.sp, color = Color(0xFF8B949E), modifier = Modifier.padding(top = 4.dp))
                }
                Text(
                    v.description,
                    color = Color(0xFFE6EDF3),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Recommendation: ${v.recommendation}",
                    color = Color(0xFF56D364),
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SecurityHeadersTable(headers: Map<String, String?>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Security Headers", fontWeight = FontWeight.Bold, color = TealPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            headers.forEach { (name, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, color = Color(0xFFE6EDF3), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(if (value != null) "✅ Yes" else "❌ No", fontSize = 12.sp, color = if (value != null) Color(0xFF4CAF50) else Color.Red)
                }
            }
        }
    }
}
