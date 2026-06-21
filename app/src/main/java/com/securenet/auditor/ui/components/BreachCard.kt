package com.securenet.auditor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securenet.auditor.domain.model.BreachResult
import com.securenet.auditor.domain.model.BreachSeverity
import com.securenet.auditor.ui.theme.*

@Composable
fun BreachCard(breach: BreachResult) {
    var expanded by remember { mutableStateOf(false) }
    
    val severityColor = when (breach.severity) {
        BreachSeverity.LOW -> SuccessGreen
        BreachSeverity.MEDIUM -> WarningAmber
        BreachSeverity.HIGH -> Color(0xFFFF5722)
        BreachSeverity.CRITICAL -> ErrorRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = 1.dp,
                color = severityColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left colored border
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .border(width = 0.dp, color = Color.Transparent)
                    .padding(0.dp)
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = severityColor) {}
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = breach.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = severityColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = breach.severity.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = severityColor
                        )
                    }
                }
                
                Text(
                    text = breach.domain,
                    fontFamily = MonoType,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Date: ${breach.breachDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%,d accounts", breach.pwnCount),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    val cleanDescription = remember(breach.description) {
                        breach.description.replace(Regex("<[^>]*>"), "")
                    }
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cleanDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
