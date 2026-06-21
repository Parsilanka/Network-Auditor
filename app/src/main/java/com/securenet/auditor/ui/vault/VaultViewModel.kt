package com.securenet.auditor.ui.vault

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.AppContainer
import com.securenet.auditor.data.db.ScanResultEntity
import com.securenet.auditor.data.repository.ScanRepository
import com.securenet.auditor.security.BiometricHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VaultViewModel(
    private val repository: ScanRepository,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    private val _scanHistory = MutableStateFlow<List<ScanResultEntity>>(emptyList())
    val scanHistory: StateFlow<List<ScanResultEntity>> = _scanHistory.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    fun authenticate(activity: FragmentActivity) {
        _isAuthenticating.value = true
        biometricHelper.authenticate(
            activity = activity,
            onSuccess = {
                _isAuthenticated.value = true
                _isAuthenticating.value = false
                collectHistory()
            },
            onFailure = {
                _isAuthenticated.value = false
                _isAuthenticating.value = false
            },
            onError = {
                _isAuthenticated.value = false
                _isAuthenticating.value = false
            }
        )
    }

    private fun collectHistory() {
        viewModelScope.launch {
            repository.getAllScansFlow().collectLatest {
                _scanHistory.value = it
            }
        }
    }

    fun lockVault() {
        _isAuthenticated.value = false
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun updateTag(id: Long, tag: String) {
        viewModelScope.launch {
            repository.updateTag(id, tag)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VaultViewModel(
                    container.scanRepository,
                    container.biometricHelper
                ) as T
            }
        }
    }
}
