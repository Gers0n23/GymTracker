package com.gcordero.gymtracker.data.repository

import com.gcordero.gymtracker.domain.model.BodyMetric
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BodyMetricsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val metricsCollection = firestore.collection("body_metrics")

    fun getBodyMetrics(userId: String): Flow<List<BodyMetric>> = callbackFlow {
        val subscription = metricsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val metrics = snapshot.toObjects(BodyMetric::class.java)
                    trySend(metrics)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addBodyMetric(metric: BodyMetric) {
        metricsCollection.add(metric).await()
    }
}
