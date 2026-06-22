package com.securenet.auditor.ui.vault

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.AppContainer
import com.securenet.auditor.data.db.ScanResultEntity
import com.securenet.auditor.data.repository.ScanRepository
import com.securenet.auditor.security.BiometricHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class VaultFilter { ALL, TAGGED, UNTAGGED, TODAY, THIS_WEEK }

class VaultViewModel(
    private val repository: ScanRepository,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    private val _scanHistory = MutableStateFlow<List<ScanResultEntity>>(emptyList())
    val scanHistory: StateFlow<List<ScanResultEntity>> = _scanHistory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(VaultFilter.ALL)
    val activeFilter: StateFlow<VaultFilter> = _activeFilter.asStateFlow()

    val filteredHistory = combine(_scanHistory, _searchQuery, _activeFilter) { history, query, filter ->
        history.filter { scan ->
            val matchesQuery = query.isEmpty() || 
                scan.ipAddress.contains(query, ignoreCase = true) ||
                (scan.tag?.contains(query, ignoreCase = true) == true) ||
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(scan.timestamp)).contains(query)
            
            val matchesFilter = when (filter) {
                VaultFilter.ALL -> true
                VaultFilter.TAGGED -> !scan.tag.isNullOrBlank()
                VaultFilter.UNTAGGED -> scan.tag.isNullOrBlank()
                VaultFilter.TODAY -> isToday(scan.timestamp)
                VaultFilter.THIS_WEEK -> isThisWeek(scan.timestamp)
            }
            
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: VaultFilter) {
        _activeFilter.value = filter
    }

    private fun isToday(timestamp: Long): Boolean {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        cal.timeInMillis = timestamp
        return today == cal.get(Calendar.DAY_OF_YEAR) && year == cal.get(Calendar.YEAR)
    }

    private fun isThisWeek(timestamp: Long): Boolean {
        val cal = Calendar.getInstance()
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        cal.timeInMillis = timestamp
        return week == cal.get(Calendar.WEEK_OF_YEAR) && year == cal.get(Calendar.YEAR)
    }

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
