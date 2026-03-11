package com.gcordero.gymtracker.data.repository

import com.gcordero.gymtracker.domain.model.NutritionLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val logs = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(NutritionLog::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addLog(log: NutritionLog, dateKey: String) {
        val data = mapOf(
            "userId"      to log.userId,
            "description" to log.description,
            "calories"    to log.calories,
            "proteinG"    to log.proteinG,
            "carbsG"      to log.carbsG,
            "fatG"        to log.fatG,
            "fiberG"      to log.fiberG,
            "timestamp"   to log.timestamp,
            "dateKey"     to dateKey
        )
        col.add(data).await()
    }
}
