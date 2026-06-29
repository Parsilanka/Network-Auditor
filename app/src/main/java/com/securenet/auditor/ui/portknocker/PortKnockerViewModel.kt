package com.securenet.auditor.ui.portknocker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.network.PortKnocker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PortKnockerViewModel(private val knocker: PortKnocker) : ViewModel() {

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _lastResult = MutableStateFlow<PortKnocker.KnockResult?>(null)
    val lastResult: StateFlow<PortKnocker.KnockResult?> = _lastResult.asStateFlow()

    private val _sequence = MutableStateFlow<List<PortKnocker.KnockStep>>(
        listOf(
            PortKnocker.KnockStep(7000, PortKnocker.Protocol.TCP),
            PortKnocker.KnockStep(8000, PortKnocker.Protocol.TCP),
            PortKnocker.KnockStep(9000, PortKnocker.Protocol.TCP)
        )
    )
    val sequence: StateFlow<List<PortKnocker.KnockStep>> = _sequence.asStateFlow()

    fun addStep() {
        val current = _sequence.value.toMutableList()
        val lastPort = current.lastOrNull()?.port ?: 1000
        current.add(PortKnocker.KnockStep(lastPort + 1000, PortKnocker.Protocol.TCP))
        _sequence.value = current
    }

    fun removeStep(index: Int) {
        val current = _sequence.value.toMutableList()
        if (current.size > 1) {
            current.removeAt(index)
            _sequence.value = current
        }
    }

    fun updateStep(index: Int, step: PortKnocker.KnockStep) {
        val current = _sequence.value.toMutableList()
        current[index] = step
        _sequence.value = current
    }

    fun executeKnock(host: String) {
        if (host.isBlank()) return
        
        viewModelScope.launch {
            _isExecuting.value = true
            _lastResult.value = null
            val result = knocker.executeSequence(host, _sequence.value)
            _lastResult.value = result
            _isExecuting.value = false
        }
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PortKnockerViewModel(PortKnocker()) as T
            }
        }
    }
}
