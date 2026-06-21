package com.securenet.auditor.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MailCheckDto(
    @SerializedName("status") val status: Int,
    @SerializedName("email") val email: String?,
    @SerializedName("domain") val domain: String?,
    @SerializedName("mx") val mx: Boolean,
    @SerializedName("disposable") val disposable: Boolean,
    @SerializedName("alias") val alias: Boolean
)
