package com.securenet.auditor.domain.model

data class BreachResult(
    val name: String,
    val domain: String,
    val breachDate: String,
    val pwnCount: Long,
    val description: String,
    val severity: BreachSeverity
)

enum class BreachSeverity { LOW, MEDIUM, HIGH, CRITICAL }
