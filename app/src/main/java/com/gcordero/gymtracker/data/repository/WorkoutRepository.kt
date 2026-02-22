package com.gcordero.gymtracker.data.repository

import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class WorkoutRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val sessionsCollection = firestore.collection("sessions")
    private val setRecordsCollection = firestore.collection("set_records")

    suspend fun saveWorkoutSession(session: WorkoutSession, sets: List<SetRecord>) {
        val batch = firestore.batch()
        
        val sessionRef = sessionsCollection.document()
        val sessionId = sessionRef.id
        val finalSession = session.copy(id = sessionId)
        
        batch.set(sessionRef, finalSession)
        
        sets.forEach { set ->
            val setRef = setRecordsCollection.document()
            batch.set(setRef, set.copy(id = setRef.id, sessionId = sessionId))
        }
        
        batch.commit().await()
    }
}
