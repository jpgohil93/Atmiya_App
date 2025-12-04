package com.atmiya.innovation

import android.app.Application

class AtmiyaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable Firestore offline persistence
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        com.google.firebase.firestore.FirebaseFirestore.getInstance().firestoreSettings = settings

        // YoutubeDL init removed
    }


}
