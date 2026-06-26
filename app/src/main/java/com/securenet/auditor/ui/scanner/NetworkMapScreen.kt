package com.securenet.auditor.ui.scanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Radar
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
import com.securenet.auditor.SecureNetApp
import com.securenet.auditor.domain.model.HostInfo
import com.securenet.auditor.domain.model.ScanProgress
import com.securenet.auditor.ui.components.EmptyStateView
import com.securenet.auditor.ui.components.HostCard
import com.securenet.auditor.ui.dashboard.getWifiInfo
import com.securenet.auditor.ui.navigation.Screen
import com.securenet.auditor.ui.report.PdfReportGenerator
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.SuccessGreen
import com.securenet.auditor.ui.theme.TealPrimary
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.platform.LocalContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkMapScreen(navController: NavController, viewModel: ScannerViewModel) {
    val TAG = "NetworkMapScreen"
    val progress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val hosts by viewModel.discoveredHosts.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val subnet by viewModel.currentSubnet.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Network Map", fontWeight = FontWeight.Bold)
                        if (subnet != null) {
                            Text(
                                text = "${subnet}.x",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = MonoType,
                                color = TealPrimary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (isScanning) viewModel.stopScan() else viewModel.startScan() },
                containerColor = if (isScanning) MaterialTheme.colorScheme.error else SuccessGreen,
                contentColor = Color.White,
                icon = {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                },
                text = { Text(if (isScanning) "Stop Scan" else "Start Scan") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isScanning) {
                val currentProgress = when (val p = progress) {
                    is ScanProgress.Scanning -> p.current / 254f
                    else -> 0f
                }
                LinearProgressIndicator(
                    progress = currentProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = TealPrimary,
                    trackColor = TealPrimary.copy(alpha = 0.1f)
                )
            }

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("${hosts.size} hosts found") },
                    colors = AssistChipDefaults.assistChipColors(labelColor = TealPrimary)
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${hosts.sumOf { it.openPorts.size }} ports open") }
                )
            }

            if (hosts.isEmpty() && !isScanning) {
                EmptyStateView(
                    icon = Icons.Outlined.Radar,
                    title = "Network Discovery",
                    subtitle = "Tap scan to discover devices on your network"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(hosts) { host ->
                        HostCard(
                            host = host,
                            onPortScan = { viewModel.runPortScan(it) },
                            onGeolocate = { navController.navigate(Screen.GeoLocation.route + "?ip=${host.ipAddress}") },
                            onSnmpQuery = { navController.navigate(Screen.SnmpInspector.route + "?ip=${host.ipAddress}") }
                        )
                    }
                    
                    if (progress is ScanProgress.Complete) {
                        item {
                            val container = (context.applicationContext as SecureNetApp).container
                            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                TextButton(
                                    onClick = { },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false
                                ) {
                                    Text("All hosts saved to vault", color = SuccessGreen)
                                }
                                
                                FilledTonalButton(
                                    onClick = {
                                        Log.d(TAG, "Generating PDF report...")
                                        val wifiInfo = getWifiInfo(context)
                                        val reportData = createReportData(
                                            hosts = hosts,
                                            networkName = wifiInfo.first,
                                            networkIp = "${subnet ?: "Unknown"}.x",
                                            duration = (progress as ScanProgress.Complete).durationMs
                                        )
                                        val uri = container.pdfReportGenerator.generateReport(reportData)
                                        Log.d(TAG, "PDF Uri: $uri")
                                        uri?.let { sharePdf(context, it) }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate PDF Report")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun createReportData(hosts: List<HostInfo>, networkName: String, networkIp: String, duration: Long): PdfReportGenerator.ReportData {
    val critical = mutableListOf<String>()
    val high = mutableListOf<String>()
    val medium = mutableListOf<String>()
    val low = mutableListOf<String>()

    hosts.forEach { host ->
        host.openPorts.forEach { port ->
            when (port) {
                23, 445, 3306 -> critical.add("Critical: Port $port open on ${host.ipAddress}")
                21, 3389, 5900 -> high.add("High: Port $port open on ${host.ipAddress}")
                80, 8080 -> medium.add("Medium: Port $port open on ${host.ipAddress}")
                else -> low.add("Low: Port $port open on ${host.ipAddress}")
            }
        }
    }

    val riskScore = (100 - (critical.size * 20 + high.size * 10 + medium.size * 5)).coerceIn(0, 100)

    return PdfReportGenerator.ReportData(
        scanDate = Date(),
        networkName = networkName,
        networkIp = networkIp,
        hostsFound = hosts,
        scanDurationMs = duration,
        openPortsCount = hosts.sumOf { it.openPorts.size },
        criticalFindings = critical,
        highFindings = high,
        mediumFindings = medium,
        lowFindings = low,
        overallRiskScore = riskScore
    )
}

fun sharePdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Security Report"))
}
