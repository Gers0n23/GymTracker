package com.gcordero.gymtracker.ui.screens.session

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.ExerciseType
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.theme.Glass
import com.gcordero.gymtracker.ui.theme.GlassBorder
import com.gcordero.gymtracker.ui.theme.Primary
import com.gcordero.gymtracker.ui.theme.Secondary

@Composable
fun TimerBadge(seconds: Int) {
    val mins = seconds / 60
    val secs = seconds % 60
    Box(
        modifier = Modifier
            .padding(end = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Primary.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "⏱️ %02d:%02d".format(mins, secs),
            color = Primary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    routineId: String,
    onFinish: () -> Unit,
    onAbandon: () -> Unit = {},
    viewModel: ActiveSessionViewModel = viewModel(factory = ActiveSessionViewModel.Factory)
) {
    val exercises by viewModel.exercises.collectAsState()
    val sets by viewModel.sets.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val restTimer by viewModel.restTimerSeconds.collectAsState()
    val isResting by viewModel.isResting.collectAsState()
    val currentIndex by viewModel.currentExerciseIndex.collectAsState()
    val currentSetNum by viewModel.currentSetNumber.collectAsState()
    val routineName by viewModel.routineName.collectAsState()
    val isSessionSaved by viewModel.isSessionSaved.collectAsState()
    val restNextAction by viewModel.restNextAction.collectAsState()
    val restMessage by viewModel.restMessage.collectAsState()
    val isDraftRestored by viewModel.isDraftRestored.collectAsState()

    val currentExercise = exercises.getOrNull(currentIndex)
    val currentExerciseSets = currentExercise?.let { sets[it.id] } ?: emptyList()
    val currentSet = currentExerciseSets.getOrNull(currentSetNum - 1)

    // Diálogo de confirmación al presionar Atrás
    var showAbandonDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = !isSessionSaved) {
        showAbandonDialog = true
    }

    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text("¿Abandonar entrenamiento?", color = Color.White) },
            text = {
                Text(
                    "Si salís ahora, el entrenamiento no se guardará.",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAbandonDialog = false
                        viewModel.abandonSession()
                        onAbandon()
                    }
                ) {
                    Text("Abandonar", color = Color(0xFFCF6679), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonDialog = false }) {
                    Text("Continuar entrenamiento", color = Primary)
                }
            },
            containerColor = Color(0xFF1E1E2E),
            tonalElevation = 0.dp
        )
    }

    // Solicitar permiso de notificaciones (Android 13+)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no action needed — notification will fire if granted */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // La vibración y notificación se manejan directamente desde el ViewModel (funciona en background)
    // El LaunchedEffect ya no es necesario para esto.

    LaunchedEffect(routineId) {
        viewModel.startSession(routineId)
    }

    // Navegar al resumen solo cuando el guardado en Firestore esté completo
    LaunchedEffect(isSessionSaved) {
        if (isSessionSaved) {
            onFinish()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(routineName, fontSize = 10.sp, color = Color.Gray)
                            Text("Sesión Activa", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TimerBadge(seconds = timerSeconds)
                            Button(
                                onClick = { viewModel.finishSession(routineId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFCF6679),
                                    disabledContainerColor = Color(0xFFCF6679).copy(alpha = 0.5f)
                                ),
                                enabled = !isSessionSaved,
                                modifier = Modifier.padding(end = 16.dp).height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSessionSaved) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("FINALIZAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Exercise Selector Card (altura fija)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 10.dp
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        exercises.forEachIndexed { index, ex ->
                            val isDone = index < currentIndex
                            val isActive = index == currentIndex
                            ExerciseSelectorPill(
                                name = ex.name,
                                isActive = isActive,
                                isDone = isDone,
                                onClick = { }
                            )
                        }
                    }
                }

                if (currentExercise != null) {
                    val targetSets = currentExercise.targetSets.coerceAtLeast(1)
                    val totalSets = maxOf(currentExerciseSets.size, currentSetNum, targetSets)
                    val previousSetRecord = currentExerciseSets.getOrNull(currentSetNum - 2)
                    val isLastExercise = currentIndex == exercises.size - 1
                    // Tarjeta principal ocupa todo el espacio restante
                    ActiveExerciseFocusCard(
                        exercise = currentExercise,
                        setNumber = currentSetNum,
                        totalSets = totalSets,
                        currentSetRecord = currentSet,
                        previousSetRecord = previousSetRecord,
                        isLastExercise = isLastExercise,
                        modifier = Modifier.weight(1f),
                        onUpdateStrengthSet = { weight, reps, rir ->
                            viewModel.updateSet(currentExercise.id, currentSetNum - 1, weight, reps, rir)
                        },
                        onUpdateTimedSet = { durationSeconds ->
                            viewModel.updateTimedSet(currentExercise.id, currentSetNum - 1, durationSeconds)
                        },
                        onUpdateCardioSet = { speedKmh, inclinePercent, durationSeconds ->
                            viewModel.updateCardioSet(currentExercise.id, currentSetNum - 1, speedKmh, inclinePercent, durationSeconds)
                        },
                        onReady = { viewModel.startRestTimer() },
                        onFinishExercise = { viewModel.nextExercise() },
                        onFinishSession = { viewModel.finishSession(routineId) }
                    )
                }
            }
        }

        // Rest Overlay
        if (isResting) {
            val nextExercise = exercises.getOrNull(currentIndex + 1)
            RestOverlay(
                secondsLeft = restTimer,
                onSkip = { viewModel.skipRest() },
                nextSetInfo = when (currentExercise?.type) {
                    ExerciseType.TIMED -> "Serie ${currentSetNum + 1} • ${currentSet?.durationSeconds ?: 0}s"
                    ExerciseType.CARDIO -> currentExercise.name
                    else -> "Serie ${currentSetNum + 1} • ${currentSet?.weight ?: 0}kg"
                },
                restNextAction = restNextAction,
                onToggleNextAction = { viewModel.setRestNextAction(it) },
                restMessage = restMessage,
                nextExerciseName = nextExercise?.name ?: "",
                nextExerciseMediaUrl = nextExercise?.mediaUrl ?: ""
            )
        }
    }
}

