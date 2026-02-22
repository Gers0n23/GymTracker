package com.gcordero.gymtracker

import android.app.Application
import com.google.firebase.FirebaseApp

class GymTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        
        // Habilitar persistencia offline para Firestore
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        com.google.firebase.firestore.FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}
