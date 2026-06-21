package com.securenet.auditor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securenet.auditor.domain.model.HostInfo
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HostCard(host: HostInfo, onPortScan: (HostInfo) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = host.ipAddress,
                        fontFamily = MonoType,
                        color = TealPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = host.hostname ?: "Unresolved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${host.responseTimeMs}ms",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = "Expand"
                    )
                }
            }

            if (host.vendor != null) {
                AssistChip(
                    onClick = {},
                    label = { Text(host.vendor, fontSize = 10.sp) },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (host.openPorts.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    host.openPorts.forEach { port ->
                        PortChip(port)
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    if (host.openPorts.isNotEmpty()) {
                        Text("Detected Services:", style = MaterialTheme.typography.labelMedium)
                        host.openPorts.forEach { port ->
                            Text(
                                text = "$port: ${getServiceName(port)}",
                                fontFamily = MonoType,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "No open ports detected",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onPortScan(host) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Scan Ports")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortChip(port: Int) {
    val color = when (port) {
        80, 8080 -> Color(0xFF2196F3) // Blue
        443, 8443 -> Color(0xFF4CAF50) // Green
        22 -> Color(0xFFFFC107) // Amber
        3306, 5432 -> Color(0xFFF44336) // Red
        21, 23 -> Color(0xFFFF9800) // Orange
        else -> Color.Gray
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = color, borderWidth = 1.dp)
    ) {
        Text(
            text = port.toString(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontFamily = MonoType,
            color = color
        )
    }
}

fun getServiceName(port: Int): String = when (port) {
    21 -> "FTP"
    22 -> "SSH"
    23 -> "Telnet"
    25 -> "SMTP"
    53 -> "DNS"
    80 -> "HTTP"
    110 -> "POP3"
    143 -> "IMAP"
    443 -> "HTTPS"
    445 -> "SMB"
    3306 -> "MySQL"
    3389 -> "RDP"
    5900 -> "VNC"
    8080 -> "HTTP-Alt"
    8443 -> "HTTPS-Alt"
    else -> "Unknown"
}
