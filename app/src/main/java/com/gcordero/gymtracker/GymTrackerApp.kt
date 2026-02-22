package com.gcordero.gymtracker

import android.app.Application
import com.google.firebase.FirebaseApp

class GymTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
