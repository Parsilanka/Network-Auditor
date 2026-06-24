package com.securenet.auditor.ui.tools

import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordStrengthUtilsTest {
    @Test
    fun weakPasswordIsRatedAsWeak() {
        val result = analyzePasswordStrength("abc")
        assertTrue(result.label.contains("Weak", ignoreCase = true))
        assertTrue(result.score < 40)
    }

    @Test
    fun strongPasswordIsRatedAsStrong() {
        val result = analyzePasswordStrength("SecureNet2026!")
        assertTrue(result.label.contains("Strong", ignoreCase = true))
        assertTrue(result.score >= 80)
    }
}
