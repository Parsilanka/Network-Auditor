package com.securenet.auditor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.securenet.auditor.R

// Using system Monospace since font resource is missing
val MonoType = FontFamily.Monospace

val MonoStyle = TextStyle(
    fontFamily = MonoType,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
