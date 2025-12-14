package com.atmiya.innovation.ui.dashboard.diagnosis

import com.google.gson.annotations.SerializedName

data class DiagnosisResponse(
    @SerializedName("missing") val missing: List<String> = emptyList(),
    @SerializedName("failure_points") val failurePoints: List<String> = emptyList(),
    @SerializedName("focus_next") val focusNext: List<String> = emptyList()
)
