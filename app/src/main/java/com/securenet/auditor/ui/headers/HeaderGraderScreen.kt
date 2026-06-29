package com.securenet.auditor.ui.headers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.domain.model.OsintResult
import com.securenet.auditor.network.HttpHeaderAnalyzer
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderGraderScreen(
    viewModel: HeaderGraderViewModel,
    onBack: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    val result by viewModel.result.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("HTTP Security Headers", fontWeight = FontWeight.Bold)
                        Text("Website security grader", style = MaterialTheme.typography.labelSmall)
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
                value = url,
                onValueChange = { url = it },
                label = { Text("Enter URL (https://example.com)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.analyze(url) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                enabled = url.isNotBlank() && result !is OsintResult.Loading
            ) {
                if (result is OsintResult.Loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Analyze Headers")
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val res = result) {
                is OsintResult.Found -> {
                    val data = res.data
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item { OverallGradeCard(data) }
                        items(data.headers) { header ->
                            HeaderGradeRow(header)
                        }
                        item { MissingHeadersSection(data.headers) }
                    }
                }
                is OsintResult.Error -> Text(res.message, color = Color.Red)
                else -> {}
            }
        }
    }
}

@Composable
fun OverallGradeCard(data: HttpHeaderAnalyzer.HeaderAnalysisResult) {
    val gradeColor = when(data.overallGrade.take(1)) {
        "A" -> Color(0xFF4CAF50)
        "B" -> TealPrimary
        "C" -> Color(0xFFFFC107)
        "D" -> Color(0xFFFF9800)
        else -> Color.Red
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(80.dp).background(gradeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(data.overallGrade, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Score: ${data.overallScore} / 100", fontWeight = FontWeight.Bold)
            Text(data.url, style = MaterialTheme.typography.labelSmall, fontFamily = MonoType, color = Color.Gray)
            
            Surface(
                modifier = Modifier.padding(top = 8.dp),
                color = if (data.responseCode == 200) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    "HTTP ${data.responseCode}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (data.responseCode == 200) Color(0xFF4CAF50) else Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun HeaderGradeRow(header: HttpHeaderAnalyzer.HeaderGrade) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(header.headerName, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                val color = when(header.grade.take(1)) {
                    "A" -> Color(0xFF4CAF50)
                    "B" -> TealPrimary
                    "C" -> Color(0xFFFFC107)
                    "D" -> Color(0xFFFF9800)
                    else -> Color.Red
                }
                Surface(color = color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small) {
                    Text(header.grade, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
            
            LinearProgressIndicator(
                progress = header.score / 100f,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(4.dp),
                color = if (header.isPresent) TealPrimary else Color.Red,
                trackColor = Color.Gray.copy(alpha = 0.1f)
            )

            if (header.value != null) {
                Text(header.value, style = MaterialTheme.typography.labelSmall, fontFamily = MonoType, color = Color.Gray, maxLines = 1)
            }
            Text("↓ ${header.description}", style = MaterialTheme.typography.labelSmall, color = TealPrimary)

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider(color = Color.Gray.copy(alpha = 0.1f))
                    Text("Recommendation:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text(header.recommendation, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontStyle = FontStyle.Italic)
                    TextButton(onClick = { uriHandler.openUri(header.learnMoreUrl) }) {
                        Text("Learn More →", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MissingHeadersSection(headers: List<HttpHeaderAnalyzer.HeaderGrade>) {
    val missing = headers.filter { !it.isPresent }
    if (missing.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Missing Security Headers", fontWeight = FontWeight.Bold, color = TealPrimary)
        missing.forEach { header ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22).copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(header.headerName, fontWeight = FontWeight.Bold, color = Color.Red)
                    Text(header.recommendation, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
