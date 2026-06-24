package com.securenet.auditor.ui.geolocation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.securenet.auditor.domain.model.GeoLocationResult
import com.securenet.auditor.domain.model.OsintResult
import com.securenet.auditor.domain.model.ThreatLevel
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoLocationScreen(
    navController: NavController,
    viewModel: GeoLocationViewModel,
    initialIp: String? = null
) {
    var ipInput by remember { mutableStateOf(initialIp ?: "") }
    val lookupResult by viewModel.lookupResult.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    var showHistory by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(initialIp) {
        if (!initialIp.isNullOrBlank()) {
            viewModel.lookupIp(initialIp)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("IP Geolocation", fontWeight = FontWeight.Bold)
                        Text("Powered by ip-api.com", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, contentDescription = "History")
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search Section
            OutlinedTextField(
                value = ipInput,
                onValueChange = { ipInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter IP address or domain") },
                trailingIcon = {
                    if (ipInput.isNotEmpty()) {
                        IconButton(onClick = { ipInput = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontFamily = MonoType)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("My IP", "8.8.8.8", "1.1.1.1").forEach { label ->
                    AssistChip(
                        onClick = {
                            if (label == "My IP") {
                                viewModel.lookupMyIp()
                                ipInput = ""
                            } else {
                                ipInput = label
                                viewModel.lookupIp(label)
                            }
                        },
                        label = { Text(label) }
                    )
                }
                AssistChip(
                    onClick = { ipInput = "" },
                    label = { Text("Custom") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.lookupIp(ipInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Locate")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Results Section
            when (val result = lookupResult) {
                is OsintResult.Found -> {
                    GeoLocationResultContent(result.data)
                }
                is OsintResult.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = result.message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                else -> {}
            }
        }

        if (showHistory) {
            ModalBottomSheet(onDismissRequest = { showHistory = false }) {
                HistoryContent(searchHistory) { ip ->
                    ipInput = ip
                    viewModel.lookupIp(ip)
                    showHistory = false
                }
            }
        }
    }
}

@Composable
fun GeoLocationResultContent(result: GeoLocationResult) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Threat Banner
        val bannerColor = when (result.threatLevel) {
            ThreatLevel.CLEAN -> Color(0xFF4CAF50)
            ThreatLevel.SUSPICIOUS -> Color(0xFFFFC107)
            ThreatLevel.DANGEROUS -> Color(0xFFF44336)
        }
        val bannerText = when (result.threatLevel) {
            ThreatLevel.CLEAN -> "✓ CLEAN IP — No threats detected"
            ThreatLevel.SUSPICIOUS -> "⚠ SUSPICIOUS — Proxy or Hosting detected"
            ThreatLevel.DANGEROUS -> "✗ DANGEROUS — VPN/Proxy + Hosting detected"
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = bannerColor,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = bannerText,
                modifier = Modifier.padding(12.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Location Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val flag = result.countryCode.uppercase().map {
                        String(Character.toChars(it.code - 'A'.code + 0x1F1E6))
                    }.joinToString("")
                    Text(flag, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = result.ipAddress,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = MonoType,
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoCell(Modifier.weight(1f), "Country", result.country)
                    InfoCell(Modifier.weight(1f), "Region", result.regionName)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoCell(Modifier.weight(1f), "City", result.city)
                    InfoCell(Modifier.weight(1f), "ZIP Code", result.zipCode)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoCell(Modifier.weight(1f), "Timezone", result.timezone)
                    InfoCell(Modifier.weight(1f), "Coordinates", "${result.latitude}, ${result.longitude}")
                }
            }
        }

        // ISP Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Network Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                NetworkRow("ISP", result.isp)
                NetworkRow("Organization", result.organization)
                NetworkRow("AS Number", result.asNumber, isTeal = true)

                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip("PROXY", result.isProxy, Color(0xFFF44336))
                    StatusChip("HOSTING/VDC", result.isHosting, Color(0xFFF44336))
                    StatusChip("MOBILE", result.isMobile, Color(0xFF2196F3))
                }
            }
        }

        // Map Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Location Map",
                    modifier = Modifier.align(Alignment.Start),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                WorldMapCanvas(result.latitude, result.longitude)

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Lat: ${result.latitude}° | Lon: ${result.longitude}°",
                    fontFamily = MonoType,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "View on OpenStreetMap →",
                    color = TealPrimary,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.mapUrl))
                        context.startActivity(intent)
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        OutlinedButton(
            onClick = {
                val report = """
                    ═══ IP GEOLOCATION REPORT ═══
                    IP Address: ${result.ipAddress}
                    Location: ${result.city}, ${result.regionName}, ${result.country}
                    Coordinates: ${result.latitude}, ${result.longitude}
                    ISP: ${result.isp}
                    Organization: ${result.organization}
                    AS Number: ${result.asNumber}
                    Timezone: ${result.timezone}
                    Proxy: ${result.isProxy}
                    Hosting: ${result.isHosting}
                    Threat Level: ${result.threatLevel}
                    ═══ Generated by Network Auditor ═══
                """.trimIndent()
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, report)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Report"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Share Report")
        }
    }
}

@Composable
fun WorldMapCanvas(lat: Double, lon: Double) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color(0xFF0D1117))
    ) {
        val w = size.width
        val h = size.height

        // Grid lines
        for (i in 1 until 10) {
            drawLine(Color(0xFF21262D), Offset(w * i / 10, 0f), Offset(w * i / 10, h))
            drawLine(Color(0xFF21262D), Offset(0f, h * i / 10), Offset(w, h * i / 10))
        }

        val x = ((lon + 180) / 360 * w).toFloat()
        val y = ((90 - lat) / 180 * h).toFloat()

        // Crosshairs
        drawLine(TealPrimary.copy(alpha = 0.3f), Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
        drawLine(TealPrimary.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())

        // Pulsing circle
        drawCircle(
            color = TealPrimary,
            radius = 20.dp.toPx() * pulseAnim,
            center = Offset(x, y),
            style = Stroke(width = 2.dp.toPx() * (1 - pulseAnim)),
            alpha = 1 - pulseAnim
        )

        // Exact point
        drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(x, y))
    }
}

@Composable
fun InfoCell(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NetworkRow(label: String, value: String?, isTeal: Boolean = false) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(
            text = value ?: "Unknown",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = MonoType,
            color = if (isTeal) TealPrimary else Color.White,
            fontWeight = if (isTeal) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun StatusChip(label: String, active: Boolean, activeColor: Color) {
    Surface(
        color = if (active) activeColor.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) activeColor else Color.Gray
        )
    }
}

@Composable
fun HistoryContent(history: List<GeoLocationResult>, onIpClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
        item {
            Text(
                "Recent Lookups",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (history.isEmpty()) {
            item {
                Text("No history yet", modifier = Modifier.padding(16.dp), color = Color.Gray)
            }
        } else {
            items(history) { item ->
                ListItem(
                    modifier = Modifier.clickable { onIpClick(item.ipAddress) },
                    headlineContent = { Text(item.ipAddress, fontFamily = MonoType) },
                    supportingContent = { Text("${item.city}, ${item.country}") },
                    trailingContent = {
                        val color = when (item.threatLevel) {
                            ThreatLevel.CLEAN -> Color(0xFF4CAF50)
                            ThreatLevel.SUSPICIOUS -> Color(0xFFFFC107)
                            ThreatLevel.DANGEROUS -> Color(0xFFF44336)
                        }
                        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                    }
                )
            }
        }
    }
}
