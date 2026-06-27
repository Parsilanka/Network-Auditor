package com.securenet.auditor.ui.bandwidth

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
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

    var selectedTab by remember { mutableStateOf(0) }

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
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                RealTimeTab(currentSpeed, snapshots)
            } else {
                AppUsageTab(viewModel, appUsage)
            }
        }
    }
}

@Composable
fun RealTimeTab(
    currentSpeed: BandwidthMonitor.BandwidthSnapshot?,
    snapshots: List<BandwidthMonitor.BandwidthSnapshot>
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
                label = "↓ Download",
                speed = currentSpeed?.downloadFormatted ?: "0 B/s",
                peak = "Peak: ${snapshots.maxOfOrNull { it.downloadBytesPerSec }?.let { BandwidthMonitor().formatSpeed(it) } ?: "0 B/s"}",
                color = TealPrimary,
                modifier = Modifier.weight(1f)
            )
            SpeedCard(
                label = "↑ Upload",
                speed = currentSpeed?.uploadFormatted ?: "0 B/s",
                peak = "Peak: ${snapshots.maxOfOrNull { it.uploadBytesPerSec }?.let { BandwidthMonitor().formatSpeed(it) } ?: "0 B/s"}",
                color = Color(0xFF0288D1),
                modifier = Modifier.weight(1f)
            )
        }

        // Live Graph
        BandwidthGraph(snapshots)

        // Stats Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalDown = currentSpeed?.totalDownloadMb ?: 0f
            val totalUp = currentSpeed?.totalUploadMb ?: 0f
            StatChip("Total ↓: ${"%.1f".format(totalDown)} MB")
            StatChip("Total ↑: ${"%.1f".format(totalUp)} MB")
        }

        // Speed Meter
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            SpeedMeter(currentSpeed?.downloadBytesPerSec ?: 0L)
        }
    }
}

@Composable
fun SpeedCard(label: String, speed: String, peak: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(speed, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, fontFamily = MonoType)
            Divider(modifier = Modifier.padding(vertical = 4.dp), color = color.copy(alpha = 0.3f))
            Text(peak, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun BandwidthGraph(snapshots: List<BandwidthMonitor.BandwidthSnapshot>) {
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

                drawPath(downPath, TealPrimary, style = Stroke(width = 2.dp.toPx()))
                drawPath(upPath, Color(0xFF0288D1), style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

@Composable
fun SpeedMeter(speedBytes: Long) {
    val maxSpeed = 100 * 1024 * 1024L // 100 MB/s for scale
    val percentage = (speedBytes.toFloat() / maxSpeed).coerceIn(0f, 1f)
    val animatedPercentage = remember { Animatable(0f) }
    
    LaunchedEffect(percentage) {
        animatedPercentage.animateTo(percentage)
    }

    Canvas(modifier = Modifier.size(180.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2

        // Gauge background
        drawArc(
            color = Color.Gray.copy(alpha = 0.2f),
            startAngle = 150f,
            sweepAngle = 240f,
            useCenter = false,
            style = Stroke(width = 12.dp.toPx()),
            topLeft = Offset.Zero,
            size = size
        )

        // Gauge progress
        val color = when {
            animatedPercentage.value < 0.25f -> Color(0xFF4CAF50)
            animatedPercentage.value < 0.75f -> TealPrimary
            animatedPercentage.value < 1.0f -> Color(0xFFFFC107)
            else -> Color.Red
        }

        drawArc(
            color = color,
            startAngle = 150f,
            sweepAngle = 240f * animatedPercentage.value,
            useCenter = false,
            style = Stroke(width = 12.dp.toPx()),
            topLeft = Offset.Zero,
            size = size
        )
    }
}

@Composable
fun StatChip(label: String) {
    Surface(
        color = Color(0xFF161B22),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = MonoType
        )
    }
}

@Composable
fun AppUsageTab(viewModel: BandwidthViewModel, appUsage: List<BandwidthViewModel.AppNetworkUsage>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadAppUsageStats(context)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(appUsage) { app ->
            AppUsageRow(app)
            Divider(color = Color.Gray.copy(alpha = 0.1f))
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
