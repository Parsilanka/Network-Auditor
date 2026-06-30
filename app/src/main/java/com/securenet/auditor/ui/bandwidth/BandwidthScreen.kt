package com.securenet.auditor.ui.bandwidth

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.network.BandwidthMonitor
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandwidthScreen(
    viewModel: BandwidthViewModel,
    onBack: () -> Unit
) {
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()
    val isMonitoring by viewModel.isMonitoring.collectAsStateWithLifecycle()
    val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()
    val appUsage by viewModel.appUsage.collectAsStateWithLifecycle()

    val showDownload by viewModel.showDownload.collectAsStateWithLifecycle()
    val showUpload by viewModel.showUpload.collectAsStateWithLifecycle()
    val showSpeedometer by viewModel.showSpeedometer.collectAsStateWithLifecycle()
    val peakDownload by viewModel.peakDownload.collectAsStateWithLifecycle()
    val peakUpload by viewModel.peakUpload.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Bandwidth Monitor", fontWeight = FontWeight.Bold)
                        Text("Real-time network traffic", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { if (isMonitoring) viewModel.stopMonitoring() else viewModel.startMonitoring() }) {
                        Icon(
                            imageVector = if (isMonitoring) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = "Toggle Recording",
                            tint = if (isMonitoring) Color.Red else Color.Gray
                        )
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Bandwidth Settings",
                            tint = Color(0xFF8B949E)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFF161B22), contentColor = TealPrimary) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Real-time") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("App Usage") })
            }

            if (selectedTab == 0) {
                RealTimeTab(currentSpeed, snapshots, showDownload, showUpload, showSpeedometer, peakDownload, peakUpload)
            } else {
                AppUsageTab(viewModel, appUsage)
            }
        }

        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = Color(0xFF161B22),
                contentColor = Color(0xFFE6EDF3)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    // Sheet title
                    Text(
                        text = "Bandwidth Monitor Settings",
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(
                            bottom = 20.dp)
                    )

                    HorizontalDivider(color = Color(0xFF30363D))
                    Spacer(modifier = Modifier.height(16.dp))

                    // ── UPDATE INTERVAL ──
                    Text(
                        text = "Update Interval",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var selectedInterval by remember {
                        mutableStateOf(1000L) }

                    Row(
                        horizontalArrangement = 
                            Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair("0.5s", 500L),
                            Pair("1s", 1000L),
                            Pair("2s", 2000L),
                            Pair("5s", 5000L)
                        ).forEach { (label, ms) ->
                            FilterChip(
                                selected = selectedInterval == ms,
                                onClick = {
                                    selectedInterval = ms
                                    viewModel.setUpdateInterval(ms)
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults
                                    .filterChipColors(
                                    selectedContainerColor =
                                        Color(0xFF00BFA5),
                                    selectedLabelColor =
                                        Color(0xFF003D36),
                                    containerColor =
                                        Color(0xFF21262D),
                                    labelColor =
                                        Color(0xFF8B949E)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFF30363D))
                    Spacer(modifier = Modifier.height(16.dp))

                    // ── GRAPH HISTORY DURATION ──
                    Text(
                        text = "Graph History",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var selectedHistory by remember {
                        mutableStateOf(60) }

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair("30s", 30),
                            Pair("60s", 60),
                            Pair("2min", 120),
                            Pair("5min", 300)
                        ).forEach { (label, seconds) ->
                            FilterChip(
                                selected = 
                                    selectedHistory == seconds,
                                onClick = {
                                    selectedHistory = seconds
                                    viewModel.setHistoryDuration(
                                        seconds)
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults
                                    .filterChipColors(
                                    selectedContainerColor =
                                        Color(0xFF00BFA5),
                                    selectedLabelColor =
                                        Color(0xFF003D36),
                                    containerColor =
                                        Color(0xFF21262D),
                                    labelColor =
                                        Color(0xFF8B949E)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFF30363D))
                    Spacer(modifier = Modifier.height(16.dp))

                    // ── GRAPH DISPLAY OPTIONS ──
                    Text(
                        text = "Display Options",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Show Download toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Show Download Line",
                                color = Color(0xFFE6EDF3),
                                fontSize = 14.sp
                            )
                            Text(
                                "Teal line on graph",
                                color = Color(0xFF8B949E),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = showDownload,
                            onCheckedChange = {
                                viewModel.setShowDownload(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor =
                                    Color(0xFF003D36),
                                checkedTrackColor =
                                    Color(0xFF00BFA5)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Show Upload toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Show Upload Line",
                                color = Color(0xFFE6EDF3),
                                fontSize = 14.sp
                            )
                            Text(
                                "Blue line on graph",
                                color = Color(0xFF8B949E),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = showUpload,
                            onCheckedChange = {
                                viewModel.setShowUpload(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor =
                                    Color(0xFF003D36),
                                checkedTrackColor =
                                    Color(0xFF00BFA5)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Show Speedometer toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Show Speedometer",
                                color = Color(0xFFE6EDF3),
                                fontSize = 14.sp
                            )
                            Text(
                                "Animated speed gauge",
                                color = Color(0xFF8B949E),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = showSpeedometer,
                            onCheckedChange = {
                                viewModel.setShowSpeedometer(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor =
                                    Color(0xFF003D36),
                                checkedTrackColor =
                                    Color(0xFF00BFA5)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFF30363D))
                    Spacer(modifier = Modifier.height(16.dp))

                    // ── DANGER ZONE ──
                    Text(
                        text = "Data",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Reset peak values button
                    OutlinedButton(
                        onClick = {
                            viewModel.resetPeakValues()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(
                            1.dp, Color(0xFF30363D)),
                        colors = ButtonDefaults
                            .outlinedButtonColors(
                            contentColor = Color(0xFFE6EDF3)
                        )
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Peak Values")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Clear history button
                    OutlinedButton(
                        onClick = {
                            viewModel.clearHistory()
                            showSettingsSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(
                            1.dp, Color(0xFFF44336)),
                        colors = ButtonDefaults
                            .outlinedButtonColors(
                            contentColor = Color(0xFFF44336)
                        )
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Graph History")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Export CSV button
                    FilledTonalButton(
                        onClick = {
                            viewModel.exportToCsv()
                            showSettingsSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults
                            .filledTonalButtonColors(
                            containerColor = Color(0xFF1C3A2E),
                            contentColor = Color(0xFF00BFA5)
                        )
                    ) {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export as CSV")
                    }
                }
            }
        }
    }
}

@Composable
fun RealTimeTab(
    currentSpeed: BandwidthMonitor.BandwidthSnapshot?,
    snapshots: List<BandwidthMonitor.BandwidthSnapshot>,
    showDownload: Boolean,
    showUpload: Boolean,
    showSpeedometer: Boolean,
    peakDownload: Long,
    peakUpload: Long
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Speed Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpeedCard(
                label = "Download",
                icon = Icons.Outlined.ArrowDownward,
                speed = currentSpeed?.downloadFormatted ?: "0 B/s",
                peak = "Peak: ${BandwidthMonitor().formatSpeed(peakDownload)}",
                color = TealPrimary,
                modifier = Modifier.weight(1f)
            )
            SpeedCard(
                label = "Upload",
                icon = Icons.Outlined.ArrowUpward,
                speed = currentSpeed?.uploadFormatted ?: "0 B/s",
                peak = "Peak: ${BandwidthMonitor().formatSpeed(peakUpload)}",
                color = Color(0xFF0288D1),
                modifier = Modifier.weight(1f)
            )
        }

        // Live Graph
        BandwidthGraph(snapshots, showDownload, showUpload)

        // Stats Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalDown = currentSpeed?.totalDownloadMb ?: 0f
            val totalUp = currentSpeed?.totalUploadMb ?: 0f
            StatChip("↓ Total: ${"%.1f".format(totalDown)} MB")
            StatChip("↑ Total: ${"%.1f".format(totalUp)} MB")
        }

        // Speed Meter
        if (showSpeedometer) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                SpeedMeter(
                    currentSpeed?.downloadBytesPerSec ?: 0L,
                    currentSpeed?.downloadFormatted ?: "0 B/s"
                )
            }
        }
    }
}

@Composable
fun SpeedCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, speed: String, peak: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    label,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                speed,
                color = Color(0xFFE6EDF3), // Fix 1: explicit bright color
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoType
            )
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = Color(0xFF30363D))
            Spacer(modifier = Modifier.height(4.dp))
            Text(peak, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun BandwidthGraph(snapshots: List<BandwidthMonitor.BandwidthSnapshot>, showDownload: Boolean, showUpload: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val width = size.width
            val height = size.height
            val maxSpeed = (snapshots.maxOfOrNull { maxOf(it.downloadBytesPerSec, it.uploadBytesPerSec) } ?: 1024L).coerceAtLeast(1024L)

            // Grid Lines
            val gridColor = Color.Gray.copy(alpha = 0.2f)
            for (i in 1..3) {
                val y = height * (i / 4f)
                drawLine(gridColor, Offset(0f, y), Offset(width, y))
            }
            for (i in 0..6) {
                val x = width * (i / 6f)
                drawLine(gridColor, Offset(x, 0f), Offset(x, height))
            }

            if (snapshots.size > 1) {
                val downPath = Path()
                val upPath = Path()

                snapshots.forEachIndexed { index, snapshot ->
                    val x = width * (index.toFloat() / 60f)
                    val dy = height - (snapshot.downloadBytesPerSec.toFloat() / maxSpeed * height)
                    val uy = height - (snapshot.uploadBytesPerSec.toFloat() / maxSpeed * height)

                    if (index == 0) {
                        downPath.moveTo(x, dy)
                        upPath.moveTo(x, uy)
                    } else {
                        downPath.lineTo(x, dy)
                        upPath.lineTo(x, uy)
                    }
                }

                if (showDownload) drawPath(downPath, TealPrimary, style = Stroke(width = 2.dp.toPx()))
                if (showUpload) drawPath(upPath, Color(0xFF0288D1), style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

@Composable
fun SpeedMeter(speedBytes: Long, speedFormatted: String) {
    val maxSpeed = 100 * 1024 * 1024L // 100 MB/s for scale
    val percentage = (speedBytes.toFloat() / maxSpeed).coerceIn(0f, 1f)
    val animatedPercentage = remember { Animatable(percentage) }
    
    LaunchedEffect(percentage) {
        animatedPercentage.animateTo(percentage)
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp).padding(16.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Gauge background
            drawArc(
                color = Color(0xFF21262D),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                topLeft = Offset.Zero,
                size = size
            )

            // Gauge progress
            drawArc(
                color = Color(0xFF00BFA5),
                startAngle = 150f,
                sweepAngle = 240f * animatedPercentage.value,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                topLeft = Offset.Zero,
                size = size
            )

            // Needle
            val needleAngle = 150f + (240f * animatedPercentage.value)
            val angleRad = Math.toRadians(needleAngle.toDouble())
            val needleLength = radius * 0.8f
            val needleEnd = Offset(
                center.x + (Math.cos(angleRad) * needleLength).toFloat(),
                center.y + (Math.sin(angleRad) * needleLength).toFloat()
            )

            drawLine(
                color = Color(0xFF00BFA5),
                start = center,
                end = needleEnd,
                strokeWidth = 4.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            drawCircle(
                color = Color(0xFF00BFA5),
                radius = 6.dp.toPx(),
                center = center
            )
        }

        // Fix 2: Proper Column separation for speedometer text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Text(
                text = speedFormatted,
                color = Color(0xFFE6EDF3),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoType
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Download Speed",
                color = Color(0xFF8B949E),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun StatChip(label: String) {
    Surface(
        color = Color(0xFF161B22),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(vertical = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00BFA5))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
            fontFamily = MonoType
        )
    }
}

@Composable
fun AppUsageTab(viewModel: BandwidthViewModel, appUsage: List<BandwidthViewModel.AppNetworkUsage>) {
    val context = LocalContext.current
    // Fix 3: Check usage stats permission
    val hasPermission = remember { viewModel.hasUsageStatsPermission(context) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadAppUsageStats(context)
        }
    }

    if (!hasPermission) {
        // Show permission request card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = Color(0xFF00BFA5),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Usage Access Required",
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "To see network usage for ALL apps (not just Network Auditor), grant Usage Access permission in system settings.",
                color = Color(0xFF8B949E),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00BFA5),
                    contentColor = Color(0xFF003D36)
                )
            ) {
                Text("Open Settings", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Find 'Network Auditor' in the list and toggle it ON",
                color = Color(0xFF8B949E),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(appUsage) { app ->
                AppUsageRow(app)
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun AppUsageRow(app: BandwidthViewModel.AppNetworkUsage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple placeholder for icon as we don't have coil/glide here easily in snippet
        Box(modifier = Modifier.size(40.dp).padding(4.dp), contentAlignment = Alignment.Center) {
            Surface(shape = MaterialTheme.shapes.small, color = TealPrimary.copy(alpha = 0.2f)) {
                Text(app.appName.take(1), color = TealPrimary, modifier = Modifier.padding(8.dp))
            }
        }
        
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = 0.5f, // Simplified
                    modifier = Modifier.weight(1f).height(4.dp),
                    color = TealPrimary
                )
                LinearProgressIndicator(
                    progress = 0.2f, // Simplified
                    modifier = Modifier.weight(1f).height(4.dp),
                    color = Color(0xFF0288D1)
                )
            }
            Text(
                "↓ ${formatBytes(app.downloadBytes)}  ↑ ${formatBytes(app.uploadBytes)}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = MonoType,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1048576 -> "${"%.1f".format(bytes/1024f)} KB"
        else -> "${"%.1f".format(bytes/1048576f)} MB"
    }
}
