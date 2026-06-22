package com.securenet.auditor.ui.tools

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.data.db.SpeedTestEntity
import com.securenet.auditor.ui.theme.SuccessGreen
import com.securenet.auditor.ui.theme.TealPrimary
import com.securenet.auditor.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Internet Speed Test", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state is SpeedTestState.Complete) {
                        IconButton(onClick = {
                            val result = (state as SpeedTestState.Complete).result
                            val shareText = "My Network Speed Test Results:\n" +
                                    "Download: ${"%.2f".format(result.downloadMbps)} Mbps\n" +
                                    "Upload: ${"%.2f".format(result.uploadMbps)} Mbps\n" +
                                    "Ping: ${result.pingMs} ms\n" +
                                    "Tested via Securenet Auditor"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Results"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SpeedGaugesRow(state)
            }

            item {
                StartTestButton(
                    state = state,
                    onClick = { viewModel.startSpeedTest() }
                )
            }

            if (state is SpeedTestState.Complete) {
                item {
                    ResultsSummaryCard((state as SpeedTestState.Complete).result)
                }
            }

            if (state is SpeedTestState.Error) {
                item {
                    Text(
                        (state as SpeedTestState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (history.isNotEmpty()) {
                item {
                    HistorySection(history)
                }
            }
        }
    }
}

@Composable
fun SpeedGaugesRow(state: SpeedTestState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val ping = when (state) {
            is SpeedTestState.TestingPing -> state.pingMs.toDouble()
            is SpeedTestState.Complete -> state.result.pingMs.toDouble()
            else -> 0.0
        }
        val download = when (state) {
            is SpeedTestState.TestingDownload -> state.currentMbps
            is SpeedTestState.TestingUpload -> (state as? SpeedTestState.Complete)?.result?.downloadMbps ?: 0.0
            is SpeedTestState.Complete -> state.result.downloadMbps
            else -> 0.0
        }
        val upload = when (state) {
            is SpeedTestState.TestingUpload -> state.currentMbps
            is SpeedTestState.Complete -> state.result.uploadMbps
            else -> 0.0
        }

        SpeedGauge("PING", ping, "ms", max = 200.0, state = state)
        SpeedGauge("DOWNLOAD", download, "Mbps", max = 200.0, state = state)
        SpeedGauge("UPLOAD", upload, "Mbps", max = 200.0, state = state)
    }
}

@Composable
fun SpeedGauge(label: String, value: Double, unit: String, max: Double, state: SpeedTestState) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
            val sweepAngle = (animatedValue / max.toFloat() * 240f).coerceIn(0f, 240f)
            val arcColor = when {
                unit == "ms" -> if (value < 50) SuccessGreen else if (value < 100) WarningAmber else Color.Red
                value < 10 -> Color.Red
                value < 50 -> WarningAmber
                value < 100 -> TealPrimary
                else -> SuccessGreen
            }

            Canvas(modifier = Modifier.size(80.dp)) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = arcColor,
                    startAngle = 150f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (unit == "ms") value.toInt().toString() else "%.1f".format(value),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(text = unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StartTestButton(state: SpeedTestState, onClick: () -> Unit) {
    val isRunning = state !is SpeedTestState.Idle && state !is SpeedTestState.Complete && state !is SpeedTestState.Error
    
    Button(
        onClick = onClick,
        modifier = Modifier.size(140.dp),
        shape = CircleShape,
        enabled = !isRunning,
        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isRunning) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (state) {
                        is SpeedTestState.TestingPing -> "PINGING"
                        is SpeedTestState.TestingDownload -> "DOWNLOADING"
                        is SpeedTestState.TestingUpload -> "UPLOADING"
                        else -> "RUNNING"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = if (state is SpeedTestState.Complete) "RESTART" else "START",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ResultsSummaryCard(result: SpeedTestResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Performance Summary", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            val rating = when {
                result.downloadMbps < 5 -> "Slow — streaming may buffer"
                result.downloadMbps < 25 -> "Fair — basic use fine"
                result.downloadMbps < 100 -> "Good — HD streaming supported"
                else -> "Excellent — 4K streaming ready"
            }
            
            Text(rating, style = MaterialTheme.typography.bodyLarge, color = TealPrimary, fontWeight = FontWeight.Medium)
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ResultSmallItem("Jitter", "${result.jitterMs.toInt()}ms")
                ResultSmallItem("Server", result.testServer)
                ResultSmallItem("Time", SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(result.timestamp)))
            }
        }
    }
}

@Composable
fun ResultSmallItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HistorySection(history: List<SpeedTestEntity>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mini Line Chart
        Card(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            Box(modifier = Modifier.padding(8.dp)) {
                HistoryLineChart(history.take(10).reversed())
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        history.forEach { item ->
            HistoryRow(item)
            Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
        }
    }
}

@Composable
fun HistoryLineChart(data: List<SpeedTestEntity>) {
    if (data.size < 2) return
    
    val maxSpeed = data.maxOf { it.downloadMbps }.toFloat().coerceAtLeast(10f)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)
        
        val path = Path().apply {
            data.forEachIndexed { index, entity ->
                val x = index * stepX
                val y = height - (entity.downloadMbps.toFloat() / maxSpeed * height)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = TealPrimary,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun HistoryRow(item: SpeedTestEntity) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                style = MaterialTheme.typography.labelSmall
            )
            Text(text = item.testServer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "DL: ${item.downloadMbps.toInt()} Mbps", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text(text = "UL: ${item.uploadMbps.toInt()} Mbps", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
