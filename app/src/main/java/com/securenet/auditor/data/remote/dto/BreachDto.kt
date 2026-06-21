package com.securenet.auditor.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BreachDto(
    @SerializedName("Name") val name: String,
    @SerializedName("Domain") val domain: String,
    @SerializedName("BreachDate") val breachDate: String,
    @SerializedName("PwnCount") val pwnCount: Long,
    @SerializedName("Description") val description: String
)
