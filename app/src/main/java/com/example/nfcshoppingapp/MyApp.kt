package com.example.nfcshoppingapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.database.FirebaseDatabase

@HiltAndroidApp
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase persistence here
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}
