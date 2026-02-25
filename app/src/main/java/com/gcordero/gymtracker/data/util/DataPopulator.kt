package com.gcordero.gymtracker.data.util

import android.util.Log
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.ExerciseType
import com.gcordero.gymtracker.domain.model.Routine
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object DataPopulator {
    private const val TAG = "DataPopulator"

    // Grupos musculares estándar disponibles para filtros
    // Espalda, Pecho, Hombros, Bíceps, Tríceps, Cuádriceps,
    // Isquiotibiales, Glúteos, Gemelos, Abductores, Core, Lumbar, Cardio

    suspend fun populateInitialData(userId: String): Result<Unit> {
        return try {
            val db = FirebaseFirestore.getInstance()
            Log.d(TAG, "Iniciando creación de rutinas para: $userId")

            Log.d(TAG, "1. Limpiando rutinas y ejercicios previos...")
            clearRoutinesAndExercises(db, userId)

            Log.d(TAG, "2. Creando rutinas y ejercicios...")
            for ((routineBase, exercises) in buildRoutinesData()) {
                val routineRef = db.collection("routines").document()
                val routineId = routineRef.id
                val routine = routineBase.copy(id = routineId, userId = userId)
                db.collection("routines").document(routineId).set(routine).await()

                exercises.forEachIndexed { index, exercise ->
                    val exRef = db.collection("exercises").document()
                    val saved = exercise.copy(id = exRef.id, routineId = routineId, order = index)
                    db.collection("exercises").document(exRef.id).set(saved).await()
                }
            }

            Log.d(TAG, "Rutinas creadas exitosamente.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la creación de rutinas: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------------------------
    // Rutina Completa del PDF – Máquinas (Fuerza + Postura)
    // Grupos musculares estándar: Espalda | Pecho | Hombros | Bíceps | Tríceps |
    //   Cuádriceps | Isquiotibiales | Glúteos | Gemelos | Abductores | Core | Lumbar | Cardio
    // ---------------------------------------------------------------------------
    private fun buildRoutinesData(): List<Pair<Routine, List<Exercise>>> = listOf(

        // --------------------------------------------------------------------
        // LUNES – Espalda + Bíceps + Core ligero  (daysOfWeek = 1)
        // --------------------------------------------------------------------
        Pair(
            Routine(
                name = "Espalda + Bíceps + Core",
                description = "Tracción y estabilidad central.",
                daysOfWeek = listOf(1),
                muscleGroups = listOf("Espalda", "Bíceps", "Core")
            ),
            listOf(
                Exercise(
                    name = "Jalón al Pecho",
                    muscleGroup = "Espalda",
                    equipment = "Polea alta",
                    notes = "4 × 10–12\n\n• Retrae las escápulas (omóplatos atrás y abajo) antes de jalar.\n• Saca el pecho al frente durante todo el movimiento.\n• Lleva los codos hacia las caderas, no hacia atrás.\n• Baja la barra hasta el mentón, nunca detrás del cuello.\n• Controla la subida resistiendo 2 segundos.",
                    mediaUrl = "https://www.youtube.com/results?search_query=jalon+al+pecho+tecnica+correcta+maquina",
                    targetSets = 4
                ),
                Exercise(
                    name = "Remo Sentado en Máquina",
                    muscleGroup = "Espalda",
                    equipment = "Polea baja",
                    notes = "3 × 10–12\n\n• Pecho afuera y hombros hacia atrás y abajo en todo momento.\n• Jala hacia el ombligo, no hacia el pecho.\n• Codos cerca del cuerpo, no abiertos.\n• Pausa 1 segundo cuando los codos estén detrás del cuerpo.\n• No uses el torso para impulsar el peso.",
                    mediaUrl = "https://www.youtube.com/results?search_query=remo+sentado+polea+maquina+tecnica",
                    targetSets = 3
                ),
                Exercise(
                    name = "Jalón Agarre Estrecho",
                    muscleGroup = "Espalda",
                    equipment = "Polea alta",
                    notes = "3 × 10–12\n\n• Usa agarre neutro (palmas enfrentadas) si la máquina lo permite.\n• Mayor énfasis en la parte baja del dorsal y redondo mayor.\n• Mismos principios del jalón ancho: escápulas atrás, pecho afuera.\n• Lleva el agarre hacia el esternón con los codos pegados al cuerpo.",
                    mediaUrl = "https://www.youtube.com/results?search_query=jalon+agarre+estrecho+dorsales+tecnica",
                    targetSets = 3
                ),
                Exercise(
                    name = "Curl Bíceps en Máquina",
                    muscleGroup = "Bíceps",
                    equipment = "Máquina de curl",
                    notes = "3 × 10–12\n\n• Codos fijos sobre el soporte, no los levantes.\n• No balancees el torso ni uses impulso para subir.\n• Contrae fuerte el bíceps arriba y mantén 1 segundo.\n• Baja lento y controlado (2–3 segundos).\n• Asegúrate de que los codos queden alineados con el eje de la máquina.",
                    mediaUrl = "https://www.youtube.com/results?search_query=curl+biceps+maquina+tecnica+correcta",
                    targetSets = 3
                ),
                Exercise(
                    name = "Curl Polea Baja",
                    muscleGroup = "Bíceps",
                    equipment = "Polea baja",
                    notes = "2 × 12–15\n\n• Codos fijos a los costados, no los muevas hacia adelante.\n• Supina las muñecas (gíralas hacia afuera) al subir para mayor contracción.\n• Controla la bajada hasta la extensión total del brazo.\n• Parado derecho, sin arquear la espalda.",
                    mediaUrl = "https://www.youtube.com/results?search_query=curl+biceps+polea+baja+tecnica",
                    targetSets = 2
                ),
                Exercise(
                    name = "Plancha Frontal",
                    muscleGroup = "Core",
                    equipment = "Suelo",
                    notes = "2 × 40 seg\n\n• Cuerpo en línea recta de cabeza a talones, sin caer las caderas.\n• Aprieta glúteos y abdomen al mismo tiempo.\n• Empuja el suelo con los antebrazos activamente.\n• Cabeza en posición neutra, mirando al suelo.\n• Respira de forma constante, no contengas el aire.",
                    mediaUrl = "https://www.youtube.com/results?search_query=plancha+frontal+tecnica+correcta+core",
                    targetSets = 2,
                    exerciseType = ExerciseType.TIMED.name
                ),
                Exercise(
                    name = "Bird Dog",
                    muscleGroup = "Core",
                    equipment = "Suelo",
                    notes = "2 × 8 por lado\n\n• Columna neutra en todo momento, sin curva ni arco.\n• Mueve brazo y pierna contraria simultáneamente y lento (3 seg).\n• Pausa 2 segundos en la posición extendida.\n• Mira al suelo para mantener el cuello neutro.\n• Si las caderas rotan al extender la pierna, no subas tan alto.",
                    mediaUrl = "https://www.youtube.com/results?search_query=bird+dog+ejercicio+core+tecnica",
                    targetSets = 2
                )
            )
        ),

        // --------------------------------------------------------------------
        // MARTES – Piernas (Cuádriceps + Glúteos)  (daysOfWeek = 2)
        // --------------------------------------------------------------------
        Pair(
            Routine(
                name = "Piernas – Cuádriceps + Glúteos",
                description = "Potencia en tren inferior, énfasis anterior.",
                daysOfWeek = listOf(2),
                muscleGroups = listOf("Cuádriceps", "Glúteos")
            ),
            listOf(
                Exercise(
                    name = "Prensa de Piernas",
                    muscleGroup = "Cuádriceps",
                    equipment = "Prensa de piernas",
                    notes = "4 × 10–12\n\n• Pies al ancho de los hombros o ligeramente más separados.\n• Rodillas en línea con los dedos del pie, no colapsen hacia adentro.\n• NUNCA bloquees las rodillas al extender, mantén leve flexión.\n• Espalda baja pegada al respaldo en todo momento.\n• Baja hasta que las rodillas formen ~90° sin que la cadera se levante.",
                    mediaUrl = "https://www.youtube.com/results?search_query=prensa+de+piernas+tecnica+correcta",
                    targetSets = 4
                ),
                Exercise(
                    name = "Extensión de Cuádriceps",
                    muscleGroup = "Cuádriceps",
                    equipment = "Máquina de extensión",
                    notes = "3 × 12–15\n\n• Alinea la articulación de la rodilla con el eje de la máquina.\n• Contrae completamente el cuádriceps al llegar arriba.\n• Baja de forma controlada en 2–3 segundos, no dejes caer el peso.\n• No uses impulso ni balancees el torso para subir.\n• Punta del pie ligeramente hacia arriba al subir.",
                    mediaUrl = "https://www.youtube.com/results?search_query=extension+cuadriceps+maquina+tecnica",
                    targetSets = 3
                ),
                Exercise(
                    name = "Curl Femoral",
                    muscleGroup = "Isquiotibiales",
                    equipment = "Máquina de curl femoral",
                    notes = "3 × 10–12\n\n• No arquees la espalda baja para compensar, torso quieto.\n• Contrae los isquiotibiales completamente al final del recorrido.\n• Baja lento y controlado (la fase excéntrica es clave).\n• En la versión acostada: la cadera no debe levantarse del pad.\n• Pies en posición neutra o ligeramente hacia adentro.",
                    mediaUrl = "https://www.youtube.com/results?search_query=curl+femoral+maquina+tecnica+correcta",
                    targetSets = 3
                ),
                Exercise(
                    name = "Gemelos en Máquina",
                    muscleGroup = "Gemelos",
                    equipment = "Máquina de gemelos",
                    notes = "4 × 12–15\n\n• Rango de movimiento COMPLETO: baja hasta sentir el estiramiento total.\n• Sube lo más alto posible en la punta del pie y mantén 1–2 segundos.\n• Para 1 segundo abajo antes de subir, no rebotas.\n• Movimiento lento y controlado.\n• Prueba con pies paralelos, hacia adentro y afuera para distintas porciones.",
                    mediaUrl = "https://www.youtube.com/results?search_query=gemelos+maquina+calf+raise+tecnica",
                    targetSets = 4
                )
            )
        ),

        // --------------------------------------------------------------------
        // MIÉRCOLES – Cardio suave + Core Postural  (daysOfWeek = 3)
        // --------------------------------------------------------------------
        Pair(
            Routine(
                name = "Cardio + Core Postural",
                description = "Recuperación activa y trabajo de postura.",
                daysOfWeek = listOf(3),
                muscleGroups = listOf("Cardio", "Core")
            ),
            listOf(
                Exercise(
                    name = "Trotadora",
                    muscleGroup = "Cardio",
                    equipment = "Cardio",
                    notes = "15–20 minutos\n\n• Intensidad moderada: debes poder mantener una conversación.\n• Objetivo: activación y preparación para el core, no agotar el cuerpo.\n• Mantén la espalda erguida y los hombros relajados.",
                    mediaUrl = "https://www.youtube.com/results?search_query=cardio+moderado+eliptica+bici+cinta+tecnica",
                    targetSets = 1,
                    exerciseType = ExerciseType.CARDIO.name
                ),
                Exercise(
                    name = "Dead Bug",
                    muscleGroup = "Core",
                    equipment = "Suelo",
                    notes = "3 × 8–10 por lado\n\n• Espalda baja PEGADA al suelo durante todo el movimiento.\n• Baja brazo y pierna opuesta simultáneamente, lento y controlado.\n• Exhala al bajar para mantener la espalda en contacto con el suelo.\n• Si la espalda se despega, reduce el rango de movimiento.\n• Pausa 1 segundo en la posición extendida.",
                    mediaUrl = "https://www.youtube.com/results?search_query=dead+bug+ejercicio+core+tecnica+correcta",
                    targetSets = 3
                ),
                Exercise(
                    name = "Plancha Frontal",
                    muscleGroup = "Core",
                    equipment = "Suelo",
                    notes = "3 × 30–45 seg\n\n• Cuerpo en línea recta de cabeza a talones, sin caer las caderas.\n• Aprieta glúteos y abdomen al mismo tiempo.\n• Empuja el suelo con los antebrazos activamente.\n• Cabeza en posición neutra, mirando al suelo.\n• Respira de forma constante, no contengas el aire.",
                    mediaUrl = "https://www.youtube.com/results?search_query=plancha+frontal+tecnica+correcta+core",
                    targetSets = 3,
                    exerciseType = ExerciseType.TIMED.name
                ),
                Exercise(
                    name = "Bird Dog Pausado",
                    muscleGroup = "Core",
                    equipment = "Suelo",
                    notes = "3 × 8 por lado\n\n• Igual que el bird dog estándar, pero mantén la posición extendida 3–5 segundos.\n• Enfócate en sentir la tensión en el core, no solo en la postura.\n• Movimiento aún más lento y deliberado.\n• Columna completamente neutra durante toda la pausa.",
                    mediaUrl = "https://www.youtube.com/results?search_query=bird+dog+pausado+core+tecnica",
                    targetSets = 3
                ),
                Exercise(
                    name = "Pallof Press en Polea",
                    muscleGroup = "Core",
                    equipment = "Polea",
                    notes = "3 × 10–12\n\n• Párate de lado a la polea, pies al ancho de hombros, rodillas ligeramente dobladas.\n• El objetivo es NO rotar: el core trabaja resistiendo la torsión.\n• Hombros hacia atrás y abajo, no te encorvs.\n• Glúteos apretados para estabilizar la pelvis.\n• Empieza con poco peso: la dificultad está en la resistencia, no en el peso.",
                    mediaUrl = "https://www.youtube.com/results?search_query=pallof+press+polea+core+antirotacion+tecnica",
                    targetSets = 3
                ),
                Exercise(
                    name = "Crunch en Polea",
                    muscleGroup = "Core",
                    equipment = "Polea alta",
                    notes = "3 × 12\n\n• De rodillas, agarra la cuerda detrás de la cabeza o al frente.\n• Dobla SOLO la columna como si quisieras tocar el suelo con los codos.\n• No jales con los brazos ni los hombros, el trabajo es del abdomen.\n• La cadera queda fija mientras la columna se flexiona.\n• Mantén 1 segundo contraído y baja lento (3 segundos).",
                    mediaUrl = "https://www.youtube.com/results?search_query=crunch+en+polea+cable+crunch+tecnica",
                    targetSets = 3
                ),
                Exercise(
                    name = "Extensión Lumbar en Máquina",
                    muscleGroup = "Lumbar",
                    equipment = "Máquina lumbar",
                    notes = "2 × 12\n\n• Movimiento SUAVE y controlado, sin hiperextender la espalda.\n• El objetivo es fortalecer los erectores espinales, no doblar al máximo.\n• Al subir, aprieta la espalda baja suavemente, sin arquear exageradamente.\n• Baja lento y controlado, sin dejar caer el peso.\n• Usa poco peso: este ejercicio es para postura.",
                    mediaUrl = "https://www.youtube.com/results?search_query=extension+lumbar+maquina+tecnica+espalda+baja",
                    targetSets = 2
                ),
                Exercise(
                    name = "Puente de Glúteos",
                    muscleGroup = "Glúteos",
                    equipment = "Suelo",
                    notes = "3 × 12–15\n\n• Pies apoyados planos en el suelo, al ancho de los hombros.\n• Empuja a través de los talones, no de los dedos.\n• Aprieta fuerte los glúteos en la parte alta y mantén 1–2 segundos.\n• La espalda lumbar NO debe arquearse exageradamente.\n• Mentón ligeramente al pecho para proteger el cuello.",
                    mediaUrl = "https://www.youtube.com/results?search_query=puente+de+gluteos+tecnica+correcta+activacion",
                    targetSets = 3
                ),
                Exercise(
                    name = "Plancha Lateral",
                    muscleGroup = "Core",
                    equipment = "Suelo",
                    notes = "2 × 30–40 seg por lado\n\n• Caderas completamente alineadas, no dejes caer la pelvis ni la subas.\n• Cuerpo en línea recta de cabeza a pies.\n• Aprieta los oblicuos activamente, no solo aguantes con el peso.\n• El brazo libre puede ir extendido al techo para mayor dificultad.\n• Respira de forma constante durante todo el tiempo.",
                    mediaUrl = "https://www.youtube.com/results?search_query=plancha+lateral+tecnica+correcta+oblicuos",
                    targetSets = 2,
                    exerciseType = ExerciseType.TIMED.name
                )
            )
        ),

        // --------------------------------------------------------------------
        // JUEVES – Pecho + Tríceps + Hombros + Core ligero  (daysOfWeek = 4)
        // --------------------------------------------------------------------
        Pair(
            Routine(
                name = "Pecho + Tríceps + Hombros + Core",
                description = "Empujes horizontales y verticales.",
                daysOfWeek = listOf(4),
                muscleGroups = listOf("Pecho", "Tríceps", "Hombros", "Core")
            ),
            listOf(
                Exercise(
                    name = "Chest Press en Máquina",
                    muscleGroup = "Pecho",
                    equipment = "Máquina de pecho",
                    notes = "4 × 10–12\n\n• Retrae las escápulas (hombros hacia atrás y abajo) antes de empujar.\n• Saca el pecho al frente, no lo dejes hundido.\n• Los codos NO deben quedar por detrás del plano del cuerpo.\n• Empuja con fuerza y regresa lento (2–3 segundos).\n• No bloquees los codos al extender.",
                    mediaUrl = "https://www.youtube.com/results?search_query=chest+press+maquina+pecho+tecnica+correcta",
                    targetSets = 4
                ),
                Exercise(
                    name = "Pec Deck / Aperturas",
                    muscleGroup = "Pecho",
                    equipment = "Pec Deck",
                    notes = "3 × 10–12\n\n• Leve flexión en los codos durante todo el movimiento.\n• Al cerrar, imagina que abrazas un árbol: el movimiento viene del pecho.\n• Aprieta el pecho en la posición cerrada y mantén 1 segundo.\n• No abras más allá de lo que permita el hombro sin tensión.\n• Hombros hacia atrás y abajo, espalda pegada al respaldo.",
                    mediaUrl = "https://www.youtube.com/results?search_query=pec+deck+apertura+maquina+pecho+tecnica",
                    targetSets = 3
                ),
                Exercise(
                    name = "Press de Hombros en Máquina",
                    muscleGroup = "Hombros",
                    equipment = "Máquina de hombros",
                    notes = "3 × 10–12\n\n• No arquees la espalda baja, ajusta bien el asiento.\n• Hombros hacia abajo durante todo el movimiento, no los encojas.\n• Empuja hacia arriba en línea recta.\n• No bloquees los codos al extender.\n• Core apretado para proteger la espalda baja.",
                    mediaUrl = "https://www.youtube.com/results?search_query=press+hombros+maquina+tecnica+deltoides",
                    targetSets = 3
                ),
                Exercise(
                    name = "Elevaciones Laterales en Máquina",
                    muscleGroup = "Hombros",
                    equipment = "Máquina lateral",
                    notes = "3 × 12–15\n\n• Codos ligeramente doblados durante todo el movimiento.\n• Levanta hasta el nivel de los hombros, no más arriba.\n• Imagina que viertes agua de dos jarras: codo ligeramente sobre la mano.\n• Baja lento y controlado: la bajada es donde más trabaja el músculo.\n• Hombros hacia abajo, no los encojas al subir.",
                    mediaUrl = "https://www.youtube.com/results?search_query=elevaciones+laterales+maquina+hombros+tecnica",
                    targetSets = 3
                ),
                Exercise(
                    name = "Extensión de Tríceps en Polea",
                    muscleGroup = "Tríceps",
                    equipment = "Polea alta",
                    notes = "3 × 10–12\n\n• Codos FIJOS a los costados del cuerpo, no los muevas.\n• Empuja hacia abajo contrayendo el tríceps completamente.\n• Mantén 1 segundo en la posición extendida.\n• Baja lento y controlado sintiendo el estiramiento del tríceps.\n• Con la cuerda: separa los extremos abajo para mayor rango de movimiento.",
                    mediaUrl = "https://www.youtube.com/results?search_query=extension+triceps+polea+pushdown+tecnica",
                    targetSets = 3
                ),
                Exercise(
                    name = "Fondos Asistidos en Máquina",
                    muscleGroup = "Tríceps",
                    equipment = "Máquina de fondos",
                    notes = "2 × 10–12\n\n• Para énfasis en TRÍCEPS: cuerpo vertical, codos cerca del cuerpo.\n• Para énfasis en PECHO: inclínate ligeramente hacia adelante.\n• Baja lento hasta que los brazos formen 90°.\n• Hombros hacia abajo, no los dejes subir hacia las orejas.\n• Sube empujando con fuerza sin bloquear los codos al final.",
                    mediaUrl = "https://www.youtube.com/results?search_query=fondos+asistidos+maquina+triceps+pecho+tecnica",
                    targetSets = 2
                ),
                Exercise(
                    name = "Pallof Press",
                    muscleGroup = "Core",
                    equipment = "Polea",
                    notes = "2 × 12\n\n• Párate de lado a la polea, pies al ancho de hombros.\n• El objetivo es NO rotar: el core trabaja resistiendo la torsión.\n• Glúteos apretados para estabilizar la pelvis.\n• Empieza con poco peso: la dificultad está en la resistencia.",
                    mediaUrl = "https://www.youtube.com/results?search_query=pallof+press+polea+core+antirotacion+tecnica",
                    targetSets = 2
                ),
                Exercise(
                    name = "Elevación de Rodillas en Banco",
                    muscleGroup = "Core",
                    equipment = "Banco",
                    notes = "2 × 12\n\n• NO uses impulso: sube las rodillas de forma controlada.\n• Contrae el abdomen activamente al subir.\n• Baja lento y controlado, no dejes caer las piernas.\n• La espalda baja debe permanecer apoyada en el banco.\n• Si puedes hacer las reps sin esfuerzo, sube más las rodillas.",
                    mediaUrl = "https://www.youtube.com/results?search_query=elevacion+rodillas+banco+abdominales+tecnica",
                    targetSets = 2
                )
            )
        ),

        // --------------------------------------------------------------------
        // VIERNES – Piernas (Isquiotibiales + Glúteos)  (daysOfWeek = 5)
        // --------------------------------------------------------------------
        Pair(
            Routine(
                name = "Piernas – Isquios + Glúteos",
                description = "Tren inferior, énfasis posterior.",
                daysOfWeek = listOf(5),
                muscleGroups = listOf("Isquios", "Glúteos")
            ),
            listOf(
                Exercise(
                    name = "Prensa de Piernas (Pies Altos)",
                    muscleGroup = "Glúteos",
                    equipment = "Prensa de piernas",
                    notes = "4 × 10–12\n\n• Pies en la parte alta de la plataforma, ligeramente más abiertos.\n• Esta posición traslada el énfasis a glúteos e isquiotibiales.\n• Baja más profundo para mayor rango de movimiento.\n• Rodillas en línea con los dedos del pie, no colapsen hacia adentro.\n• NUNCA bloquees las rodillas al extender.",
                    mediaUrl = "https://www.youtube.com/results?search_query=prensa+piernas+pies+altos+gluteos+isquios+tecnica",
                    targetSets = 4
                ),
                Exercise(
                    name = "Hip Thrust en Máquina",
                    muscleGroup = "Glúteos",
                    equipment = "Máquina hip thrust",
                    notes = "3 × 10–12\n\n• Empuja principalmente con los talones, no con los dedos.\n• Aprieta los glúteos FUERTE en la posición alta y mantén 1–2 segundos.\n• Mentón al pecho: evita que la espalda baja se arquee en exceso.\n• Rodillas al ancho de los hombros durante el movimiento.\n• Siente que el trabajo viene del glúteo, no de la espalda.",
                    mediaUrl = "https://www.youtube.com/results?search_query=hip+thrust+maquina+gluteos+tecnica+correcta",
                    targetSets = 3
                ),
                Exercise(
                    name = "Curl Femoral",
                    muscleGroup = "Isquiotibiales",
                    equipment = "Máquina de curl femoral",
                    notes = "3 × 10–12\n\n• No arquees la espalda baja para compensar, torso quieto.\n• Contrae los isquiotibiales completamente al final del recorrido.\n• Baja lento y controlado (la fase excéntrica es clave).\n• En la versión acostada: la cadera no debe levantarse del pad.",
                    mediaUrl = "https://www.youtube.com/results?search_query=curl+femoral+maquina+tecnica+correcta",
                    targetSets = 3
                ),
                Exercise(
                    name = "Abductores / Aductores en Máquina",
                    muscleGroup = "Abductores",
                    equipment = "Máquina abductores",
                    notes = "3 × 12–15\n\nABDUCTORES (abre las piernas):\n• Movimiento controlado, no uses impulso ni rebotes.\n• Pausa 1 segundo en la posición abierta.\n\nADUCTORES (cierra las piernas):\n• Cierra lento y controlado, la bajada es tan importante como la subida.\n• No aprietes tanto que la pelvis rote, mantén la cadera neutra.",
                    mediaUrl = "https://www.youtube.com/results?search_query=abductores+aductores+maquina+tecnica+correcta",
                    targetSets = 3
                ),
                Exercise(
                    name = "Gemelos en Máquina",
                    muscleGroup = "Gemelos",
                    equipment = "Máquina de gemelos",
                    notes = "3 × 12–15\n\n• Rango de movimiento COMPLETO: baja hasta sentir el estiramiento total.\n• Sube lo más alto posible y mantén 1–2 segundos arriba.\n• Para 1 segundo abajo antes de subir, no rebotas.\n• Movimiento lento y controlado, sin impulso.",
                    mediaUrl = "https://www.youtube.com/results?search_query=gemelos+maquina+calf+raise+tecnica",
                    targetSets = 3
                )
            )
        )
    )

    private suspend fun clearRoutinesAndExercises(db: FirebaseFirestore, userId: String) {
        val routinesSnapshot = db.collection("routines").whereEqualTo("userId", userId).get().await()
        for (routineDoc in routinesSnapshot.documents) {
            val exercisesSnapshot = db.collection("exercises")
                .whereEqualTo("routineId", routineDoc.id).get().await()
            for (exerciseDoc in exercisesSnapshot.documents) {
                exerciseDoc.reference.delete().await()
            }
            routineDoc.reference.delete().await()
        }
    }
}
