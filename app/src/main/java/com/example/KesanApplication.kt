package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class KesanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.kesanwater.azkqwv")
                    .setProjectId("gen-lang-client-0645171996")
                    .setApiKey("AIzaSyDefaultPlaceholderApiKey")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("KesanApplication", "FirebaseApp initialized successfully")
            }
        } catch (e: Exception) {
            Log.w("KesanApplication", "Could not initialize default FirebaseApp: ${e.message}")
        }
    }
}
