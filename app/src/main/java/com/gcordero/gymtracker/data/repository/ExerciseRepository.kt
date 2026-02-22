package com.gcordero.gymtracker.data.repository

import com.gcordero.gymtracker.domain.model.Exercise
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ExerciseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val exercisesCollection = firestore.collection("exercises")

    fun getExercisesByRoutine(routineId: String): Flow<List<Exercise>> = callbackFlow {
        val subscription = exercisesCollection
            .whereEqualTo("routineId", routineId)
            .orderBy("order")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val exercises = snapshot.toObjects(Exercise::class.java)
                    trySend(exercises)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addExercise(exercise: Exercise) {
        val docRef = exercisesCollection.document()
        val exerciseWithId = exercise.copy(id = docRef.id)
        docRef.set(exerciseWithId).await()
    }

    suspend fun updateExercise(exercise: Exercise) {
        if (exercise.id.isNotEmpty()) {
            exercisesCollection.document(exercise.id).set(exercise).await()
        }
    }

    suspend fun deleteExercise(exerciseId: String) {
        exercisesCollection.document(exerciseId).delete().await()
    }
}
