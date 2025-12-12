package com.atmiya.innovation.repository

import android.content.Context
import com.atmiya.innovation.data.Incubator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

class IncubatorRepository(private val context: Context) {

    fun getIncubators(): Flow<List<Incubator>> = flow {
        // delay(300) // Optional: Simulate loading if desired, or remove for instant load
        
        try {
            val jsonString = context.assets.open("incubators.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<Incubator>>() {}.type
            val incubators: List<Incubator> = Gson().fromJson(jsonString, listType)
            emit(incubators)
        } catch (e: IOException) {
            e.printStackTrace()
            emit(emptyList()) 
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }
}