@Composable
fun ExerciseSelectorPill(
    name: String,
    isActive: Boolean,
    isDone: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isActive -> Primary.copy(alpha = 0.1f)
        isDone -> Secondary.copy(alpha = 0.05f)
        else -> Glass
    }
    val borderColor = when {
        isActive -> Primary
        isDone -> Secondary.copy(alpha = 0.4f)
        else -> GlassBorder
    }
    val textColor = when {
        isActive -> Color.White
        isDone -> Secondary.copy(alpha = 0.6f)
        else -> Color.White.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ActiveExerciseFocusCard(
    exercise: Exercise,
    setNumber: Int,
    totalSets: Int,
    currentSetRecord: SetRecord?,
    previousSetRecord: SetRecord? = null,
    isLastExercise: Boolean = false,
    modifier: Modifier = Modifier,
    onUpdateStrengthSet: (weight: Double, reps: Int, rir: Int?) -> Unit = { _, _, _ -> },
    onUpdateTimedSet: (durationSeconds: Int) -> Unit = { _ -> },
    onUpdateCardioSet: (speedKmh: Double, inclinePercent: Double, durationSeconds: Int) -> Unit = { _, _, _ -> },
    onReady: () -> Unit,
    onFinishExercise: () -> Unit,
    onFinishSession: () -> Unit = {}
) {
    val context = LocalContext.current
    val exerciseType = exercise.type
    val targetSets = exercise.targetSets.coerceAtLeast(1)
    val isLastTargetSet = setNumber >= targetSets

    // ---- Estado para STRENGTH ----
    val initialWeight = currentSetRecord?.weight?.takeIf { it > 0 }
        ?: previousSetRecord?.weight?.takeIf { it > 0 } ?: 0.0
    val initialReps = currentSetRecord?.reps?.takeIf { it > 0 }
        ?: previousSetRecord?.reps?.takeIf { it > 0 } ?: 0
    var weightText by remember(exercise.id, setNumber) {
        mutableStateOf(if (initialWeight > 0) initialWeight.toString() else "")
    }
    var repsText by remember(exercise.id, setNumber) {
        mutableStateOf(if (initialReps > 0) initialReps.toString() else "")
    }
    var rir by remember(exercise.id, setNumber) { mutableStateOf(currentSetRecord?.rir ?: 2) }

    // ---- Estado para TIMED ----
    val initialDuration = currentSetRecord?.durationSeconds
        ?: previousSetRecord?.durationSeconds ?: 0
    var durationText by remember(exercise.id, setNumber) {
        mutableStateOf(if (initialDuration > 0) initialDuration.toString() else "")
    }

    // ---- Estado para CARDIO ----
    val initialSpeed = currentSetRecord?.speedKmh ?: previousSetRecord?.speedKmh ?: 0.0
    val initialIncline = currentSetRecord?.inclinePercent ?: previousSetRecord?.inclinePercent ?: 0.0
    val initialCardioDuration = currentSetRecord?.durationSeconds ?: previousSetRecord?.durationSeconds ?: 0
    var speedText by remember(exercise.id, setNumber) {
        mutableStateOf(if (initialSpeed > 0) initialSpeed.toString() else "")
    }
    var inclineText by remember(exercise.id, setNumber) {
        mutableStateOf(if (initialIncline > 0) initialIncline.toString() else "")
    }
    var cardioDurationText by remember(exercise.id, setNumber) {
        mutableStateOf(if (initialCardioDuration > 0) initialCardioDuration.toString() else "")
    }

    var showTips by remember(exercise.id) { mutableStateOf(false) }

    GlassCard(modifier = modifier.fillMaxWidth(), padding = 0.dp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: subtítulo + nombre + botón de video
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val subtitle = buildList<String> {
                    if (exercise.equipment.isNotEmpty()) add(exercise.equipment)
                    if (exercise.muscleGroup.isNotEmpty()) add(exercise.muscleGroup)
                }.joinToString(" – ")
                if (subtitle.isNotEmpty()) {
                    Text(subtitle.uppercase(), fontSize = 11.sp, color = Color.Gray, letterSpacing = 1.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        exercise.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (exercise.notes.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if(showTips) Primary else Glass)
                                    .clickable { showTips = !showTips }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💡", fontSize = 14.sp)
                            }
                        }
                        if (exercise.mediaUrl.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFF0000))
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exercise.mediaUrl))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Ver video", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                if (showTips && exercise.notes.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Primary.copy(alpha = 0.15f))
                            .border(1.dp, Primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = exercise.notes,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            // Badge de serie (CARDIO muestra "SESIÓN DE CARDIO")
            when (exerciseType) {
                ExerciseType.CARDIO -> {
                    Surface(color = Color(0xFF1565C0), shape = RoundedCornerShape(12.dp)) {
                        Text(
                            "🏃  SESIÓN DE CARDIO",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                else -> {
                    Surface(
                        color = if (isLastTargetSet) Color(0xFF4CAF50) else Primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (isLastTargetSet) "★  SERIE $setNumber DE $totalSets" else "SERIE $setNumber DE $totalSets",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                            color = if (isLastTargetSet) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ============================
            // INPUTS SEGÚN TIPO
            // ============================
            when (exerciseType) {
                ExerciseType.STRENGTH -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        FocusedInput(
                            label = "PESO (kg)",
                            value = weightText,
                            onValueChange = {
                                weightText = it
                                onUpdateStrengthSet(it.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, rir)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FocusedInput(
                            label = "REPS",
                            value = repsText,
                            onValueChange = {
                                repsText = it
                                onUpdateStrengthSet(weightText.toDoubleOrNull() ?: 0.0, it.toIntOrNull() ?: 0, rir)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // RIR
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("RIR — Esfuerzo sobrante", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 1, 2, 3, 4).forEach { value ->
                                RirFocusButton(
                                    value = value,
                                    isSelected = rir == value,
                                    onClick = {
                                        rir = value
                                        onUpdateStrengthSet(weightText.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, value)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                ExerciseType.TIMED -> {
                    FocusedInput(
                        label = "DURACIÓN (segundos)",
                        value = durationText,
                        onValueChange = {
                            durationText = it
                            onUpdateTimedSet(it.toIntOrNull() ?: 0)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ExerciseType.CARDIO -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        FocusedInput(
                            label = "VELOCIDAD (km/h)",
                            value = speedText,
                            onValueChange = {
                                speedText = it
                                onUpdateCardioSet(
                                    it.toDoubleOrNull() ?: 0.0,
                                    inclineText.toDoubleOrNull() ?: 0.0,
                                    cardioDurationText.toIntOrNull() ?: 0
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FocusedInput(
                            label = "INCLINACIÓN (%)",
                            value = inclineText,
                            onValueChange = {
                                inclineText = it
                                onUpdateCardioSet(
                                    speedText.toDoubleOrNull() ?: 0.0,
                                    it.toDoubleOrNull() ?: 0.0,
                                    cardioDurationText.toIntOrNull() ?: 0
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FocusedInput(
                        label = "DURACIÓN (minutos)",
                        value = cardioDurationText,
                        onValueChange = {
                            cardioDurationText = it
                            onUpdateCardioSet(
                                speedText.toDoubleOrNull() ?: 0.0,
                                inclineText.toDoubleOrNull() ?: 0.0,
                                // Convertimos minutos a segundos al guardar
                                ((it.toDoubleOrNull() ?: 0.0) * 60).toInt()
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Distancia calculada
                    val speedVal = speedText.toDoubleOrNull() ?: 0.0
                    val durationMin = cardioDurationText.toDoubleOrNull() ?: 0.0
                    if (speedVal > 0 && durationMin > 0) {
                        val distKm = speedVal * (durationMin / 60.0)
                        Surface(
                            color = Color(0xFF1565C0).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "Distancia estimada: ${"%.2f".format(distKm)} km",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                color = Color(0xFF90CAF9),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ============================
            // ZONA DE ACCIONES
            // ============================
            when {
                exerciseType == ExerciseType.CARDIO -> {
                    Button(
                        onClick = {
                            onUpdateCardioSet(
                                speedText.toDoubleOrNull() ?: 0.0,
                                inclineText.toDoubleOrNull() ?: 0.0,
                                ((cardioDurationText.toDoubleOrNull() ?: 0.0) * 60).toInt()
                            )
                            if (isLastExercise) onFinishSession() else onFinishExercise()
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLastExercise) Color(0xFF4CAF50) else Color(0xFF1565C0)
                        )
                    ) {
                        Text(
                            if (isLastExercise) "🏁  FIN ENTRENAMIENTO" else "✓  TERMINAR CARDIO",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (!isLastExercise) {
                        TextButton(
                            onClick = { onFinishExercise() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Saltar ejercicio", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
                        }
                    }
                }

                isLastExercise && isLastTargetSet -> {
                    Button(
                        onClick = {
                            if (exerciseType == ExerciseType.TIMED)
                                onUpdateTimedSet(durationText.toIntOrNull() ?: 0)
                            else
                                onUpdateStrengthSet(weightText.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, rir)
                            onFinishSession()
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("🏁  FIN ENTRENAMIENTO", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 0.5.sp)
                    }
                    TextButton(
                        onClick = {
                            if (exerciseType == ExerciseType.TIMED)
                                onUpdateTimedSet(durationText.toIntOrNull() ?: 0)
                            else
                                onUpdateStrengthSet(weightText.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, rir)
                            onReady()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hacer otra serie (iniciar descanso)", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
                    }
                }

                else -> {
                    Button(
                        onClick = {
                            if (exerciseType == ExerciseType.TIMED)
                                onUpdateTimedSet(durationText.toIntOrNull() ?: 0)
                            else
                                onUpdateStrengthSet(weightText.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, rir)
                            onReady()
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("✓  LISTO — INICIAR DESCANSO", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    if (!isLastExercise) {
                        TextButton(
                            onClick = {
                                if (exerciseType == ExerciseType.TIMED)
                                    onUpdateTimedSet(durationText.toIntOrNull() ?: 0)
                                else
                                    onUpdateStrengthSet(weightText.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, rir)
                                onFinishExercise()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Siguiente Ejercicio", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun FocusedInput(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .border(2.dp, GlassBorder, RoundedCornerShape(16.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Glass,
                unfocusedContainerColor = Glass,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
    }
}

@Composable
fun RirFocusButton(value: Int, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Primary else Glass)
            .border(1.dp, if (isSelected) Primary else GlassBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (value == 4) "4+" else "$value",
            color = if (isSelected) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun RestOverlay(
    secondsLeft: Int,
    onSkip: () -> Unit,
    nextSetInfo: String,
    restNextAction: String = "serie",
    onToggleNextAction: (String) -> Unit = {},
    restMessage: String = "",
    nextExerciseName: String = "",
    nextExerciseMediaUrl: String = ""
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212).copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "DESCANSANDO",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 3.sp
            )
            Text(
                "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                fontSize = 96.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Primary
            )

            // Mensaje motivacional / de estado
            if (restMessage.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val msgColor = when {
                    restMessage.contains("Terminaste") -> Color(0xFF4CAF50)
                    restMessage.contains("Falta") -> Color(0xFFFFD600)
                    else -> Primary
                }
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 14.dp,
                    containerColor = msgColor.copy(alpha = 0.1f),
                    borderColor = msgColor.copy(alpha = 0.4f)
                ) {
                    Text(
                        restMessage,
                        color = msgColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(32.dp))
            }

            // Toggle: ¿Qué hacemos al terminar el descanso?
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "AL TERMINAR EL DESCANSO",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(25.dp))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(21.dp))
                                    .background(if (restNextAction == "serie") Primary else Color.Transparent)
                                    .clickable { onToggleNextAction("serie") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "💪  Otra serie",
                                    color = if (restNextAction == "serie") Color.Black else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(21.dp))
                                    .background(if (restNextAction == "ejercicio") Color(0xFF3D8EFF) else Color.Transparent)
                                    .clickable { onToggleNextAction("ejercicio") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "⏭  Sig. ejercicio",
                                    color = if (restNextAction == "ejercicio") Color.White else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Preview del siguiente ejercicio (solo visible cuando el toggle es "ejercicio")
            if (restNextAction == "ejercicio" && nextExerciseName.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                GlassCard(modifier = Modifier.fillMaxWidth(), padding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "SIGUIENTE EJERCICIO",
                                fontSize = 10.sp,
                                color = Color(0xFF3D8EFF),
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                nextExerciseName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        if (nextExerciseMediaUrl.isNotEmpty()) {
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFF0000))
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(nextExerciseMediaUrl))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Ver video del siguiente ejercicio",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onSkip) {
                Text(
                    "Saltar descanso",
                    color = Color.White.copy(alpha = 0.35f),
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }
        }
    }
}
