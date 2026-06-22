package com.securenet.auditor.ui.osint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.AppContainer
import com.securenet.auditor.data.prefs.EncryptedPrefsManager
import com.securenet.auditor.data.remote.IpGeoDto
import com.securenet.auditor.data.remote.UrlHausResponse
import com.securenet.auditor.data.remote.dto.HunterResponseDto
import com.securenet.auditor.data.repository.OsintRepository
import com.securenet.auditor.domain.model.BreachResult
import com.securenet.auditor.domain.model.CombinedEmailCheckResult
import com.securenet.auditor.domain.model.OsintResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OsintViewModel(
    private val repository: OsintRepository,
    private val prefs: EncryptedPrefsManager
) : ViewModel() {

    private val _emailResult = MutableStateFlow<OsintResult<List<BreachResult>>>(OsintResult.Idle)
    val emailResult: StateFlow<OsintResult<List<BreachResult>>> = _emailResult.asStateFlow()

    private val _domainResult = MutableStateFlow<OsintResult<HunterResponseDto>>(OsintResult.Idle)
    val domainResult: StateFlow<OsintResult<HunterResponseDto>> = _domainResult.asStateFlow()

    private val _emailRepResult = MutableStateFlow<OsintResult<CombinedEmailCheckResult>>(OsintResult.Idle)
    val emailRepResult: StateFlow<OsintResult<CombinedEmailCheckResult>> = _emailRepResult.asStateFlow()

    private val _ipGeoResult = MutableStateFlow<OsintResult<IpGeoDto>>(OsintResult.Idle)
    val ipGeoResult: StateFlow<OsintResult<IpGeoDto>> = _ipGeoResult.asStateFlow()

    private val _whoisResult = MutableStateFlow<OsintResult<String>>(OsintResult.Idle)
    val whoisResult: StateFlow<OsintResult<String>> = _whoisResult.asStateFlow()

    private val _malwareResult = MutableStateFlow<OsintResult<UrlHausResponse>>(OsintResult.Idle)
    val malwareResult: StateFlow<OsintResult<UrlHausResponse>> = _malwareResult.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _hibpKeySet = MutableStateFlow(false)
    val hibpKeySet: StateFlow<Boolean> = _hibpKeySet.asStateFlow()

    private val _hunterKeySet = MutableStateFlow(false)
    val hunterKeySet: StateFlow<Boolean> = _hunterKeySet.asStateFlow()

    init {
        refreshKeyStatus()
    }

    fun checkEmail(email: String) {
        viewModelScope.launch {
            _emailResult.value = OsintResult.Loading
            _emailResult.value = repository.checkEmail(email)
        }
    }

    fun searchDomain(domain: String) {
        viewModelScope.launch {
            _domainResult.value = OsintResult.Loading
            _domainResult.value = repository.searchDomain(domain)
        }
    }

    fun checkEmailReputation(email: String) {
        viewModelScope.launch {
            _emailRepResult.value = OsintResult.Loading
            _emailRepResult.value = repository.checkEmailReputation(email)
        }
    }

    fun checkIpGeo(ip: String) {
        viewModelScope.launch {
            _ipGeoResult.value = OsintResult.Loading
            _ipGeoResult.value = repository.getIpGeo(ip)
        }
    }

    fun performWhois(query: String) {
        viewModelScope.launch {
            _whoisResult.value = OsintResult.Loading
            _whoisResult.value = repository.getWhois(query)
        }
    }

    fun checkMalware(url: String) {
        viewModelScope.launch {
            _malwareResult.value = OsintResult.Loading
            _malwareResult.value = repository.checkMalwareUrl(url)
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun saveApiKey(service: String, key: String) {
        prefs.saveApiKey(service, key)
        refreshKeyStatus()
    }

    fun clearApiKey(service: String) {
        prefs.clearApiKey(service)
        refreshKeyStatus()
    }

    fun refreshKeyStatus() {
        _hibpKeySet.value = prefs.hasApiKey(EncryptedPrefsManager.HIBP_KEY)
        _hunterKeySet.value = prefs.hasApiKey(EncryptedPrefsManager.HUNTER_KEY)
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OsintViewModel(
                    container.osintRepository,
                    container.encryptedPrefs
                ) as T
            }
        }
    }
}
