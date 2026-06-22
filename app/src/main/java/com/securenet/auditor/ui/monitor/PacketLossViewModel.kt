package com.securenet.auditor.ui.monitor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PacketLossPoint(
    val timestamp: Long,
    val rtt: Long
)

class PacketLossViewModel(private val context: Context) : ViewModel() {

    private val _history = MutableStateFlow<List<PacketLossPoint>>(emptyList())
    val history: StateFlow<List<PacketLossPoint>> = _history

    init {
        loadLogs()
    }

    fun loadLogs() {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("packet_loss_logs", Context.MODE_PRIVATE)
            val logs = prefs.getStringSet("logs", emptySet()) ?: emptySet()
            val points = logs.mapNotNull { 
                try {
                    val parts = it.split("|")
                    PacketLossPoint(parts[0].toLong(), parts[1].toLong())
                } catch (e: Exception) { null }
            }.sortedBy { it.timestamp }
            _history.value = points
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PacketLossViewModel(context) as T
            }
        }
    }
}
