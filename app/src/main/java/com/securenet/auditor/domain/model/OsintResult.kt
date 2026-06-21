package com.securenet.auditor.domain.model

sealed class OsintResult<out T> {
    object Idle : OsintResult<Nothing>()
    object Loading : OsintResult<Nothing>()
    data class Found<T>(val data: T) : OsintResult<T>()
    object NotFound : OsintResult<Nothing>()
    data class Error(val message: String) : OsintResult<Nothing>()
}
