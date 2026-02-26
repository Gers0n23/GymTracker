package com.gcordero.gymtracker.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcordero.gymtracker.data.repository.DashboardRepository
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

// ── muscleGroup Spanish name → internal ID ────────────────────────────────────
private val MUSCLE_ID_MAP = mapOf(
    "Espalda"        to "back",
    "Bíceps"         to "biceps",
    "Core"           to "core",
    "Cuádriceps"     to "quads",
    "Isquiotibiales" to "hamstrings",
    "Gemelos"        to "calves",
    "Glúteos"        to "glutes",
    "Hombros"        to "shoulders",
    "Pecho"          to "chest",
    "Tríceps"        to "triceps",
    "Lumbar"         to "back",        // merge into back
    "Abductores"     to "glutes"       // merge into glutes
    // "Cardio" intentionally absent → maps to null → skipped
)

private val MUSCLE_DISPLAY_NAMES = mapOf(
    "back"       to "Espalda",
    "biceps"     to "Bíceps",
    "core"       to "Core / Abdomen",
    "quads"      to "Cuádriceps",
    "hamstrings" to "Isquiotibiales",
    "calves"     to "Gemelos",
    "glutes"     to "Glúteos",
    "shoulders"  to "Hombros",
    "chest"      to "Pecho",
    "triceps"    to "Tríceps"
)

// Which muscles are targeted today based on day-of-week (Calendar constants)
private val TODAY_MUSCLES_BY_DOW = mapOf(
    Calendar.MONDAY    to setOf("back", "biceps", "core"),
    Calendar.TUESDAY   to setOf("quads", "glutes", "calves"),
    Calendar.WEDNESDAY to setOf("core"),
    Calendar.THURSDAY  to setOf("chest", "triceps", "shoulders", "core"),
    Calendar.FRIDAY    to setOf("hamstrings", "glutes", "calves")
)

// Fallback static tips per day (used when there's not enough history)
private val FALLBACK_TIPS_BY_DOW = mapOf(
    Calendar.MONDAY to listOf(
        SmartTip("Jalón al Pecho",      "Sube 2.5 kg — completa todas las reps para seguir progresando",  TipType.WEIGHT),
        SmartTip("Curl Bíceps",         "Aumenta a 12 reps — si llegas fácil, es hora de progresar",      TipType.REPS),
        SmartTip("Remo Sentado",        "Añade 1 serie extra — el volumen en espalda consolida la fuerza", TipType.SETS)
    ),
    Calendar.TUESDAY to listOf(
        SmartTip("Prensa de Piernas",       "Sube 5 kg — la prensa admite cargas altas con buena técnica",    TipType.WEIGHT),
        SmartTip("Extensión de Cuádriceps", "Aumenta a 12 reps — cuádriceps responden bien al alto volumen",  TipType.REPS),
        SmartTip("Curl Femoral",            "Añade 1 serie — los isquios necesitan volumen para desarrollarse",TipType.SETS)
    ),
    Calendar.WEDNESDAY to listOf(
        SmartTip("Plancha Frontal",     "Extiende a 45 s — tu resistencia de core ha mejorado notablemente", TipType.WEIGHT),
        SmartTip("Dead Bug",            "Aumenta a 10 reps por lado — controla bien la espalda baja",        TipType.REPS),
        SmartTip("Crunch en Polea",     "Añade 1 serie extra — mayor volumen abdominal refuerza la postura",  TipType.SETS)
    ),
    Calendar.THURSDAY to listOf(
        SmartTip("Chest Press en Máquina",      "Sube 2.5 kg — pecho responde bien a la progresión de carga",     TipType.WEIGHT),
        SmartTip("Press de Hombros en Máquina", "Aumenta a 10 reps — hombros resistentes protegen el manguito",   TipType.REPS),
        SmartTip("Extensión de Tríceps",        "Añade 1 serie extra — tríceps necesitan volumen para crecer",     TipType.SETS)
    ),
    Calendar.FRIDAY to listOf(
        SmartTip("Prensa (Pies Altos)",  "Sube 5 kg — mayor carga en glúteos con buena ejecución",             TipType.WEIGHT),
        SmartTip("Hip Thrust",           "Aumenta a 12 reps — glúteos responden especialmente bien al volumen", TipType.REPS),
        SmartTip("Curl Femoral",         "Añade 1 serie — la excéntrica del curl femoral es muy efectiva",      TipType.SETS)
    )
)

