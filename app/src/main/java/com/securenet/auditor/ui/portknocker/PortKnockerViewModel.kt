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

    private val _knockResults = MutableStateFlow<List<PortKnocker.KnockResult>>(emptyList())
    val knockResults: StateFlow<List<PortKnocker.KnockResult>> = _knockResults.asStateFlow()

    private val _sequence = MutableStateFlow<List<PortKnocker.UIKnockStep>>(
        listOf(
            PortKnocker.UIKnockStep("7000", "TCP"),
            PortKnocker.UIKnockStep("8000", "TCP"),
            PortKnocker.UIKnockStep("9000", "TCP")
        )
    )
    val sequence: StateFlow<List<PortKnocker.UIKnockStep>> = _sequence.asStateFlow()

    fun addStep() {
        val current = _sequence.value.toMutableList()
        val lastPort = current.lastOrNull()?.port?.toIntOrNull() ?: 1000
        current.add(PortKnocker.UIKnockStep((lastPort + 1000).toString(), "TCP"))
        _sequence.value = current
    }

    fun removeStep(index: Int) {
        val current = _sequence.value.toMutableList()
        if (current.size > 1) {
            current.removeAt(index)
            _sequence.value = current
        }
    }

    fun updateStep(index: Int, step: PortKnocker.UIKnockStep) {
        val current = _sequence.value.toMutableList()
        current[index] = step
        _sequence.value = current
    }

    fun executeKnockSequence(host: String) {
        if (host.isBlank()) return
        
        viewModelScope.launch {
            _isExecuting.value = true
            _knockResults.value = emptyList()
            
            knocker.executeKnockSequence(host, _sequence.value).collect { result ->
                val currentResults = _knockResults.value.toMutableList()
                currentResults.add(result)
                _knockResults.value = currentResults
            }

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
