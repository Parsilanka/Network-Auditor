package com.securenet.auditor.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.AppContainer
import com.securenet.auditor.data.db.PasswordEntity
import com.securenet.auditor.data.repository.OsintRepository
import com.securenet.auditor.data.repository.PasswordRepository
import com.securenet.auditor.domain.model.OsintResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PasswordViewModel(
    private val passwordRepository: PasswordRepository,
    private val osintRepository: OsintRepository
) : ViewModel() {

    val allPasswords = passwordRepository.allPasswords

    private val _breachResult = MutableStateFlow<OsintResult<Int>>(OsintResult.Idle)
    val breachResult: StateFlow<OsintResult<Int>> = _breachResult.asStateFlow()

    fun addPassword(password: PasswordEntity) {
        viewModelScope.launch {
            passwordRepository.insert(password)
        }
    }

    fun deletePassword(password: PasswordEntity) {
        viewModelScope.launch {
            passwordRepository.delete(password)
        }
    }

    fun checkBreach(password: String) {
        viewModelScope.launch {
            _breachResult.value = OsintResult.Loading
            _breachResult.value = osintRepository.checkPasswordBreach(password)
        }
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PasswordViewModel(
                    container.passwordRepository,
                    container.osintRepository
                ) as T
            }
        }
    }
}