class DashboardViewModel(
    private val repo: DashboardRepository = DashboardRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val userId get() = auth.currentUser?.uid ?: "test_user"

    // ── Dashboard data state ──────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val now = Calendar.getInstance()
            val calDow = now.get(Calendar.DAY_OF_WEEK)  // Calendar.MONDAY = 2
            val routineDow = calDow - 1  // DataPopulator: 1=Mon … 5=Fri

            // 1. User name
            // Firestore puede tardar en crearse tras el primer login con Google,
            // usamos el displayName de Firebase Auth como fallback inmediato.
            val user = repo.getUser(userId)
            val userName = user?.name?.ifBlank { null }
                ?: auth.currentUser?.displayName?.ifBlank { null }
                ?: "Entrenador"

            // 2. Greeting
            val greeting = computeGreeting(now.get(Calendar.HOUR_OF_DAY))

            // 3. Sessions from last 28 days
            val since28 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -28) }
            val since28Timestamp = Timestamp(since28.time)
            val recentSessions = repo.getRecentSessions(userId, since28Timestamp)

            // 4. Routines
            val routines = repo.getUserRoutines(userId)
            val todayRoutine = routines.firstOrNull { it.daysOfWeek.contains(routineDow) }

            // 5. All exercises (for muscleGroup mapping)
            val allRoutineIds = routines.map { it.id }.filter { it.isNotEmpty() }
            val allExercises = repo.getExercisesForRoutines(allRoutineIds)

            // 6. Exercises for today's routine (hero card)
            val todayExercises = if (todayRoutine != null) {
                allExercises.filter { it.routineId == todayRoutine.id }
                    .sortedBy { it.order }
            } else emptyList()

            // 7. Set records for all sessions
            val sessionIds = recentSessions.map { it.id }
            val allSetRecords = repo.getSetRecordsForSessions(sessionIds)

            // 8. Compute everything
            val streak = computeStreak(recentSessions)
            val todayIdx = computeTodayIdx(calDow)
            val monday = getMondayOfCurrentWeek()

            val sessionsThisMonth = countSessionsThisMonth(recentSessions, now)
            val activeDaysThisWeek = countActiveDaysThisWeek(recentSessions, monday)
            val prsThisWeek = allSetRecords.count {
                it.isPersonalBest && it.timestamp.toDate() >= monday
            }

            val (weeklyBars, weeklyTotalKg, weeklyChangePercent) =
                computeWeeklyStats(recentSessions, calDow)

            val muscleGroups = computeMuscleData(
                sessions       = recentSessions,
                setRecords     = allSetRecords,
                exercises      = allExercises,
                todayMuscles   = TODAY_MUSCLES_BY_DOW[calDow] ?: emptySet()
            )

            val todayWorkout = buildTodayWorkout(calDow, todayRoutine, todayExercises)

            val smartTips = computeSmartTips(
                allSetRecords  = allSetRecords,
                sessions       = recentSessions,
                todayExercises = todayExercises.map { it.name },
                fallbackDow    = calDow
            )

            _uiState.value = DashboardUiState(
                isLoading           = false,
                userName            = userName,
                greeting            = greeting,
                streak              = streak,
                sessionsThisMonth   = sessionsThisMonth,
                prsThisWeek         = prsThisWeek,
                activeDaysThisWeek  = activeDaysThisWeek,
                activeDaysTarget    = 5,
                weeklyBarHeights    = weeklyBars,
                todayIdx            = todayIdx,
                weeklyTotalKg       = weeklyTotalKg,
                weeklyChangePercent = weeklyChangePercent,
                muscleGroups        = muscleGroups,
                todayWorkout        = todayWorkout,
                smartTips           = smartTips
            )
        }
    }

    // ── Computation helpers ───────────────────────────────────────────────────

    private fun computeGreeting(hour: Int): String = when {
        hour < 12 -> "Buenos días"
        hour < 19 -> "Buenas tardes"
        else      -> "Buenas noches"
    }

    /**
     * Counts how many consecutive days ending today (or yesterday) had at least
     * one session. Walks backwards day by day.
     */
    private fun computeStreak(sessions: List<WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0
        val sessionDays = sessions
            .map { stripToDate(it.startTime.toDate()) }
            .toSortedSet(reverseOrder())

        val today = stripToDate(Date())
        var current = today
        var streak = 0

        // Allow the streak to start from today or yesterday
        if (!sessionDays.contains(current)) {
            val yesterday = addDays(current, -1)
            if (!sessionDays.contains(yesterday)) return 0
            current = yesterday
        }

        while (sessionDays.contains(current)) {
            streak++
            current = addDays(current, -1)
        }
        return streak
    }

    /**
     * Returns a triple: (7-bar normalized heights, this-week total kg, % change vs last week)
     * Bars are indexed 0=Monday … 6=Sunday.
     */
    private fun computeWeeklyStats(
        sessions: List<WorkoutSession>,
        calDow: Int
    ): Triple<List<Float>, Double, Double?> {
        val monday = getMondayOfCurrentWeek()
        val lastMonday = addDays(monday, -7)

        // Volume per day index (0=Mon … 6=Sun)
        val volumeByDay = MutableList(7) { 0.0 }
        var thisWeekTotal = 0.0
        var lastWeekTotal = 0.0

        for (session in sessions) {
            val sessionDate = session.startTime.toDate()
            val sessionDow = Calendar.getInstance().apply { time = sessionDate }.get(Calendar.DAY_OF_WEEK)
            val dayIdx = (sessionDow - Calendar.MONDAY + 7) % 7  // 0=Mon … 6=Sun

            when {
                sessionDate >= monday -> {
                    volumeByDay[dayIdx] += session.totalWeightLifted
                    thisWeekTotal += session.totalWeightLifted
                }
                sessionDate >= lastMonday -> {
                    lastWeekTotal += session.totalWeightLifted
                }
            }
        }

        val max = volumeByDay.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val normalized = volumeByDay.mapIndexed { i, v ->
            val todayDayIdx = computeTodayIdx(calDow)
            if (i == todayDayIdx && v == 0.0) 0f  // today with no session → 0
            else (v / max).toFloat()
        }

        val changePercent = if (lastWeekTotal > 0)
            (thisWeekTotal - lastWeekTotal) / lastWeekTotal * 100.0
        else null

        return Triple(normalized, thisWeekTotal, changePercent)
    }

    /** 0 = Monday, 1 = Tuesday, … 6 = Sunday */
    private fun computeTodayIdx(calDow: Int): Int = (calDow - Calendar.MONDAY + 7) % 7

    private fun countSessionsThisMonth(
        sessions: List<WorkoutSession>,
        now: Calendar
    ): Int {
        val firstOfMonth = Calendar.getInstance().apply {
            set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        return sessions.count { it.startTime.toDate() >= firstOfMonth }
    }

    private fun countActiveDaysThisWeek(
        sessions: List<WorkoutSession>,
        monday: Date
    ): Int {
        return sessions
            .filter { it.startTime.toDate() >= monday }
            .map { stripToDate(it.startTime.toDate()) }
            .distinct()
            .size
    }

    private fun computeMuscleData(
        sessions: List<WorkoutSession>,
        setRecords: List<SetRecord>,
        exercises: List<Exercise>,
        todayMuscles: Set<String>
    ): Map<String, MuscleGroup> {
        val nowMs = System.currentTimeMillis()
        val weekMs = 7 * 24 * 60_000L * 60

        // Build exerciseName → muscleId map
        val exerciseToMuscleId: Map<String, String> = exercises
            .mapNotNull { ex ->
                val muscleId = MUSCLE_ID_MAP[ex.muscleGroup] ?: return@mapNotNull null
                ex.name to muscleId
            }
            .toMap()

        // Build sessionId → startTime map for quick lookup
        val sessionTimeMap: Map<String, Long> = sessions.associate {
            it.id to it.startTime.toDate().time
        }

        // Find last training timestamp per muscleId
        val lastTrainedMs = mutableMapOf<String, Long>()
        // Compute volumes per muscle per week (4 buckets, 0=oldest, 3=this week)
        val weeklyVolumes = mutableMapOf<String, MutableList<Int>>()

        for (setRecord in setRecords) {
            val muscleId = exerciseToMuscleId[setRecord.exerciseName] ?: continue
            val sessionMs = sessionTimeMap[setRecord.sessionId] ?: continue

            // Last trained
            val prev = lastTrainedMs[muscleId] ?: 0L
            if (sessionMs > prev) lastTrainedMs[muscleId] = sessionMs

            // Weekly volume bucket
            val weeksAgo = ((nowMs - sessionMs) / weekMs).toInt()
            if (weeksAgo < 4) {
                val bucketIdx = 3 - weeksAgo  // 0=3 weeks ago … 3=this week
                val vol = weeklyVolumes.getOrPut(muscleId) { mutableListOf(0, 0, 0, 0) }
                vol[bucketIdx] += (setRecord.weight * setRecord.reps).toInt()
            }
        }

        return MUSCLE_DISPLAY_NAMES.map { (muscleId, displayName) ->
            val lastMs = lastTrainedMs[muscleId]
            val hoursAgo = if (lastMs != null) (nowMs - lastMs) / 3_600_000L else null

            val status = when {
                lastMs != null && hoursAgo!! < 24 -> MuscleStatus.FATIGUED
                todayMuscles.contains(muscleId)   -> MuscleStatus.TODAY
                hoursAgo == null || hoursAgo > 48 -> MuscleStatus.READY
                else                               -> MuscleStatus.RECOVERING
            }

            val restInfo = when (status) {
                MuscleStatus.TODAY      -> "Objetivo de hoy — músculos frescos"
                MuscleStatus.READY      -> "Totalmente recuperado"
                MuscleStatus.RECOVERING -> "${48 - (hoursAgo ?: 0)}h para recuperación completa"
                MuscleStatus.FATIGUED   -> "Entrena en ~${24 - (hoursAgo ?: 0)}h — no recomendado hoy"
            }

            muscleId to MuscleGroup(
                id       = muscleId,
                name     = displayName,
                status   = status,
                restInfo = restInfo,
                volumes  = weeklyVolumes[muscleId]?.toList() ?: listOf(0, 0, 0, 0)
            )
        }.toMap()
    }

    private fun buildTodayWorkout(
        calDow: Int,
        routine: com.gcordero.gymtracker.domain.model.Routine?,
        exercises: List<Exercise>
    ): TodayWorkout? {
        if (routine == null) return null
        val dayNames = mapOf(
            Calendar.MONDAY    to "LUNES",
            Calendar.TUESDAY   to "MARTES",
            Calendar.WEDNESDAY to "MIÉRCOLES",
            Calendar.THURSDAY  to "JUEVES",
            Calendar.FRIDAY    to "VIERNES"
        )
        val dayTag = "${dayNames[calDow] ?: "HOY"} · HOY"
        val exerciseNames = exercises.map { it.name }
        val shown = exerciseNames.take(3)
        val extra = maxOf(0, exerciseNames.size - 3)
        // Estimate 7 min per exercise on average
        val estimatedMin = (exercises.size * 7).coerceAtLeast(30)

        return TodayWorkout(
            routineId        = routine.id,
            dayTag           = dayTag,
            routineName      = routine.name,
            exercises        = shown,
            extraCount       = extra,
            estimatedMinutes = estimatedMin
        )
    }

    private fun computeSmartTips(
        allSetRecords: List<SetRecord>,
        sessions: List<WorkoutSession>,
        todayExercises: List<String>,
        fallbackDow: Int
    ): List<SmartTip> {
        if (todayExercises.isEmpty()) return emptyList()

        // Build sessionId → date map
        val sessionDateMap = sessions.associate { it.id to it.startTime.toDate() }

        val tips = mutableListOf<SmartTip>()

        for (exerciseName in todayExercises) {
            if (tips.size >= 3) break

            // Get all set records for this exercise, grouped by session, sorted by date desc
            val bySession = allSetRecords
                .filter { it.exerciseName == exerciseName }
                .groupBy { it.sessionId }
                .entries
                .mapNotNull { (sid, sets) ->
                    val date = sessionDateMap[sid] ?: return@mapNotNull null
                    date to sets
                }
                .sortedByDescending { it.first }

            if (bySession.size < 2) continue  // Need at least 2 sessions to compare

            val lastSets  = bySession[0].second
            val prevSets  = bySession[1].second

            val lastMaxWeight = lastSets.maxOf { it.weight }
            val prevMaxWeight = prevSets.maxOf { it.weight }
            val lastAvgReps   = lastSets.map { it.reps }.average()

            val tip = when {
                // Weight progressed in last session
                lastMaxWeight > prevMaxWeight -> SmartTip(
                    exercise = exerciseName,
                    message  = "Subiste ${(lastMaxWeight - prevMaxWeight).toInt()} kg en la última sesión — ¡sigue así!",
                    type     = TipType.WEIGHT
                )
                // Same weight for 2+ sessions AND reps are good → suggest progress
                bySession.size >= 3 &&
                bySession[2].second.maxOf { it.weight } == lastMaxWeight &&
                lastAvgReps >= 10.0 -> SmartTip(
                    exercise = exerciseName,
                    message  = "Llevas 3 sesiones con ${lastMaxWeight.toInt()} kg — intenta subir 2.5 kg hoy",
                    type     = TipType.WEIGHT
                )
                // High reps → suggest more reps or a set
                lastAvgReps >= 12.0 -> SmartTip(
                    exercise = exerciseName,
                    message  = "Promedio ${lastAvgReps.toInt()} reps — considera subir peso o añadir una serie",
                    type     = TipType.REPS
                )
                else -> null
            }
            tip?.let { tips.add(it) }
        }

        return if (tips.isNotEmpty()) tips
        else FALLBACK_TIPS_BY_DOW[fallbackDow] ?: emptyList()
    }

    // ── Date helpers ──────────────────────────────────────────────────────────

    private fun getMondayOfCurrentWeek(): Date {
        return Calendar.getInstance().apply {
            val daysFromMonday = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_MONTH, -daysFromMonday)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun stripToDate(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun addDays(date: Date, days: Int): Date {
        return Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_MONTH, days)
        }.time
    }
}
