package com.atmiya.innovation.data

import com.google.gson.annotations.SerializedName

@androidx.annotation.Keep
data class AppConfig(
    @SerializedName("min_version_code")
    @get:com.google.firebase.firestore.PropertyName("min_version_code")
    val minVersionCode: Int = 0,

    @SerializedName("latest_version_code")
    @get:com.google.firebase.firestore.PropertyName("latest_version_code")
    val latestVersionCode: Int = 0,

    @SerializedName("force_update_message")
    @get:com.google.firebase.firestore.PropertyName("force_update_message")
    val forceUpdateMessage: String = "A new version of Atmiya Innovation is available. Please update to continue using the app.",
    
    @SerializedName("force_update_title")
    @get:com.google.firebase.firestore.PropertyName("force_update_title")
    val forceUpdateTitle: String = "Update Required"
)
