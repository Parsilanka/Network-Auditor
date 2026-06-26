package com.securenet.auditor.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.data.repository.OsintRepository
import com.securenet.auditor.domain.model.OsintResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WhoisViewModel(private val repository: OsintRepository) : ViewModel() {

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun performWhois(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getWhois(query)
            when (response) {
                is OsintResult.Found -> _result.value = response.data
                is OsintResult.Error -> _result.value = "Error: ${response.message}"
                else -> _result.value = "No data found"
            }
            _isLoading.value = false
        }
    }

    companion object {
        fun provideFactory(repository: OsintRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WhoisViewModel(repository) as T
            }
        }
    }
}
