package com.gcordero.gymtracker.data.util

import com.gcordero.gymtracker.domain.model.BodyMetric
import com.gcordero.gymtracker.domain.model.User
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.Routine
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

object DataPopulator {
    
    suspend fun populateInitialData(userId: String) {
        val db = FirebaseFirestore.getInstance()
        
        // 1. Crear Perfil de Usuario
        val user = User(
            id = userId,
            name = "Gerson",
            email = "gcordero@example.com",
            heightCm = 175.0,
            weightUnit = "kg"
        )
        db.collection("users").document(userId).set(user).await()

        // 2. Cargar Métricas de Cuerpo (Histórico para gráficas)
        val calendar = Calendar.getInstance()
        val metrics = mutableListOf<BodyMetric>()
        val startWeight = 85.0
        for (i in 0 until 10) {
            calendar.add(Calendar.DAY_OF_YEAR, -7 * i)
            metrics.add(BodyMetric(
                id = db.collection("body_metrics").document().id,
                userId = userId,
                weightKg = startWeight - (i * 0.5), // Baja de peso simulada
                imc = (startWeight - (i * 0.5)) / (1.75 * 1.75),
                timestamp = Timestamp(calendar.time)
            ))
            calendar.setTime(java.util.Date()) // Reset to now
        }
        
        metrics.forEach { metric ->
            db.collection("body_metrics").document(metric.id).set(metric).await()
        }

        // 3. Crear Rutinas y Ejercicios
        val routines = listOf(
            Routine(name = "Lunes: Empuje (Fuerza)", description = "Enfoque en pecho y tríceps.", daysOfWeek = listOf(1)),
            Routine(name = "Martes: Tracción (Postura)", description = "Enfoque en espalda, bíceps y corrección postural.", daysOfWeek = listOf(2)),
            Routine(name = "Miércoles: Piernas (Fuerza)", description = "Entrenamiento de tren inferior en máquinas.", daysOfWeek = listOf(3)),
            Routine(name = "Jueves: Hombros y Core", description = "Fuerza de hombros y estabilidad postural.", daysOfWeek = listOf(4)),
            Routine(name = "Viernes: Full Body (Híbrido)", description = "Resumen semanal enfocado en movilidad y fuerza general.", daysOfWeek = listOf(5))
        )

        val exerciseData = mapOf(
            "Lunes: Empuje (Fuerza)" to listOf(
                Exercise(name = "Press de Banca (Máquina)", muscleGroup = "Pecho", order = 0, mediaUrl = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOHpobnBzMnE5dzBxbW94eHd4eHd4eHd4eHd4eHd4eHAmZXA9djFfaW50ZXJuYWxfZ2lmX2J5X2lkJmN0PWc/3o7TKIF9uK7fOIf9Lq/giphy.gif"),
                Exercise(name = "Aperturas (Peck Deck)", muscleGroup = "Pecho", order = 1, mediaUrl = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOHpobnBzMnE5dzBxbW94eHd4eHd4eHd4eHd4eHd4eHAmZXA9djFfaW50ZXJuYWxfZ2lmX2J5X2lkJmN0PWc/3o7TKFv6wLjH7q7X3S/giphy.gif"),
                Exercise(name = "Extensiones de Tríceps (Polea)", muscleGroup = "Tríceps", order = 2)
            ),
            "Martes: Tracción (Postura)" to listOf(
                Exercise(name = "Jalón al Pecho", muscleGroup = "Espalda", order = 0, mediaUrl = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOHpobnBzMnE5dzBxbW94eHd4eHd4eHd4eHd4eHd4eHAmZXA9djFfaW50ZXJuYWxfZ2lmX2J5X2lkJmN0PWc/3o7TKP7uLjH7q7X3S/giphy.gif"),
                Exercise(name = "Remo Sentado (Máquina)", muscleGroup = "Espalda (Postura)", order = 1),
                Exercise(name = "Face Pulls", muscleGroup = "Hombro Posterior/Postura", order = 2),
                Exercise(name = "Curl de Bíceps (Polea)", muscleGroup = "Bíceps", order = 3)
            ),
            "Miércoles: Piernas (Fuerza)" to listOf(
                Exercise(name = "Prensa de Piernas", muscleGroup = "Cuádriceps/Glúteo", order = 0, mediaUrl = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOHpobnBzMnE5dzBxbW94eHd4eHd4eHd4eHd4eHd4eHAmZXA9djFfaW50ZXJuYWxfZ2lmX2J5X2lkJmN0PWc/3o7TKFv6wLjH7q7X3S/giphy.gif"),
                Exercise(name = "Extensión de Cuádriceps", muscleGroup = "Cuádriceps", order = 1),
                Exercise(name = "Curl Femoral (Sentado)", muscleGroup = "Isquios", order = 2)
            ),
            "Jueves: Hombros y Core" to listOf(
                Exercise(name = "Press Militar (Máquina)", muscleGroup = "Hombros", order = 0),
                Exercise(name = "Vuelos Laterales (Máquina)", muscleGroup = "Hombro Lateral", order = 1),
                Exercise(name = "Plancha Abdominal", muscleGroup = "Core (Postura)", order = 2, notes = "Mantener por tiempo")
            ),
            "Viernes: Full Body (Híbrido)" to listOf(
                Exercise(name = "Press de Pecho", muscleGroup = "Pecho", order = 0),
                Exercise(name = "Remo Horizontal", muscleGroup = "Espalda", order = 1),
                Exercise(name = "Prensa", muscleGroup = "Piernas", order = 2),
                Exercise(name = "Bird Dog", muscleGroup = "Lumbar/Postura", order = 3, notes = "Control lumbar")
            )
        )

        routines.forEach { routineBase ->
            val routineRef = db.collection("routines").document()
            val routineId = routineRef.id
            val routine = routineBase.copy(id = routineId, userId = userId)
            
            db.collection("routines").document(routineId).set(routine).await()
            
            val exercises = exerciseData[routineBase.name] ?: emptyList()
            exercises.forEach { exBase ->
                val exRef = db.collection("exercises").document()
                val exercise = exBase.copy(id = exRef.id, routineId = routineId)
                db.collection("exercises").document(exRef.id).set(exercise).await()
            }

            // 4. Crear algunas sesiones históricas para ver Volumen Semanal
            val session = WorkoutSession(
                id = db.collection("workouts").document().id,
                userId = userId,
                routineId = routineId,
                routineName = routineBase.name,
                startTime = Timestamp.now(),
                totalWeightLifted = 45200.0,
                rpe = 8
            )
            db.collection("workouts").document(session.id).set(session).await()
        }
    }
}
