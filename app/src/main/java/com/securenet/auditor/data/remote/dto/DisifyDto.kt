package com.securenet.auditor.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DisifyDto(
    @SerializedName("format") val format: Boolean,
    @SerializedName("domain") val domain: String?,
    @SerializedName("disposable") val disposable: Boolean,
    @SerializedName("dns") val dns: Boolean
)
