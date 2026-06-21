package com.securenet.auditor.domain.model

data class CombinedEmailCheckResult(
    val email: String,
    val domain: String?,
    val isDisposable: Boolean,
    val isFormatValid: Boolean,
    val hasDns: Boolean,
    val hasMx: Boolean,
    val isAlias: Boolean
)
