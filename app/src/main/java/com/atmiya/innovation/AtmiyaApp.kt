package com.atmiya.innovation

import android.app.Application

import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

class AtmiyaApp : Application(), ImageLoaderFactory {
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

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
