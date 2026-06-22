package com.securenet.auditor.ui.tools

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

enum class HashAlgorithm(val algorithm: String) {
    MD5("MD5"),
    SHA1("SHA-1"),
    SHA256("SHA-256"),
    SHA512("SHA-512")
}

data class HashResult(
    val algorithm: HashAlgorithm,
    val hash: String,
    val isMatch: Boolean? = null
)

class HashViewModel : ViewModel() {

    private val _hashResult = MutableStateFlow<HashResult?>(null)
    val hashResult: StateFlow<HashResult?> = _hashResult.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun generateTextHash(text: String, algorithm: HashAlgorithm, compareHash: String? = null) {
        _isProcessing.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val hash = hashText(text, algorithm)
            val isMatch = compareHash?.let { it.trim().equals(hash, ignoreCase = true) }
            _hashResult.value = HashResult(algorithm, hash, isMatch)
            _isProcessing.value = false
        }
    }

    fun generateFileHash(context: Context, uri: Uri, algorithm: HashAlgorithm, compareHash: String? = null) {
        _isProcessing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hash = hashFile(context, uri, algorithm)
                val isMatch = compareHash?.let { it.trim().equals(hash, ignoreCase = true) }
                _hashResult.value = HashResult(algorithm, hash, isMatch)
            } catch (e: Exception) {
                _hashResult.value = null // Handle error UI if needed
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun hashText(text: String, algorithm: HashAlgorithm): String {
        val digest = MessageDigest.getInstance(algorithm.algorithm)
        val bytes = digest.digest(text.toByteArray())
        return bytes.toHexString()
    }

    private suspend fun hashFile(context: Context, uri: Uri, algorithm: HashAlgorithm): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance(algorithm.algorithm)
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().toHexString()
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }

    fun clearResult() {
        _hashResult.value = null
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HashViewModel() as T
            }
        }
    }
}
