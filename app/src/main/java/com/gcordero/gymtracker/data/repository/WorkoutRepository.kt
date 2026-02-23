package com.gcordero.gymtracker.data.repository

import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WorkoutRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val sessionsCollection = firestore.collection("sessions")
    private val setRecordsCollection = firestore.collection("set_records")

    suspend fun saveWorkoutSession(session: WorkoutSession, sets: List<SetRecord>): String {
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
        return sessionId
    }

    fun getSessions(userId: String): Flow<List<WorkoutSession>> = callbackFlow {
        val listener = sessionsCollection
            .whereEqualTo("userId", userId)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WorkoutSession::class.java)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getSetsBySession(sessionId: String): List<SetRecord> {
        return setRecordsCollection
            .whereEqualTo("sessionId", sessionId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(SetRecord::class.java) }
            .sortedWith(compareBy({ it.exerciseName }, { it.setNumber }))
    }

    suspend fun updateSet(set: SetRecord) {
        setRecordsCollection.document(set.id).set(set).await()
    }

    suspend fun deleteSet(setId: String) {
        setRecordsCollection.document(setId).delete().await()
    }

    suspend fun updateSession(session: WorkoutSession) {
        sessionsCollection.document(session.id).set(session).await()
    }

    /**
     * Obtiene los sets de la sesión más reciente para una rutina dada.
     * Usado para prefill de pesos al iniciar una nueva sesión.
     * Retorna un mapa de exerciseId -> (peso de la última serie del ejercicio)
     */
    suspend fun getLastWeightsByRoutine(routineId: String, userId: String): Map<String, Double> {
        val lastSession = sessionsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("routineId", routineId)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toObject(WorkoutSession::class.java) ?: return emptyMap()

        val sets = getSetsBySession(lastSession.id)
        // Para cada ejercicio, guardamos el peso más alto de la última sesión
        return sets
            .groupBy { it.exerciseId }
            .mapValues { (_, exerciseSets) ->
                exerciseSets.maxOf { it.weight }
            }
    }
}
