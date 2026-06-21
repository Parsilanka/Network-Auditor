package com.securenet.auditor.domain.model

sealed class ScanProgress {
    object Idle : ScanProgress()
    data class Scanning(val current: Int, val total: Int, val currentIp: String) : ScanProgress()
    data class HostFound(val host: HostInfo) : ScanProgress()
    data class Complete(val results: List<HostInfo>, val durationMs: Long) : ScanProgress()
    data class Error(val message: String) : ScanProgress()
}
