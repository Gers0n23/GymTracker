package com.gcordero.gymtracker.data.util

import android.util.Log
import com.gcordero.gymtracker.data.repository.ExerciseCatalogRepository
import com.gcordero.gymtracker.domain.model.CatalogExercise
import com.gcordero.gymtracker.domain.model.ExerciseType

/**
 * Pobla el catálogo global de ejercicios en Firestore.
 * Solo debe ejecutarse una vez (valida si el catálogo ya existe antes de insertar).
 *
 * Incluye todos los ejercicios del DataPopulator existente más ejercicios adicionales
 * con aliases en español, inglés y nombres de máquinas.
 */
object ExerciseCatalogSeeder {
    private const val TAG = "ExerciseCatalogSeeder"

    suspend fun seedIfEmpty(repository: ExerciseCatalogRepository): Result<Unit> {
        return try {
            if (repository.isCatalogPopulated()) {
                Log.d(TAG, "El catálogo ya está poblado. No se hace nada.")
                return Result.success(Unit)
            }
            Log.d(TAG, "Poblando catálogo de ejercicios...")
            val exercises = buildCatalog()
            exercises.forEach { repository.addExerciseToCatalog(it) }
            Log.d(TAG, "Catálogo poblado con ${exercises.size} ejercicios.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al poblar el catálogo: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun buildCatalog(): List<CatalogExercise> = listOf(

        // ============================================================
        // ESPALDA
        // ============================================================
        CatalogExercise(
            name = "Jalón al Pecho",
            muscleGroup = "Espalda",
            secondaryMuscles = listOf("Bíceps", "Romboides"),
            equipment = "Polea alta",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=jalon+al+pecho+tecnica+correcta+maquina",
            notes = "Retrae las escápulas antes de jalar. Saca el pecho al frente. Lleva los codos hacia las caderas, no hacia atrás. Baja la barra hasta el mentón. Controla la subida 2 segundos.",
            aliases = listOf("Lat Pulldown", "Jalón Dorsal", "Jalón en Polea", "Polea al Pecho", "Pull-down", "Pulldown", "Lat pull", "Jalón frontal")
        ),
        CatalogExercise(
            name = "Remo Sentado en Máquina",
            muscleGroup = "Espalda",
            secondaryMuscles = listOf("Bíceps", "Romboides", "Trapecio"),
            equipment = "Polea baja",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=remo+sentado+polea+maquina+tecnica",
            notes = "Pecho afuera, hombros hacia atrás y abajo. Jala hacia el ombligo, no el pecho. Codos cerca del cuerpo. Pausa 1 segundo detrás. No uses el torso para impulsar.",
            aliases = listOf("Seated Row", "Remo en Polea", "Cable Row", "Remo bajo", "Remo en máquina", "Remo en cable")
        ),
        CatalogExercise(
            name = "Jalón Agarre Estrecho",
            muscleGroup = "Espalda",
            secondaryMuscles = listOf("Bíceps", "Redondo mayor"),
            equipment = "Polea alta",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=jalon+agarre+estrecho+dorsales+tecnica",
            notes = "Agarre neutro si la máquina lo permite. Mayor énfasis en dorsal bajo y redondo mayor. Mismos principios del jalón ancho. Lleva el agarre al esternón con codos pegados.",
            aliases = listOf("Close Grip Pulldown", "Jalón agarre cerrado", "Jalón neutro", "Pulldown agarre estrecho")
        ),
        CatalogExercise(
            name = "Remo con Barra",
            muscleGroup = "Espalda",
            secondaryMuscles = listOf("Bíceps", "Isquiotibiales", "Glúteos"),
            equipment = "Barra",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=remo+con+barra+tecnica+correcta",
            notes = "Espalda recta, torso a 45°. Jala la barra hacia el abdomen. Codos deslizándose hacia atrás y arriba. Pausa 1 segundo arriba.",
            aliases = listOf("Barbell Row", "Remo inclinado", "Bent over row", "Remo libre", "Remo barra")
        ),
        CatalogExercise(
            name = "Remo con Mancuerna",
            muscleGroup = "Espalda",
            secondaryMuscles = listOf("Bíceps", "Romboides"),
            equipment = "Mancuernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=remo+mancuerna+un+brazo+tecnica",
            notes = "Apóyate en un banco. Jala la mancuerna hacia la cadera con el codo cerca del cuerpo. Pausa arriba y baja controlado.",
            aliases = listOf("Dumbbell Row", "Remo unilateral", "Single arm row", "Remo a un brazo", "Remo mancuerna")
        ),
        CatalogExercise(
            name = "Dominadas",
            muscleGroup = "Espalda",
            secondaryMuscles = listOf("Bíceps", "Core"),
            equipment = "Barra de dominadas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=dominadas+tecnica+correcta",
            notes = "Agarre pronado más ancho que hombros. Retrae escápulas antes de jalar. Sube hasta que el mentón supere la barra. Baja completamente.",
            aliases = listOf("Pull-ups", "Pullups", "Jalón en barra", "Barras", "Pull up")
        ),
        CatalogExercise(
            name = "Face Pull",
            muscleGroup = "Espalda",
            secondaryMuscles = listOf("Hombros", "Romboides", "Manguito rotador"),
            equipment = "Polea alta",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=face+pull+tecnica+correcta",
            notes = "Polea a la altura de la cara o superior. Jala hacia la nariz separando los codos. Rota los hombros hacia afuera al final.",
            aliases = listOf("Jalón a la cara", "Jalo facial", "Cable face pull", "Face pull polea")
        ),
        CatalogExercise(
            name = "Extensión Lumbar en Máquina",
            muscleGroup = "Lumbar",
            secondaryMuscles = listOf("Glúteos", "Isquiotibiales"),
            equipment = "Máquina lumbar",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=extension+lumbar+maquina+tecnica+espalda+baja",
            notes = "Movimiento suave y controlado, sin hiperextender. Aprieta la espalda baja suavemente al subir. Baja controlado. Usa poco peso.",
            aliases = listOf("Back extension", "Extensión de espalda baja", "Hiperextensión", "Lumbar extension", "Roman chair")
        ),

        // ============================================================
        // PECHO
        // ============================================================
        CatalogExercise(
            name = "Chest Press en Máquina",
            muscleGroup = "Pecho",
            secondaryMuscles = listOf("Tríceps", "Deltoides anterior"),
            equipment = "Máquina de pecho",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=chest+press+maquina+pecho+tecnica+correcta",
            notes = "Retrae las escápulas antes de empujar. Saca el pecho. Codos NO detrás del plano del cuerpo. Empuja fuerte, regresa lento 2-3 segundos.",
            aliases = listOf("Press de pecho en máquina", "Machine chest press", "Press plano máquina", "Press pecho sentado", "Peck deck press")
        ),
        CatalogExercise(
            name = "Press de Banca con Barra",
            muscleGroup = "Pecho",
            secondaryMuscles = listOf("Tríceps", "Deltoides anterior"),
            equipment = "Barra",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=press+de+banca+tecnica+correcta",
            notes = "Espalda levemente arqueada, pies en el suelo. Baja la barra al pecho medio-bajo. Empuja verticalmente. Escápulas retraídas y deprimidas.",
            aliases = listOf("Bench Press", "Press Plano", "Press Plano Barra", "Banco Plano", "Flat bench press", "Bench")
        ),
        CatalogExercise(
            name = "Press de Banca con Mancuernas",
            muscleGroup = "Pecho",
            secondaryMuscles = listOf("Tríceps", "Deltoides anterior"),
            equipment = "Mancuernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=press+de+banca+mancuernas+tecnica",
            notes = "Mayor rango de movimiento que con barra. Mancuernas se juntan arriba. Controla la bajada más allá del pecho. Nunca dejes las mancuernas caer.",
            aliases = listOf("Dumbbell Bench Press", "Press banca mancuernas", "Press plano con mancuernas", "Dumbbell press", "DB Press")
        ),
        CatalogExercise(
            name = "Press Inclinado con Barra",
            muscleGroup = "Pecho",
            secondaryMuscles = listOf("Deltoides anterior", "Tríceps"),
            equipment = "Barra",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=press+inclinado+barra+pecho+superior+tecnica",
            notes = "Banco a 30-45°. Baja la barra a la clavícula. Énfasis en pecho superior. Misma técnica de escápulas que press plano.",
            aliases = listOf("Incline Bench Press", "Press inclinado", "Incline press", "Press banco inclinado")
        ),
        CatalogExercise(
            name = "Pec Deck / Aperturas en Máquina",
            muscleGroup = "Pecho",
            secondaryMuscles = listOf("Deltoides anterior"),
            equipment = "Pec Deck",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=pec+deck+apertura+maquina+pecho+tecnica",
            notes = "Leve flexión en codos. Al cerrar, imagina abrazar un árbol. Aprieta el pecho 1 segundo. No abras más de lo que el hombro permita.",
            aliases = listOf("Pec Deck", "Aperturas máquina", "Butterfly", "Chest fly máquina", "Fly máquina", "Apertura de pecho")
        ),
        CatalogExercise(
            name = "Aperturas con Mancuernas",
            muscleGroup = "Pecho",
            secondaryMuscles = listOf("Deltoides anterior"),
            equipment = "Mancuernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=aperturas+mancuerna+pecho+tecnica+correcta",
            notes = "Leve flexión en codos durante todo el movimiento. Abre hasta sentir estiramiento del pecho. Cierra siguiendo un arco, no empujes.",
            aliases = listOf("Dumbbell Fly", "Chest Fly mancuernas", "Fly", "Apertura pecho mancuernas")
        ),
        CatalogExercise(
            name = "Cruce de Poleas",
            muscleGroup = "Pecho",
            secondaryMuscles = listOf("Deltoides anterior"),
            equipment = "Poleas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=cruce+de+poleas+pecho+cable+crossover+tecnica",
            notes = "Cruza las manos al frente para máxima contracción. Controla la apertura. Varía la altura de poleas para enfatizar partes distintas del pecho.",
            aliases = listOf("Cable Crossover", "Cross de poleas", "Cruce de cables", "Pec fly cable", "Cables pecho")
        ),

        // ============================================================
        // HOMBROS
        // ============================================================
        CatalogExercise(
            name = "Press de Hombros en Máquina",
            muscleGroup = "Hombros",
            secondaryMuscles = listOf("Tríceps", "Trapecio"),
            equipment = "Máquina de hombros",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=press+hombros+maquina+tecnica+deltoides",
            notes = "No arquees la espalda baja. Hombros abajo durante todo el movimiento. Empuja en línea recta. Core apretado para proteger la espalda baja.",
            aliases = listOf("Shoulder Press Machine", "Press militar máquina", "Overhead press máquina", "Press hombros sentado")
        ),
        CatalogExercise(
            name = "Press Militar con Barra",
            muscleGroup = "Hombros",
            secondaryMuscles = listOf("Tríceps", "Trapecio"),
            equipment = "Barra",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=press+militar+barra+tecnica+correcta",
            notes = "Barra baja desde la clavícula. Empuja verticalmente. Core muy apretado para proteger la columna. No bloquees codos arriba.",
            aliases = listOf("Military Press", "Overhead Press", "Press sobre cabeza", "OH Press", "Barbell shoulder press")
        ),
        CatalogExercise(
            name = "Press de Hombros con Mancuernas",
            muscleGroup = "Hombros",
            secondaryMuscles = listOf("Tríceps"),
            equipment = "Mancuernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=press+hombros+mancuernas+tecnica",
            notes = "Mancuernas a altura de orejas. Empuja hacia arriba y levemente al centro. Controla la bajada. No arquees la espalda.",
            aliases = listOf("Dumbbell Shoulder Press", "Dumbbell Overhead Press", "Arnold Press", "Press mancuernas hombro")
        ),
        CatalogExercise(
            name = "Elevaciones Laterales en Máquina",
            muscleGroup = "Hombros",
            secondaryMuscles = listOf("Trapecio"),
            equipment = "Máquina lateral",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=elevaciones+laterales+maquina+hombros+tecnica",
            notes = "Codos ligeramente doblados. Levanta hasta nivel de hombros. Codo ligeramente sobre la mano al subir. Baja lento.",
            aliases = listOf("Lateral raises machine", "Elevación lateral máquina", "Vuelos laterales máquina", "Shoulder lateral machine")
        ),
        CatalogExercise(
            name = "Elevaciones Laterales con Mancuernas",
            muscleGroup = "Hombros",
            secondaryMuscles = listOf("Trapecio"),
            equipment = "Mancuernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=elevaciones+laterales+mancuernas+hombros+tecnica",
            notes = "Codo ligeramente por encima de la mano. Levanta hasta el nivel del hombro. Baja 3 segundos. No uses impulso.",
            aliases = listOf("Lateral Raise", "Vuelos laterales", "Lateral raises", "Hombros laterales", "Elevaciones mancuernas")
        ),
        CatalogExercise(
            name = "Elevaciones Frontales",
            muscleGroup = "Hombros",
            secondaryMuscles = listOf("Pecho", "Deltoides anterior"),
            equipment = "Mancuernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=elevaciones+frontales+tecnica+correcta+hombros",
            notes = "Levanta al frente hasta la altura del hombro. Codos ligeramente flexionados. Control en la bajada.",
            aliases = listOf("Front Raise", "Elevaciones delanteras", "Front raise mancuernas", "Elevaciones frontales mancuernas")
        ),
        CatalogExercise(
            name = "Pájaros / Vuelos Posteriores",
            muscleGroup = "Hombros",
            secondaryMuscles = listOf("Romboides", "Trapecio"),
            equipment = "Mancuernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=vuelos+posteriores+mancuernas+tecnica+deltoides",
            notes = "Torso inclinado. Abre los brazos hacia los lados con codos ligeramente flexionados. Lleva la mirada al suelo.",
            aliases = listOf("Reverse Fly", "Pájaros", "Rear delt fly", "Vuelos posteriores", "Reverse delt fly")
        ),

        // ============================================================
        // BÍCEPS
        // ============================================================
        CatalogExercise(
            name = "Curl Bíceps en Máquina",
            muscleGroup = "Bíceps",
            secondaryMuscles = listOf("Braquial"),
            equipment = "Máquina de curl",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=curl+biceps+maquina+tecnica+correcta",
            notes = "Codos fijos sobre el soporte. Sin balanceo de torso. Contrae fuerte arriba 1 segundo. Baja lento 2-3 segundos.",
            aliases = listOf("Machine curl", "Curl en máquina", "Bicep machine curl", "Curl máquina bíceps", "Preacher curl máquina")
        ),
        CatalogExercise(
            name = "Curl Bíceps con Barra",
            muscleGroup = "Bíceps",
            secondaryMuscles = listOf("Braquial", "Braquiorradial"),
            equipment = "Barra",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=curl+biceps+barra+tecnica+correcta",
            notes = "Codos fijos a los costados. Sin balanceo de torso. Sube controlado y baja lento.",
            aliases = listOf("Barbell Curl", "Curl con barra", "Bicep curl barra", "Curl barra recta", "EZ bar curl")
        ),
        CatalogExercise(
            name = "Curl Polea Baja",
            muscleGroup = "Bíceps",
            secondaryMuscles = listOf("Braquial"),
            equipment = "Polea baja",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=curl+biceps+polea+baja+tecnica",
            notes = "Codos fijos a los costados. Supina las muñecas al subir. Controla hasta la extensión total. Parado derecho, sin arquear espalda.",
            aliases = listOf("Cable Curl", "Curl en cable", "Curl bíceps cable", "Low cable curl", "Curl polea")
        ),
        CatalogExercise(
            name = "Curl Martillo",
            muscleGroup = "Bíceps",
            secondaryMuscles = listOf("Braquiorradial", "Braquial"),
            equipment = "Mancuernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=curl+martillo+mancuernas+tecnica+correcta",
            notes = "Agarre neutro (palmas enfrentadas) durante todo el movimiento. Trabaja more el braquiorradial. Sin balanceo.",
            aliases = listOf("Hammer Curl", "Curl neutro", "Hammers", "Curl hammer", "Martillo bíceps")
        ),
        CatalogExercise(
            name = "Curl Scott",
            muscleGroup = "Bíceps",
            secondaryMuscles = listOf("Braquial"),
            equipment = "Predicador",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=curl+scott+predicador+tecnica+correcta",
            notes = "Codos sobre el pad, sin levantarlos. Baja hasta extensión completa. Sube de forma controlada. Contrae fuerte arriba.",
            aliases = listOf("Preacher Curl", "Curl predicador", "Scott curl", "Curl en predicador")
        ),
        CatalogExercise(
            name = "Curl con Mancuernas Alterno",
            muscleGroup = "Bíceps",
            secondaryMuscles = listOf("Braquial"),
            equipment = "Mancuernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=curl+mancuernas+alternos+biceps+tecnica",
            notes = "Alterna brazos manteniendo el no activo quieto. Supina la muñeca al subir. Codos fijos.",
            aliases = listOf("Alternating Dumbbell Curl", "Curl mancuernas alternos", "Dumbbell curl", "Curl alterno mancuernas")
        ),

        // ============================================================
        // TRÍCEPS
        // ============================================================
        CatalogExercise(
            name = "Extensión de Tríceps en Polea",
            muscleGroup = "Tríceps",
            secondaryMuscles = emptyList(),
            equipment = "Polea alta",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=extension+triceps+polea+pushdown+tecnica",
            notes = "Codos FIJOS a los costados. Empuja hacia abajo contrayendo el tríceps. Mantén 1 segundo extendido. Baja lento.",
            aliases = listOf("Tricep Pushdown", "Press de tríceps polea", "Cable pushdown", "Pushdown cuerda", "Pushdown barra", "Tríceps polea", "Extension triceps cable")
        ),
        CatalogExercise(
            name = "Fondos Asistidos en Máquina",
            muscleGroup = "Tríceps",
            secondaryMuscles = listOf("Pecho", "Deltoides anterior"),
            equipment = "Máquina de fondos",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=fondos+asistidos+maquina+triceps+pecho+tecnica",
            notes = "Para tríceps: cuerpo vertical, codos cerca del cuerpo. Para pecho: inclínate hacia adelante. Baja hasta 90°. Hombros abajo.",
            aliases = listOf("Assisted Dips", "Fondos en máquina", "Dips asistidos", "Dips máquina", "Machine dips")
        ),
        CatalogExercise(
            name = "Tríceps Francés",
            muscleGroup = "Tríceps",
            secondaryMuscles = emptyList(),
            equipment = "Barra EZ",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=triceps+frances+skull+crusher+tecnica",
            notes = "Acostado, baja la barra EZ hacia la frente con codos apuntando al techo. Codos fijos. Extiende completamente.",
            aliases = listOf("Skull Crusher", "EZ curl press", "French Press", "Triceps francés", "JM Press", "Lying tricep extension")
        ),
        CatalogExercise(
            name = "Extensión de Tríceps sobre Cabeza",
            muscleGroup = "Tríceps",
            secondaryMuscles = emptyList(),
            equipment = "Mancuernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=extension+triceps+sobre+cabeza+mancuerna+tecnica",
            notes = "Codos apuntando al techo, cerca de la cabeza. Baja la mancuerna detrás de la cabeza. Extiende sin mover los codos.",
            aliases = listOf("Overhead Tricep Extension", "Extensión tríceps alta", "Mancuerna sobre cabeza", "Tricep overhead")
        ),
        CatalogExercise(
            name = "Fondos en Paralelas",
            muscleGroup = "Tríceps",
            secondaryMuscles = listOf("Pecho", "Deltoides anterior"),
            equipment = "Paralelas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=fondos+paralelas+triceps+tecnica+correcta",
            notes = "Cuerpo vertical para enfatizar tríceps. Baja hasta que los brazos formen 90°. Hombros abajo en todo momento.",
            aliases = listOf("Dips", "Paralelas", "Fondos paralelas", "Tricep dips", "Bar dips")
        ),

        // ============================================================
        // CUÁDRICEPS
        // ============================================================
        CatalogExercise(
            name = "Prensa de Piernas",
            muscleGroup = "Cuádriceps",
            secondaryMuscles = listOf("Glúteos", "Isquiotibiales"),
            equipment = "Prensa de piernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=prensa+de+piernas+tecnica+correcta",
            notes = "Pies al ancho de hombros. Rodillas en línea con dedos. NUNCA bloquees las rodillas. Espalda baja pegada al respaldo. Baja hasta ~90° sin que la cadera se levante.",
            aliases = listOf("Leg Press", "Prensa piernas", "Leg press máquina", "Prensa", "Machine leg press")
        ),
        CatalogExercise(
            name = "Extensión de Cuádriceps",
            muscleGroup = "Cuádriceps",
            secondaryMuscles = emptyList(),
            equipment = "Máquina de extensión",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=extension+cuadriceps+maquina+tecnica",
            notes = "Alinea la rodilla con el eje de la máquina. Contrae completamente el cuádriceps arriba. Baja 2-3 segundos. Sin balanceo.",
            aliases = listOf("Leg Extension", "Extensión de piernas", "Cuádriceps máquina", "Quad extension", "Leg curl máquina frente")
        ),
        CatalogExercise(
            name = "Sentadilla",
            muscleGroup = "Cuádriceps",
            secondaryMuscles = listOf("Glúteos", "Isquiotibiales", "Core"),
            equipment = "Barra",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=sentadilla+con+barra+tecnica+correcta",
            notes = "Pies al ancho de hombros o más. Rodillas siguen la dirección de los pies. Baja hasta paralelo o más. Espalda neutra. Empuja a través de toda la planta.",
            aliases = listOf("Squat", "Back Squat", "Sentadilla trasera", "Squat con barra", "Squats", "Cuclillas")
        ),
        CatalogExercise(
            name = "Prensa de Piernas (Pies Altos)",
            muscleGroup = "Glúteos",
            secondaryMuscles = listOf("Isquiotibiales", "Cuádriceps"),
            equipment = "Prensa de piernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=prensa+piernas+pies+altos+gluteos+isquios+tecnica",
            notes = "Pies en la parte alta de la plataforma, ligeramente más abiertos. Traslada el énfasis a glúteos e isquiotibiales. Mayor rango de movimiento posible.",
            aliases = listOf("High feet leg press", "Prensa pies arriba", "Leg press glúteos", "Prensa isquios")
        ),
        CatalogExercise(
            name = "Zancadas",
            muscleGroup = "Cuádriceps",
            secondaryMuscles = listOf("Glúteos", "Isquiotibiales"),
            equipment = "Mancuernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=zancadas+lunges+tecnica+correcta",
            notes = "Rodilla trasera baja casi al suelo. Rodilla delantera no supera la punta del pie. Torso erguido. Empuja desde el talón delantero para volver.",
            aliases = listOf("Lunges", "Estocadas", "Split squat", "Lunge", "Zancada")
        ),

        // ============================================================
        // ISQUIOTIBIALES / FEMORAL
        // ============================================================
        CatalogExercise(
            name = "Curl Femoral",
            muscleGroup = "Isquiotibiales",
            secondaryMuscles = listOf("Gemelos"),
            equipment = "Máquina de curl femoral",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=curl+femoral+maquina+tecnica+correcta",
            notes = "Sin arquear la espalda baja. Contrae los isquiotibiales completamente. Baja lento y controlado. Cadera no se levanta.",
            aliases = listOf("Leg Curl", "Curl de piernas", "Curl isquiotibial", "Hamstring curl", "Femoral curl", "Curl bíceps femoral")
        ),
        CatalogExercise(
            name = "Peso Muerto Rumano",
            muscleGroup = "Isquiotibiales",
            secondaryMuscles = listOf("Glúteos", "Lumbar"),
            equipment = "Barra",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=peso+muerto+rumano+tecnica+correcta",
            notes = "Espalda plana en todo momento. Baja la barra deslizándola por las piernas. Siente el estiramiento en isquios. Rodillas ligeramente flexionadas.",
            aliases = listOf("Romanian Deadlift", "RDL", "Peso muerto piernas rectas", "Deadlift rumano", "Peso muerto isquios")
        ),
        CatalogExercise(
            name = "Peso Muerto",
            muscleGroup = "Isquiotibiales",
            secondaryMuscles = listOf("Glúteos", "Lumbar", "Trapecios", "Cuádriceps"),
            equipment = "Barra",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=peso+muerto+tecnica+correcta+principiantes",
            notes = "Espalda plana. Caderas bajas al inicio. Empuja el suelo al levantarte. Barra pegada al cuerpo. Bloqueo de caderas al final.",
            aliases = listOf("Deadlift", "Weight lifting", "Peso muerto estándar", "Conventional deadlift", "Levantamiento de peso")
        ),

        // ============================================================
        // GLÚTEOS
        // ============================================================
        CatalogExercise(
            name = "Hip Thrust en Máquina",
            muscleGroup = "Glúteos",
            secondaryMuscles = listOf("Isquiotibiales"),
            equipment = "Máquina hip thrust",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=hip+thrust+maquina+gluteos+tecnica+correcta",
            notes = "Empuja a través de los talones. Aprieta los glúteos FUERTE en la posición alta 1-2 segundos. Mentón al pecho. Rodillas al ancho de hombros.",
            aliases = listOf("Hip thrust máquina", "Elevación de cadera máquina", "Glute bridge máquina")
        ),
        CatalogExercise(
            name = "Hip Thrust con Barra",
            muscleGroup = "Glúteos",
            secondaryMuscles = listOf("Isquiotibiales"),
            equipment = "Barra",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=hip+thrust+barra+tecnica+correcta",
            notes = "Espalda apoyada en banco. Barra sobre las caderas con almohadilla. Empuja a través de los talones. Aprieta los glúteos al tope.",
            aliases = listOf("Hip thrust", "Glute bridge barra", "Hip thrust barra", "Barbell hip thrust", "Elevación de caderas")
        ),
        CatalogExercise(
            name = "Puente de Glúteos",
            muscleGroup = "Glúteos",
            secondaryMuscles = listOf("Core"),
            equipment = "Suelo",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=puente+de+gluteos+tecnica+correcta+activacion",
            notes = "Pies apoyados planos, al ancho de los hombros. Empuja a través de los talones. Aprieta los glúteos en la parte alta 1-2 segundos.",
            aliases = listOf("Glute Bridge", "Elevación glúteos suelo", "Bridge gluteos", "Puente de glute")
        ),
        CatalogExercise(
            name = "Abductores en Máquina",
            muscleGroup = "Abductores",
            secondaryMuscles = listOf("Glúteos medios"),
            equipment = "Máquina abductores",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=abductores+aductores+maquina+tecnica+correcta",
            notes = "Movimiento controlado, sin impulso ni rebotes. Pausa 1 segundo en posición abierta. La pelvis no debe rotar.",
            aliases = listOf("Hip Abduction", "Aductores", "Abductores máquina", "Outer thigh machine", "Abductor machine", "Aductor máquina")
        ),
        CatalogExercise(
            name = "Abductores / Aductores en Máquina",
            muscleGroup = "Abductores",
            secondaryMuscles = listOf("Glúteos medios", "Aductores"),
            equipment = "Máquina abductores",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=abductores+aductores+maquina+tecnica+correcta",
            notes = "Para abductores: abre las piernas controlado, pausa. Para aductores: cierra lento, sin rebote. Pelvis neutra.",
            aliases = listOf("Inner outer thigh", "Adductor abductor machine", "Máquina piernas", "Aductor abductor")
        ),

        // ============================================================
        // GEMELOS
        // ============================================================
        CatalogExercise(
            name = "Gemelos en Máquina",
            muscleGroup = "Gemelos",
            secondaryMuscles = listOf("Sóleo"),
            equipment = "Máquina de gemelos",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=gemelos+maquina+calf+raise+tecnica",
            notes = "Rango de movimiento COMPLETO: baja hasta sentir el estiramiento total. Sube lo más alto posible. Pausa 1 segundo abajo. Sin rebotar.",
            aliases = listOf("Calf Raise Machine", "Elevaciones de talones máquina", "Standing calf raise", "Gemelos sentado máquina", "Pantorrillas máquina")
        ),
        CatalogExercise(
            name = "Gemelos en Prensa",
            muscleGroup = "Gemelos",
            secondaryMuscles = listOf("Sóleo"),
            equipment = "Prensa de piernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=gemelos+prensa+piernas+calf+raise+tecnica",
            notes = "Solo los antepies apoyados en la plataforma. Extiende y flexiona el tobillo en rango completo. Pausa en la posición estirada.",
            aliases = listOf("Calf raise en prensa", "Gemelos prensa", "Leg press calf raise", "Pantorrilla prensa")
        ),

        // ============================================================
        // CORE
        // ============================================================
        CatalogExercise(
            name = "Plancha Frontal",
            muscleGroup = "Core",
            secondaryMuscles = listOf("Hombros", "Glúteos"),
            equipment = "Suelo",
            exerciseType = ExerciseType.TIMED.name,
            mediaUrl = "https://www.youtube.com/results?search_query=plancha+frontal+tecnica+correcta+core",
            notes = "Cuerpo en línea recta. Aprieta glúteos y abdomen. Empuja el suelo con los antebrazos. Cabeza neutra. Respira constantemente.",
            aliases = listOf("Plank", "Plancha abdominal", "Forearm plank", "Plancha prona", "Tabla", "Core plank")
        ),
        CatalogExercise(
            name = "Plancha Lateral",
            muscleGroup = "Core",
            secondaryMuscles = listOf("Hombros", "Glúteos medios"),
            equipment = "Suelo",
            exerciseType = ExerciseType.TIMED.name,
            mediaUrl = "https://www.youtube.com/results?search_query=plancha+lateral+tecnica+correcta+oblicuos",
            notes = "Caderas completamente alineadas. Cuerpo en línea recta. Aprieta los oblicuos activamente. Respira constantemente.",
            aliases = listOf("Side Plank", "Plancha lateral oblicuos", "Lateral plank", "Plancha de lado")
        ),
        CatalogExercise(
            name = "Bird Dog",
            muscleGroup = "Core",
            secondaryMuscles = listOf("Lumbar", "Glúteos"),
            equipment = "Suelo",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=bird+dog+ejercicio+core+tecnica",
            notes = "Columna neutra. Brazo y pierna contraria simultáneamente, lento 3 seg. Pausa 2 segundos. Mira al suelo.",
            aliases = listOf("Bird dog", "Perro apuntador", "Cuadrupedia extensión", "Quadruped extension")
        ),
        CatalogExercise(
            name = "Bird Dog Pausado",
            muscleGroup = "Core",
            secondaryMuscles = listOf("Lumbar", "Glúteos"),
            equipment = "Suelo",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=bird+dog+pausado+core+tecnica",
            notes = "Igual que el bird dog estándar, pero mantén la posición extendida 3-5 segundos. Más lento y deliberado.",
            aliases = listOf("Bird dog isométrico", "Slow bird dog", "Perro apuntador pausado")
        ),
        CatalogExercise(
            name = "Dead Bug",
            muscleGroup = "Core",
            secondaryMuscles = listOf("Lumbar"),
            equipment = "Suelo",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=dead+bug+ejercicio+core+tecnica+correcta",
            notes = "Espalda baja PEGADA al suelo. Baja brazo y pierna opuesta simultáneamente, lento. Exhala al bajar. Pausa 1 segundo extendido.",
            aliases = listOf("Gusano muerto", "Dead bug core", "Insecto muerto")
        ),
        CatalogExercise(
            name = "Pallof Press en Polea",
            muscleGroup = "Core",
            secondaryMuscles = listOf("Hombros", "Glúteos"),
            equipment = "Polea",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=pallof+press+polea+core+antirotacion+tecnica",
            notes = "De lado a la polea, pies al ancho de hombros. El objetivo es NO rotar. Glúteos apretados. Empieza con poco peso.",
            aliases = listOf("Pallof press", "Press antirotación", "Anti-rotation press", "Cable antirotation")
        ),
        CatalogExercise(
            name = "Crunch en Polea",
            muscleGroup = "Core",
            secondaryMuscles = emptyList(),
            equipment = "Polea alta",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=crunch+en+polea+cable+crunch+tecnica",
            notes = "De rodillas, agarra la cuerda detrás de la cabeza. Dobla SOLO la columna. No jales con brazos ni hombros. 1 segundo contraído. Baja lento.",
            aliases = listOf("Cable crunch", "Crunch polea", "Abdominal polea", "Abs cable", "Crunch en cable")
        ),
        CatalogExercise(
            name = "Elevación de Rodillas en Banco",
            muscleGroup = "Core",
            secondaryMuscles = listOf("Flexores de cadera"),
            equipment = "Banco",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=elevacion+rodillas+banco+abdominales+tecnica",
            notes = "Sin impulso. Sube las rodillas contrayendo el abdomen. Baja lento. Espalda baja apoyada en el banco.",
            aliases = listOf("Knee raise", "Elevación de rodillas", "Hanging knee raise", "Elevación piernas banco")
        ),
        CatalogExercise(
            name = "Crunch",
            muscleGroup = "Core",
            secondaryMuscles = emptyList(),
            equipment = "Suelo",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=crunch+abdominal+tecnica+correcta",
            notes = "Solo flexiona la parte superior de la columna. No jalones el cuello con las manos. Exhala al contraer.",
            aliases = listOf("Abdominal crunch", "Abdominal", "Crunch abdominal", "Sit up", "Abdominales")
        ),

        // ============================================================
        // CARDIO
        // ============================================================
        CatalogExercise(
            name = "Trotadora",
            muscleGroup = "Cardio",
            secondaryMuscles = emptyList(),
            equipment = "Cardio",
            exerciseType = ExerciseType.CARDIO.name,
            mediaUrl = "https://www.youtube.com/results?search_query=cardio+moderado+eliptica+bici+cinta+tecnica",
            notes = "Intensidad moderada: debes poder mantener una conversación. Espalda erguida, hombros relajados.",
            aliases = listOf("Caminadora", "Cinta", "Treadmill", "Running", "Correr", "Cardio cinta")
        ),
        CatalogExercise(
            name = "Elíptica",
            muscleGroup = "Cardio",
            secondaryMuscles = emptyList(),
            equipment = "Cardio",
            exerciseType = ExerciseType.CARDIO.name,
            mediaUrl = "https://www.youtube.com/results?search_query=eliptica+tecnica+correcta+cardio",
            notes = "Intensidad moderada. Usa también los brazos para trabajo de cuerpo completo. Postura erguida.",
            aliases = listOf("Elliptical", "Eliptico", "Cross trainer", "Elíptico")
        ),
        CatalogExercise(
            name = "Bicicleta Estática",
            muscleGroup = "Cardio",
            secondaryMuscles = emptyList(),
            equipment = "Cardio",
            exerciseType = ExerciseType.CARDIO.name,
            mediaUrl = "https://www.youtube.com/results?search_query=bicicleta+estatica+tecnica+correcta",
            notes = "Ajusta el asiento para que la rodilla quede con leve flexión al extender. Pedales completos.",
            aliases = listOf("Stationary bike", "Bici estática", "Spinning", "Bike", "Cycling", "Bici")
        ),
        CatalogExercise(
            name = "Remo Ergómetro",
            muscleGroup = "Cardio",
            secondaryMuscles = listOf("Espalda", "Piernas"),
            equipment = "Cardio",
            exerciseType = ExerciseType.CARDIO.name,
            mediaUrl = "https://www.youtube.com/results?search_query=rowing+machine+rowing+ergometer+tecnica",
            notes = "Secuencia: piernas-torso-brazos al jalar. Brazos-torso-piernas al volver. No encorves la espalda.",
            aliases = listOf("Rowing machine", "Remo máquina", "Ergómetro de remo", "Rowing ergometro")
        ),

        // ============================================================
        // FUNCIONAL / LIBRE
        // ============================================================
        CatalogExercise(
            name = "Sentadilla Goblet",
            muscleGroup = "Cuádriceps",
            secondaryMuscles = listOf("Glúteos", "Core"),
            equipment = "Mancuernas",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=sentadilla+goblet+tecnica+correcta",
            notes = "Sostén la mancuerna a la altura del pecho. Pies al ancho de hombros. Baja manteniendo el torso erguido. Codos empujan las rodillas.",
            aliases = listOf("Goblet Squat", "Sentadilla copa", "Squat copa", "Goblet")
        ),
        CatalogExercise(
            name = "Peso Muerto Sumo",
            muscleGroup = "Isquiotibiales",
            secondaryMuscles = listOf("Glúteos", "Aductores", "Lumbar"),
            equipment = "Barra",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=peso+muerto+sumo+tecnica+correcta",
            notes = "Pies más separados, puntas hacia afuera. Rodillas siguen los pies. Espalda plana. La barra recorre menos distancia.",
            aliases = listOf("Sumo Deadlift", "Deadlift sumo", "Peso muerto pies abiertos")
        ),
        CatalogExercise(
            name = "Hip Thrust Unilateral",
            muscleGroup = "Glúteos",
            secondaryMuscles = listOf("Core"),
            equipment = "Suelo",
            exerciseType = ExerciseType.STRENGTH.name,
            mediaUrl = "https://www.youtube.com/results?search_query=hip+thrust+unilateral+una+pierna+tecnica",
            notes = "Una sola pierna empuja. Mantén la pelvis nivelada. El glúteo de la pierna que trabaja debe contraerse fuertemente.",
            aliases = listOf("Single leg hip thrust", "Hip thrust una pierna", "Glute bridge unilateral")
        )
    )
}
