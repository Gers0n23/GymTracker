package com.gcordero.gymtracker.data.repository

import com.gcordero.gymtracker.domain.model.NutritionLog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NutritionRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("nutrition_logs")

    fun getTodayLogs(userId: String, dateKey: String): Flow<List<NutritionLog>> = callbackFlow {
        val listener = col
            .whereEqualTo("userId", userId)
            .whereEqualTo("dateKey", dateKey)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val logs = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(NutritionLog::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.timestamp.seconds } ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }

    fun getLogsByDateKey(userId: String, dateKey: String): Flow<List<NutritionLog>> =
        getTodayLogs(userId, dateKey)

    suspend fun addLog(log: NutritionLog, dateKey: String) {
        val data = mapOf(
            "userId"      to log.userId,
            "description" to log.description,
            "calories"    to log.calories,
            "proteinG"    to log.proteinG,
            "carbsG"      to log.carbsG,
            "fatG"        to log.fatG,
            "fiberG"      to log.fiberG,
            "mealType"    to log.mealType,
            "timestamp"   to log.timestamp,
            "dateKey"     to dateKey
        )
        col.add(data).await()
    }

    suspend fun updateLog(logId: String, log: NutritionLog) {
        col.document(logId).update(
            mapOf(
                "description" to log.description,
                "calories"    to log.calories,
                "proteinG"    to log.proteinG,
                "carbsG"      to log.carbsG,
                "fatG"        to log.fatG,
                "fiberG"      to log.fiberG,
                "mealType"    to log.mealType
            )
        ).await()
    }

    suspend fun deleteLog(logId: String) {
        col.document(logId).delete().await()
    }

    suspend fun getLogsForPeriod(
        userId: String,
        startDateKey: String,
        endDateKey: String
    ): List<NutritionLog> {
        return try {
            col.whereEqualTo("userId", userId)
                .get().await()
                .documents
                .mapNotNull { it.toObject(NutritionLog::class.java)?.copy(id = it.id) }
                .filter { it.dateKey in startDateKey..endDateKey }
                .sortedBy { it.dateKey }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
