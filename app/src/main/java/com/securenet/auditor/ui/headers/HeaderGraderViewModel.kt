package com.securenet.auditor.ui.headers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.domain.model.OsintResult
import com.securenet.auditor.network.HttpHeaderAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HeaderGraderViewModel(private val analyzer: HttpHeaderAnalyzer) : ViewModel() {

    private val _result = MutableStateFlow<OsintResult<HttpHeaderAnalyzer.HeaderAnalysisResult>>(OsintResult.Idle)
    val result: StateFlow<OsintResult<HttpHeaderAnalyzer.HeaderAnalysisResult>> = _result.asStateFlow()

    fun analyze(url: String) {
        viewModelScope.launch {
            _result.value = OsintResult.Loading
            _result.value = analyzer.analyze(url)
        }
    }

    companion object {
        fun provideFactory(analyzer: HttpHeaderAnalyzer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HeaderGraderViewModel(analyzer) as T
            }
        }
    }
}
