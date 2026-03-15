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
                    val metrics = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(BodyMetric::class.java)?.copy(id = doc.id)
                    }
                    trySend(metrics)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addBodyMetric(metric: BodyMetric) {
        metricsCollection.add(metric).await()
    }

    suspend fun updateBodyMetric(metric: BodyMetric) {
        if (metric.id.isNotEmpty()) {
            metricsCollection.document(metric.id).set(metric).await()
        }
    }

    suspend fun deleteBodyMetric(metricId: String) {
        metricsCollection.document(metricId).delete().await()
    }
}
