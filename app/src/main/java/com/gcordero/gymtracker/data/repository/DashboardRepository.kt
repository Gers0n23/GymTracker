package com.gcordero.gymtracker.data.repository

import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.Routine
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.User
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DashboardRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getUser(userId: String): User? {
        return try {
            firestore.collection("users").document(userId).get().await()
                .toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserRoutines(userId: String): List<Routine> {
        return try {
            firestore.collection("routines")
                .whereEqualTo("userId", userId)
                .get().await()
                .documents
                .mapNotNull { it.toObject(Routine::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRecentSessions(userId: String, since: Timestamp): List<WorkoutSession> {
        return try {
            firestore.collection("sessions")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("startTime", since)
                .get().await()
                .documents
                .mapNotNull { it.toObject(WorkoutSession::class.java) }
                .sortedByDescending { it.startTime }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetches set records for the given session IDs, chunking requests to
     * stay within Firestore's whereIn limit of 30 items.
     */
    suspend fun getSetRecordsForSessions(sessionIds: List<String>): List<SetRecord> {
        if (sessionIds.isEmpty()) return emptyList()
        return try {
            sessionIds.chunked(30).flatMap { chunk ->
                firestore.collection("set_records")
                    .whereIn("sessionId", chunk)
                    .get().await()
                    .documents
                    .mapNotNull { it.toObject(SetRecord::class.java) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fetches all exercises for the given list of routine IDs.
     * Uses a single whereIn query (≤30 routines).
     */
    suspend fun getExercisesForRoutines(routineIds: List<String>): List<Exercise> {
        if (routineIds.isEmpty()) return emptyList()
        return try {
            routineIds.chunked(30).flatMap { chunk ->
                firestore.collection("exercises")
                    .whereIn("routineId", chunk)
                    .get().await()
                    .documents
                    .mapNotNull { it.toObject(Exercise::class.java)?.copy(id = it.id) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
