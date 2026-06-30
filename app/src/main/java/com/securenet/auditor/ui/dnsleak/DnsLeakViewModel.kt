package com.securenet.auditor.ui.dnsleak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.AppContainer
import com.securenet.auditor.network.DnsLeakTester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DnsLeakViewModel(private val tester: DnsLeakTester) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _result = MutableStateFlow<DnsLeakTester.LeakResult?>(null)
    val result: StateFlow<DnsLeakTester.LeakResult?> = _result.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun runTest() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _result.value = null
            
            val res = tester.runTest()
            if (res != null) {
                _result.value = res
            } else {
                _error.value = "Failed to run DNS leak test. Check your internet connection."
            }
            _isLoading.value = false
        }
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DnsLeakViewModel(container.dnsLeakTester) as T
            }
        }
    }
}
