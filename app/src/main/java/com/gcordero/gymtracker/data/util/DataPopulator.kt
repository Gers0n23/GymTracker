package com.gcordero.gymtracker.data.util

import android.util.Log
import com.gcordero.gymtracker.domain.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

object DataPopulator {
    private const val TAG = "DataPopulator"

    suspend fun populateInitialData(userId: String): Result<Unit> {
        return try {
            val db = FirebaseFirestore.getInstance()
            Log.d(TAG, "Iniciando población de datos para el usuario: $userId")

            // 1. Limpiar datos previos
            Log.d(TAG, "1. Limpiando datos previos...")
            clearUserData(db, userId)

            // 2. Crear Perfil de Usuario
            Log.d(TAG, "2. Creando perfil de usuario...")
            val user = User(
                id = userId,
                name = "Gerson",
                email = "gcordero@example.com",
                heightCm = 175.0,
                weightUnit = "kg"
            )
            db.collection("users").document(userId).set(user).await()

            // 3. Cargar Métricas de Cuerpo
            Log.d(TAG, "3. Cargando métricas corporales...")
            val calendar = Calendar.getInstance()
            val metricsBatch = db.batch()
            val startWeight = 85.0
            for (i in 0 until 12) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -7 * i)
                val metricId = db.collection("body_metrics").document().id
                val weight = startWeight - (i * 0.4) + (Math.random() * 0.5)
                val metric = BodyMetric(
                    id = metricId,
                    userId = userId,
                    weightKg = weight,
                    imc = weight / (1.75 * 1.75),
                    timestamp = Timestamp(calendar.time)
                )
                metricsBatch.set(db.collection("body_metrics").document(metricId), metric)
            }
            metricsBatch.commit().await()

            // 4. Definir Rutinas
            Log.d(TAG, "4. Definiendo rutinas...")
            val routines = listOf(
                Routine(name = "Lunes: Espalda + Bíceps + Core", description = "Enfoque en tracción y estabilidad central.", daysOfWeek = listOf(1)),
                Routine(name = "Martes: Piernas (Cuádriceps y Glúteos)", description = "Potencia en tren inferior, énfasis anterior.", daysOfWeek = listOf(2)),
                Routine(name = "Miércoles: Cardio + Core Postural", description = "Recuperación activa y trabajo de postura.", daysOfWeek = listOf(3)),
                Routine(name = "Jueves: Pecho + Tríceps + Hombros", description = "Empujes horizontales y verticales.", daysOfWeek = listOf(4)),
                Routine(name = "Viernes: Piernas (Isquios y Glúteos)", description = "Tren inferior, énfasis posterior.", daysOfWeek = listOf(5))
            )

            val exerciseData = mapOf(
                "Lunes: Espalda + Bíceps + Core" to listOf(
                    Pair("Jalón al Pecho", "Espalda"),
                    Pair("Remo Sentado", "Espalda"),
                    Pair("Curl Bíceps Máquina", "Bíceps"),
                    Pair("Plancha Frontal", "Core")
                ),
                "Martes: Piernas (Cuádriceps y Glúteos)" to listOf(
                    Pair("Prensa de Piernas", "Piernas"),
                    Pair("Extensión de Cuádriceps", "Piernas"),
                    Pair("Gemelos en Máquina", "Gemelos")
                ),
                "Jueves: Pecho + Tríceps + Hombros" to listOf(
                    Pair("Chest Press Máquina", "Pecho"),
                    Pair("Press Hombros Máquina", "Hombros"),
                    Pair("Tríceps Polea", "Tríceps")
                )
            )

            // 5. Poblar Rutinas y Ejercicios
            Log.d(TAG, "5. Poblando rutinas y ejercicios...")
            for (routineBase in routines) {
                val routineRef = db.collection("routines").document()
                val routineId = routineRef.id
                val routine = routineBase.copy(id = routineId, userId = userId)
                db.collection("routines").document(routineId).set(routine).await()

                val exercises = exerciseData[routineBase.name] ?: listOf(Pair("Ejercicio Genérico", "General"))
                val exerciseIds = mutableListOf<String>()
                
                exercises.forEachIndexed { index, (name, muscle) ->
                    val exRef = db.collection("exercises").document()
                    val exercise = Exercise(
                        id = exRef.id,
                        routineId = routineId,
                        name = name,
                        muscleGroup = muscle,
                        order = index,
                        notes = "Realizar con control técnico."
                    )
                    db.collection("exercises").document(exRef.id).set(exercise).await()
                    exerciseIds.add(exRef.id)
                }

                // 6. Crear una sesión histórica
                createHistoricalSession(db, userId, routineId, routineBase.name, exerciseIds, exercises)
            }

            Log.d(TAG, "Población completada exitosamente.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la población de datos: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun createHistoricalSession(
        db: FirebaseFirestore,
        userId: String,
        routineId: String,
        routineName: String,
        exerciseIds: List<String>,
        exerciseNames: List<Pair<String, String>>
    ) {
        val sessionRef = db.collection("sessions").document()
        val sessionId = sessionRef.id
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -(2..7).random())
        
        val session = WorkoutSession(
            id = sessionId,
            userId = userId,
            routineId = routineId,
            routineName = routineName,
            startTime = Timestamp(calendar.time),
            endTime = Timestamp(Date(calendar.timeInMillis + 3600000)),
            totalWeightLifted = 0.0,
            rpe = (7..9).random(),
            comments = "Sesión cargada por el sistema."
        )

        var totalVolume = 0.0
        val setsBatch = db.batch()

        exerciseIds.forEachIndexed { exIndex, exId ->
            val exName = exerciseNames[exIndex].first
            for (setNum in 1..3) {
                val weight = (40..80).random().toDouble()
                val reps = (8..12).random()
                val setId = db.collection("set_records").document().id
                val setRecord = SetRecord(
                    id = setId,
                    sessionId = sessionId,
                    exerciseId = exId,
                    exerciseName = exName,
                    setNumber = setNum,
                    weight = weight,
                    reps = reps,
                    rir = (1..3).random(),
                    timestamp = session.startTime
                )
                setsBatch.set(db.collection("set_records").document(setId), setRecord)
                totalVolume += weight * reps
            }
        }

        val finalSession = session.copy(totalWeightLifted = totalVolume)
        db.collection("sessions").document(sessionId).set(finalSession).await()
        setsBatch.commit().await()
    }

    private suspend fun clearUserData(db: FirebaseFirestore, userId: String) {
        val collections = listOf("routines", "sessions", "body_metrics")
        for (coll in collections) {
            val query = db.collection(coll).whereEqualTo("userId", userId)
            val snapshot = query.get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        }
        // Nota: Los ejercicios y set_records son más difíciles de borrar sin userId directo,
        // pero al borrar rutinas y sesiones, se consideran "limpiados" para el flujo de la app.
    }
}
