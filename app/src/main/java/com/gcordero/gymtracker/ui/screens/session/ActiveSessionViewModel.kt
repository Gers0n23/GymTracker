package com.gcordero.gymtracker.ui.screens.session

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gcordero.gymtracker.data.repository.ExerciseRepository
import com.gcordero.gymtracker.data.repository.RoutineRepository
import com.gcordero.gymtracker.data.repository.WorkoutRepository
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.gcordero.gymtracker.ui.navigation.SessionHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

class ActiveSessionViewModel(
    application: Application,
    private val exerciseRepository: ExerciseRepository = ExerciseRepository(),
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val routineRepository: RoutineRepository = RoutineRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AndroidViewModel(application) {

    private val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _sets = MutableStateFlow<Map<String, List<SetRecord>>>(emptyMap())
    val sets: StateFlow<Map<String, List<SetRecord>>> = _sets.asStateFlow()

    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex: StateFlow<Int> = _currentExerciseIndex.asStateFlow()

    private val _currentSetNumber = MutableStateFlow(1)
    val currentSetNumber: StateFlow<Int> = _currentSetNumber.asStateFlow()

    private val _isResting = MutableStateFlow(false)
    val isResting: StateFlow<Boolean> = _isResting.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()

    private val _routineName = MutableStateFlow("")
    val routineName: StateFlow<String> = _routineName.asStateFlow()

    private val _lastSessionWeights = MutableStateFlow<Map<String, Double>>(emptyMap())
    val lastSessionWeights: StateFlow<Map<String, Double>> = _lastSessionWeights.asStateFlow()

    private val _isSessionSaved = MutableStateFlow(false)
    val isSessionSaved: StateFlow<Boolean> = _isSessionSaved.asStateFlow()

    // Controla qué pasa al terminar el descanso: "serie" = otra serie, "ejercicio" = siguiente ejercicio
    private val _restNextAction = MutableStateFlow("serie")
    val restNextAction: StateFlow<String> = _restNextAction.asStateFlow()

    // Mensaje motivacional / estado en la pantalla de descanso
    private val _restMessage = MutableStateFlow("")
    val restMessage: StateFlow<String> = _restMessage.asStateFlow()

    // Evento que se emite cuando el descanso termina naturalmente (para vibración/sonido)
    private val _restCompletedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val restCompletedEvent: SharedFlow<Unit> = _restCompletedEvent.asSharedFlow()

    // Se emite true si la sesión se restauró desde un borrador guardado
    private val _isDraftRestored = MutableStateFlow(false)
    val isDraftRestored: StateFlow<Boolean> = _isDraftRestored.asStateFlow()

    private val motivationalMessages = listOf(
        "¡Extraordinario! 💥",
        "¡Sigue así, crack! 🔥",
        "¡Gran serie! 💪",
        "¡Imparable! ⚡",
        "¡Eso es! 🎯",
        "¡Brutal! 🏆"
    )

    fun setRestNextAction(action: String) {
        _restNextAction.value = action
    }

    // Identificador de la rutina actual (necesario para persistencia del borrador)
    private var currentRoutineId: String = ""

    // Timestamp de inicio real de la sesión (ms desde epoch). Se restaura del borrador si existe.
    private var startTimeMs: Long = System.currentTimeMillis()

    // Tiempo objetivo exacto de finalización del descanso
    private var targetRestEndTimeMs: Long = 0L

    private var timerJob: Job? = null
    private var restTimerJob: Job? = null

    fun startSession(routineId: String) {
        // Si el ViewModel sobrevivió (e.g., volvió de YouTube), el estado sigue intacto
        if (_sets.value.isNotEmpty()) return

        currentRoutineId = routineId

        // Intentar restaurar un borrador guardado
        val draft = loadDraft(routineId)
        if (draft != null) {
            startTimeMs = draft.startTimeMs
            _currentExerciseIndex.value = draft.currentExerciseIndex
            _currentSetNumber.value = draft.currentSetNumber
            _sets.value = draft.sets
            // Calcular cuánto tiempo ha pasado realmente desde el inicio
            _timerSeconds.value = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
            _isDraftRestored.value = true
        } else {
            startTimeMs = System.currentTimeMillis()
        }

        viewModelScope.launch {
            val routine = routineRepository.getRoutineById(routineId)
            _routineName.value = routine?.name ?: "Entrenamiento"
        }

        val userId = auth.currentUser?.uid ?: "test_user"
        viewModelScope.launch {
            val lastWeights = workoutRepository.getLastWeightsByRoutine(routineId, userId)
            _lastSessionWeights.value = lastWeights

            exerciseRepository.getExercisesByRoutine(routineId).collect { exerciseList ->
                _exercises.value = exerciseList
                // Solo inicializar sets si no se restauró borrador ni sobrevivió el VM
                if (_sets.value.isEmpty()) {
                    val initialSets = mutableMapOf<String, List<SetRecord>>()
                    exerciseList.forEach { exercise ->
                        val lastWeight = lastWeights[exercise.id] ?: 0.0
                        initialSets[exercise.id] = listOf(
                            SetRecord(
                                exerciseId = exercise.id,
                                exerciseName = exercise.name,
                                setNumber = 1,
                                weight = lastWeight
                            )
                        )
                    }
                    _sets.value = initialSets
                }
            }
        }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timerSeconds.value++
            }
        }
    }

    private fun getRestAlarmPendingIntent(): PendingIntent {
        val intent = Intent(getApplication(), RestTimerReceiver::class.java)
        return PendingIntent.getBroadcast(
            getApplication(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleRestAlarm(delaySeconds: Int) {
        val triggerAt = SystemClock.elapsedRealtime() + delaySeconds * 1000L
        val pendingIntent = getRestAlarmPendingIntent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancelRestAlarm() {
        alarmManager.cancel(getRestAlarmPendingIntent())
    }

    fun startRestTimer() {
        restTimerJob?.cancel()
        val defaultRest = 90
        _restTimerSeconds.value = defaultRest
        _isResting.value = true

        val exercise = _exercises.value.getOrNull(_currentExerciseIndex.value)
        val completedSetNum = _currentSetNumber.value
        val targetSets = (exercise?.targetSets ?: 3).coerceAtLeast(1)

        val message = when {
            completedSetNum >= targetSets -> {
                _restNextAction.value = "ejercicio"
                "¡Terminaste! ¡Bien hecho! 🎉"
            }
            completedSetNum == targetSets - 1 -> "¡Falta solo 1 serie más! 💪"
            else -> motivationalMessages[(completedSetNum - 1).coerceAtLeast(0) % motivationalMessages.size]
        }
        _restMessage.value = message

        targetRestEndTimeMs = SystemClock.elapsedRealtime() + defaultRest * 1000L
        scheduleRestAlarm(defaultRest)

        startRestCoroutine()
    }

    private fun startRestCoroutine() {
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (true) {
                val remainingMs = targetRestEndTimeMs - SystemClock.elapsedRealtime()
                if (remainingMs <= 0) {
                    _restTimerSeconds.value = 0
                    break
                }
                _restTimerSeconds.value = (remainingMs / 1000).toInt() + 1
                delay(200) // Verificación frecuente para evitar deriva y ser preciso
            }
            onRestComplete(skipped = false)
        }
    }

    fun adjustRestTime(deltaSeconds: Int) {
        if (!_isResting.value) return
        val currentRemainingSeconds = _restTimerSeconds.value
        val newRemaining = (currentRemainingSeconds + deltaSeconds).coerceAtLeast(5)
        
        targetRestEndTimeMs = SystemClock.elapsedRealtime() + newRemaining * 1000L
        _restTimerSeconds.value = newRemaining
        
        cancelRestAlarm()
        scheduleRestAlarm(newRemaining)
    }

    fun skipRest() {
        restTimerJob?.cancel()
        cancelRestAlarm()
        onRestComplete(skipped = true)
    }

    private fun onRestComplete(skipped: Boolean = false) {
        if (!skipped) {
            val app = getApplication<Application>()
            RestTimerReceiver.vibrate(app)
            RestTimerReceiver.showNotification(app)
        }
        cancelRestAlarm()
        _isResting.value = false
        _restTimerSeconds.value = 0

        if (_restNextAction.value == "ejercicio") {
            nextExercise()
            _restNextAction.value = "serie"
            return
        }

        val exercise = _exercises.value.getOrNull(_currentExerciseIndex.value) ?: return
        val currentSets = _sets.value[exercise.id] ?: emptyList()
        val lastSet = currentSets.lastOrNull()

        _currentSetNumber.value += 1
        if (currentSets.size < _currentSetNumber.value) {
            addSet(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                initialWeight = lastSet?.weight ?: 0.0,
                initialReps = lastSet?.reps ?: 0,
                initialRir = lastSet?.rir,
                initialDurationSeconds = lastSet?.durationSeconds,
                initialSpeedKmh = lastSet?.speedKmh,
                initialInclinePercent = lastSet?.inclinePercent
            )
        }

        saveDraft(currentRoutineId)
    }

    fun nextExercise() {
        if (_currentExerciseIndex.value < _exercises.value.size - 1) {
            _currentExerciseIndex.value += 1
            returnToLatestSet()
            saveDraft(currentRoutineId)
        }
    }

    fun selectExercise(index: Int) {
        if (index in _exercises.value.indices) {
            val isRestingBefore = _isResting.value
            _currentExerciseIndex.value = index
            returnToLatestSet()
            saveDraft(currentRoutineId)
            
            // Si estaba descansando o no, no deberíamos cancelar el descanso al cambiar de ejercicio.
            // Pero si queremos que al tocar otro ejercicio cancele el descanso:
            if (isRestingBefore) {
                // skipRest()
                // Por ahora lo dejaremos seguir corriendo el descanso en background a menos que se quiera saltar.
            }
        }
    }

    fun selectSet(setNumber: Int) {
        val exercise = _exercises.value.getOrNull(_currentExerciseIndex.value) ?: return
        val currentSets = _sets.value[exercise.id] ?: emptyList()
        if (setNumber in 1..currentSets.size) {
            _currentSetNumber.value = setNumber
        }
    }

    fun returnToLatestSet() {
        val exercise = _exercises.value.getOrNull(_currentExerciseIndex.value) ?: return
        val currentSets = _sets.value[exercise.id] ?: emptyList()
        // El último elemento añadido en la lista de sets es precisamente el set actual en progreso
        _currentSetNumber.value = maxOf(1, currentSets.size)
    }

    fun addSet(
        exerciseId: String,
        exerciseName: String,
        initialWeight: Double = 0.0,
        initialReps: Int = 0,
        initialRir: Int? = null,
        initialDurationSeconds: Int? = null,
        initialSpeedKmh: Double? = null,
        initialInclinePercent: Double? = null
    ) {
        val currentSets = _sets.value.toMutableMap()
        val exerciseSets = currentSets[exerciseId]?.toMutableList() ?: mutableListOf()
        exerciseSets.add(
            SetRecord(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                setNumber = exerciseSets.size + 1,
                weight = initialWeight,
                reps = initialReps,
                rir = initialRir,
                durationSeconds = initialDurationSeconds,
                speedKmh = initialSpeedKmh,
                inclinePercent = initialInclinePercent
            )
        )
        currentSets[exerciseId] = exerciseSets
        _sets.value = currentSets
    }

    fun updateSet(exerciseId: String, index: Int, weight: Double, reps: Int, rir: Int?) {
        val currentSets = _sets.value.toMutableMap()
        val exerciseSets = currentSets[exerciseId]?.toMutableList() ?: return
        if (index < exerciseSets.size) {
            exerciseSets[index] = exerciseSets[index].copy(weight = weight, reps = reps, rir = rir)
            currentSets[exerciseId] = exerciseSets
            _sets.value = currentSets
            saveDraft(currentRoutineId)
        }
    }

    fun updateTimedSet(exerciseId: String, index: Int, durationSeconds: Int) {
        val currentSets = _sets.value.toMutableMap()
        val exerciseSets = currentSets[exerciseId]?.toMutableList() ?: return
        if (index < exerciseSets.size) {
            exerciseSets[index] = exerciseSets[index].copy(durationSeconds = durationSeconds)
            currentSets[exerciseId] = exerciseSets
            _sets.value = currentSets
            saveDraft(currentRoutineId)
        }
    }

    fun updateCardioSet(
        exerciseId: String,
        index: Int,
        speedKmh: Double,
        inclinePercent: Double,
        durationSeconds: Int
    ) {
        val currentSets = _sets.value.toMutableMap()
        val exerciseSets = currentSets[exerciseId]?.toMutableList() ?: return
        if (index < exerciseSets.size) {
            exerciseSets[index] = exerciseSets[index].copy(
                speedKmh = speedKmh,
                inclinePercent = inclinePercent,
                durationSeconds = durationSeconds
            )
            currentSets[exerciseId] = exerciseSets
            _sets.value = currentSets
            saveDraft(currentRoutineId)
        }
    }

    /** Abandona la sesión sin guardar: limpia el borrador y cancela timers. */
    fun abandonSession() {
        clearDraft(currentRoutineId)
        timerJob?.cancel()
        restTimerJob?.cancel()
        cancelRestAlarm()
    }

    /**
     * Estima calorías quemadas usando fórmulas MET por tipo de ejercicio (inferido de los campos):
     * - CARDIO (speedKmh != null): fórmula ACSM para cinta/cardio con velocidad e inclinación.
     * - TIMED (durationSeconds != null, speedKmh == null): MET fijo 3.5 (planchas, isométricos).
     * - STRENGTH: MET 5.0 aplicado al tiempo de sesión no cubierto por CARDIO/TIMED.
     */
    private fun calculateCaloriesBurned(
        allSets: List<SetRecord>,
        bodyWeightKg: Double,
        totalDurationMinutes: Double
    ): Int {
        var calories = 0.0
        val cardioSets = allSets.filter { it.speedKmh != null && it.durationSeconds != null }
        val timedSets  = allSets.filter { it.speedKmh == null && it.durationSeconds != null }

        // CARDIO — fórmula ACSM: VO2 (mL/kg/min) = 0.1*speed_m_min + 1.8*speed_m_min*grade + 3.5
        cardioSets.forEach { set ->
            val speedMMin = (set.speedKmh!! * 1000.0) / 60.0
            val grade     = (set.inclinePercent ?: 0.0) / 100.0
            val vo2       = 0.1 * speedMMin + 1.8 * speedMMin * grade + 3.5
            val met       = vo2 / 3.5
            calories += met * bodyWeightKg * (set.durationSeconds!! / 3600.0)
        }
        // TIMED — plancha/isométrico: MET 3.5
        timedSets.forEach { set ->
            calories += 3.5 * bodyWeightKg * (set.durationSeconds!! / 3600.0)
        }
        // STRENGTH — MET 5.0 para el tiempo restante de la sesión
        val cardioMinutes   = cardioSets.sumOf { it.durationSeconds!! } / 60.0
        val timedMinutes    = timedSets.sumOf  { it.durationSeconds!! } / 60.0
        val strengthMinutes = (totalDurationMinutes - cardioMinutes - timedMinutes).coerceAtLeast(0.0)
        calories += 5.0 * bodyWeightKg * (strengthMinutes / 60.0)

        return calories.toInt().coerceAtLeast(0)
    }

    fun finishSession(routineId: String) {
        val userId = auth.currentUser?.uid ?: "test_user"
        val allSets = _sets.value.values.flatten()
        val totalWeight = allSets.sumOf { it.weight * it.reps }
        val name = _routineName.value.ifEmpty { "Entrenamiento" }

        val durationMinutes = (System.currentTimeMillis() - startTimeMs) / 60000.0
        val prefs = getApplication<Application>()
            .getSharedPreferences("body_prefs_$userId", android.content.Context.MODE_PRIVATE)
        val bodyWeightKg = prefs.getFloat("latest_weight_kg", 70f).toDouble()
        val caloriesBurned = calculateCaloriesBurned(allSets, bodyWeightKg, durationMinutes)

        val session = WorkoutSession(
            userId = userId,
            routineId = routineId,
            routineName = name,
            startTime = Timestamp(Date(startTimeMs)),
            endTime = Timestamp.now(),
            totalWeightLifted = totalWeight,
            caloriesBurned = caloriesBurned
        )

        viewModelScope.launch {
            val sessionId = workoutRepository.saveWorkoutSession(session, allSets)
            clearDraft(routineId)
            SessionHolder.selectedSession = session.copy(id = sessionId)
            timerJob?.cancel()
            _isSessionSaved.value = true
        }
    }

    // =====================================================================
    // Persistencia de borrador en SharedPreferences (org.json)
    // Protege contra la muerte del proceso cuando Android mata la app.
    // =====================================================================

    private data class SessionDraft(
        val startTimeMs: Long,
        val currentExerciseIndex: Int,
        val currentSetNumber: Int,
        val sets: Map<String, List<SetRecord>>
    )

    private fun saveDraft(routineId: String) {
        if (routineId.isEmpty()) return
        val prefs = getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val setsJson = JSONObject()
            _sets.value.forEach { (exerciseId, setList) ->
                val setArray = JSONArray()
                setList.forEach { set ->
                    setArray.put(JSONObject().apply {
                        put("exerciseId", set.exerciseId)
                        put("exerciseName", set.exerciseName)
                        put("setNumber", set.setNumber)
                        put("weight", set.weight)
                        put("reps", set.reps)
                        if (set.rir != null) put("rir", set.rir)
                        if (set.durationSeconds != null) put("durationSeconds", set.durationSeconds)
                        if (set.speedKmh != null) put("speedKmh", set.speedKmh)
                        if (set.inclinePercent != null) put("inclinePercent", set.inclinePercent)
                    })
                }
                setsJson.put(exerciseId, setArray)
            }
            val json = JSONObject().apply {
                put("startTimeMs", startTimeMs)
                put("currentExerciseIndex", _currentExerciseIndex.value)
                put("currentSetNumber", _currentSetNumber.value)
                put("savedAt", System.currentTimeMillis())
                put("sets", setsJson)
            }
            prefs.edit().putString("draft_$routineId", json.toString()).apply()
        } catch (_: Exception) {
            // Fallo silencioso — el guardado del borrador es best-effort
        }
    }

    private fun loadDraft(routineId: String): SessionDraft? {
        val prefs = getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("draft_$routineId", null) ?: return null
        return try {
            val json = JSONObject(jsonStr)
            val savedAt = json.getLong("savedAt")
            // Solo restaurar borradores de las últimas 12 horas
            if (System.currentTimeMillis() - savedAt > 12 * 60 * 60 * 1000L) {
                clearDraft(routineId)
                return null
            }
            val startTimeMsVal = json.getLong("startTimeMs")
            val currentExerciseIndex = json.getInt("currentExerciseIndex")
            val currentSetNumber = json.getInt("currentSetNumber")
            val setsJson = json.getJSONObject("sets")
            val sets = mutableMapOf<String, List<SetRecord>>()
            setsJson.keys().forEach { exerciseId ->
                val setArray = setsJson.getJSONArray(exerciseId)
                val setList = mutableListOf<SetRecord>()
                for (i in 0 until setArray.length()) {
                    val s = setArray.getJSONObject(i)
                    setList.add(
                        SetRecord(
                            exerciseId = s.getString("exerciseId"),
                            exerciseName = s.getString("exerciseName"),
                            setNumber = s.getInt("setNumber"),
                            weight = s.getDouble("weight"),
                            reps = s.getInt("reps"),
                            rir = if (s.has("rir")) s.getInt("rir") else null,
                            durationSeconds = if (s.has("durationSeconds")) s.getInt("durationSeconds") else null,
                            speedKmh = if (s.has("speedKmh")) s.getDouble("speedKmh") else null,
                            inclinePercent = if (s.has("inclinePercent")) s.getDouble("inclinePercent") else null
                        )
                    )
                }
                sets[exerciseId] = setList
            }
            SessionDraft(startTimeMsVal, currentExerciseIndex, currentSetNumber, sets)
        } catch (_: Exception) {
            null
        }
    }

    private fun clearDraft(routineId: String) {
        if (routineId.isEmpty()) return
        val prefs = getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("draft_$routineId").apply()
    }

    companion object {
        private const val PREFS_NAME = "gymtracker_session_drafts"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return ActiveSessionViewModel(application) as T
            }
        }
    }
}
