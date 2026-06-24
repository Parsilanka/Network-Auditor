package com.securenet.auditor.ui.tools

data class PasswordStrengthResult(
    val score: Int,
    val label: String,
    val feedback: String
)

fun analyzePasswordStrength(password: String): PasswordStrengthResult {
    var score = 0
    val feedback = mutableListOf<String>()

    if (password.length >= 8) score += 25 else feedback.add("Use at least 8 characters")
    if (password.any { it.isUpperCase() }) score += 15 else feedback.add("Add uppercase letters")
    if (password.any { it.isLowerCase() }) score += 15 else feedback.add("Add lowercase letters")
    if (password.any { it.isDigit() }) score += 20 else feedback.add("Add numbers")
    if (password.any { !it.isLetterOrDigit() }) score += 25 else feedback.add("Add symbols")

    val label = when {
        score >= 80 -> "Strong"
        score >= 60 -> "Good"
        score >= 40 -> "Fair"
        else -> "Weak"
    }

    return PasswordStrengthResult(
        score = score.coerceIn(0, 100),
        label = label,
        feedback = feedback.joinToString(" • ")
    )
}
