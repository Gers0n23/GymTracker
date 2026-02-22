package com.gcordero.gymtracker.data.repository

import com.gcordero.gymtracker.domain.model.Routine
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RoutineRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val routinesCollection = firestore.collection("routines")

    fun getRoutines(userId: String): Flow<List<Routine>> = callbackFlow {
        val subscription = routinesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val routines = snapshot.toObjects(Routine::class.java)
                    trySend(routines)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addRoutine(routine: Routine) {
        routinesCollection.add(routine).await()
    }

    suspend fun updateRoutine(routine: Routine) {
        if (routine.id.isNotEmpty()) {
            routinesCollection.document(routine.id).set(routine).await()
        }
    }

    suspend fun deleteRoutine(routineId: String) {
        routinesCollection.document(routineId).delete().await()
    }
}
