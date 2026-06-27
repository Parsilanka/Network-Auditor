package com.securenet.auditor.ui.subdomain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.network.SubdomainEnumerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubdomainViewModel(private val enumerator: SubdomainEnumerator) : ViewModel() {

    private val _results = MutableStateFlow<List<SubdomainEnumerator.SubdomainResult>>(emptyList())
    val results: StateFlow<List<SubdomainEnumerator.SubdomainResult>> = _results.asStateFlow()

    private val _isEnumerating = MutableStateFlow(false)
    val isEnumerating: StateFlow<Boolean> = _isEnumerating.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentChecking = MutableStateFlow("")
    val currentChecking: StateFlow<String> = _currentChecking.asStateFlow()

    private var enumJob: Job? = null

    fun startEnumeration(domain: String) {
        stopEnumeration()
        _results.value = emptyList()
        _isEnumerating.value = true
        _progress.value = 0f
        
        enumJob = viewModelScope.launch {
            enumerator.enumerate(
                domain = domain,
                onProgress = { current, total, checking ->
                    _progress.value = current.toFloat() / total
                    _currentChecking.value = checking
                },
                onFound = { result ->
                    _results.value = _results.value + result
                }
            ).collect {
                if (_progress.value >= 1f) {
                    _isEnumerating.value = false
                }
            }
            _isEnumerating.value = false
        }
    }

    fun stopEnumeration() {
        enumJob?.cancel()
        _isEnumerating.value = false
    }

    companion object {
        fun provideFactory(enumerator: SubdomainEnumerator): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SubdomainViewModel(enumerator) as T
            }
        }
    }
}
