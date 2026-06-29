package com.securenet.auditor.ui.ssh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.data.repository.PasswordRepository
import com.securenet.auditor.network.SshClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SshTerminalViewModel(
    private val client: SshClient,
    private val passwordRepository: PasswordRepository
) : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _terminalOutput = MutableStateFlow<List<TerminalLine>>(
        listOf(TerminalLine("SecureNet SSH Terminal v1.1", TerminalLineType.SYSTEM))
    )
    val terminalOutput: StateFlow<List<TerminalLine>> = _terminalOutput.asStateFlow()

    private val _host = MutableStateFlow("")
    val host: StateFlow<String> = _host.asStateFlow()

    private val _user = MutableStateFlow("root")
    val user: StateFlow<String> = _user.asStateFlow()

    val savedKeys = passwordRepository.allPasswords

    data class TerminalLine(
        val text: String,
        val type: TerminalLineType
    )

    enum class TerminalLineType { INPUT, OUTPUT, ERROR, SYSTEM }

    fun setHost(h: String) { _host.value = h }
    fun setUser(u: String) { _user.value = u }

    fun connect(host: String, user: String, pass: String? = null, keyId: Int? = null) {
        viewModelScope.launch {
            _isConnecting.value = true
            addOutputLine("Connecting to $user@$host...", TerminalLineType.SYSTEM)
            
            var privateKey: String? = null
            if (keyId != null) {
                val keys = savedKeys.first()
                privateKey = keys.find { it.id == keyId }?.encryptedPassword
                addOutputLine("Using key from Vault...", TerminalLineType.SYSTEM)
            }

            val success = client.connect(host, user, password = pass, privateKey = privateKey)
            if (success) {
                _isConnected.value = true
                addOutputLine("Connected successfully.", TerminalLineType.SYSTEM)
            } else {
                addOutputLine("Connection failed. Check credentials or host.", TerminalLineType.ERROR)
            }
            _isConnecting.value = false
        }
    }

    fun sendCommand(command: String) {
        if (command.isBlank()) return
        
        viewModelScope.launch {
            addOutputLine("${_user.value}@${_host.value}:~$ $command", TerminalLineType.INPUT)
            
            val result = client.executeCommand(command)
            if (result.error != null) {
                addOutputLine(result.error, TerminalLineType.ERROR)
            } else {
                addOutputLine(result.output, TerminalLineType.OUTPUT)
            }
        }
    }

    private fun addOutputLine(text: String, type: TerminalLineType) {
        val current = _terminalOutput.value.toMutableList()
        current.add(TerminalLine(text, type))
        if (current.size > 200) current.removeAt(0)
        _terminalOutput.value = current
    }

    fun disconnect() {
        client.disconnect()
        _isConnected.value = false
        addOutputLine("Disconnected.", TerminalLineType.SYSTEM)
    }

    companion object {
        fun provideFactory(container: com.securenet.auditor.AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SshTerminalViewModel(
                    SshClient(),
                    container.passwordRepository
                ) as T
            }
        }
    }
}
