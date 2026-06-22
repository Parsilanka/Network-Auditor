package com.securenet.auditor.ui.tools

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.network.RogueApDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RogueApViewModel(context: Context) : ViewModel() {
    private val detector = RogueApDetector(context)

    private val _report = MutableStateFlow<RogueApDetector.RogueApReport?>(null)
    val report: StateFlow<RogueApDetector.RogueApReport?> = _report.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _report.value = detector.scanForRogueAps()
            _isScanning.value = false
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RogueApViewModel(context.applicationContext) as T
            }
        }
    }
}
