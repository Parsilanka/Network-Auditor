package com.securenet.auditor.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HunterResponseDto(
    @SerializedName("data") val data: HunterData?
)

data class HunterData(
    @SerializedName("domain") val domain: String?,
    @SerializedName("organization") val organization: String?,
    @SerializedName("emails") val emails: List<HunterEmail>?
)

data class HunterEmail(
    @SerializedName("value") val value: String,
    @SerializedName("type") val type: String?,
    @SerializedName("confidence") val confidence: Int?
)
