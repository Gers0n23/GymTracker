package com.gcordero.gymtracker.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.theme.Glass
import com.gcordero.gymtracker.ui.theme.GlassBorder
import com.gcordero.gymtracker.ui.theme.Primary
import com.gcordero.gymtracker.ui.theme.Secondary
import coil.compose.AsyncImage

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
    viewModel: ActiveSessionViewModel = viewModel()
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

    val currentExercise = exercises.getOrNull(currentIndex)
    val currentExerciseSets = currentExercise?.let { sets[it.id] } ?: emptyList()
    val currentSet = currentExerciseSets.getOrNull(currentSetNum - 1)

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
                    val totalSets = maxOf(currentExerciseSets.size, currentSetNum)
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
                        onUpdateSet = { weight, reps, rir ->
                            viewModel.updateSet(currentExercise.id, currentSetNum - 1, weight, reps, rir)
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
            RestOverlay(
                secondsLeft = restTimer,
                onSkip = { viewModel.skipRest() },
                nextSetInfo = "Serie ${currentSetNum + 1} • ${currentSet?.weight ?: 0}kg",
                restNextAction = restNextAction,
                onToggleNextAction = { viewModel.setRestNextAction(it) }
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (isDone) {
                Text("✓", color = Secondary, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
            }
        }
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
    onUpdateSet: (Double, Int, Int?) -> Unit,
    onReady: () -> Unit,
    onFinishExercise: () -> Unit,
    onFinishSession: () -> Unit = {}
) {
    // Peso inicial: primero el guardado en el set actual,
    // si no hay, hereda de la serie anterior (misma sesión),
    // y si tampoco, queda vacío (ya fue prefilled desde la sesión anterior por el ViewModel)
    val initialWeight = (currentSetRecord?.weight?.takeIf { it > 0 }
        ?: previousSetRecord?.weight?.takeIf { it > 0 }
        ?: 0.0)
    val initialReps = (currentSetRecord?.reps?.takeIf { it > 0 }
        ?: previousSetRecord?.reps?.takeIf { it > 0 }
        ?: 0)

    var weightText by remember(exercise.id, setNumber) {
        mutableStateOf(if (initialWeight > 0) initialWeight.toString() else "")
    }
    var repsText by remember(exercise.id, setNumber) {
        mutableStateOf(if (initialReps > 0) initialReps.toString() else "")
    }
    var rir by remember(exercise.id, setNumber) { mutableStateOf(currentSetRecord?.rir ?: 2) }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        padding = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header del ejercicio
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (exercise.equipment.isNotEmpty()) {
                    Text(exercise.equipment.uppercase(), fontSize = 11.sp, color = Color.Gray, letterSpacing = 1.sp)
                }
                Text(
                    exercise.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Surface(
                color = Primary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "SERIE $setNumber DE $totalSets",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Inputs de peso y reps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FocusedInput(
                    label = "PESO (kg)",
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        onUpdateSet(it.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, rir)
                    },
                    modifier = Modifier.weight(1f)
                )
                FocusedInput(
                    label = "REPS",
                    value = repsText,
                    onValueChange = {
                        repsText = it
                        onUpdateSet(weightText.toDoubleOrNull() ?: 0.0, it.toIntOrNull() ?: 0, rir)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // RIR
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("RIR — Esfuerzo sobrante", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 1, 2, 3, 4).forEach { value ->
                        RirFocusButton(
                            value = value,
                            isSelected = rir == value,
                            onClick = {
                                rir = value
                                onUpdateSet(weightText.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, value)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ============================
            // ZONA DE ACCIONES
            // ============================
            if (isLastExercise) {
                // --- Último ejercicio: botón principal = TERMINAR ENTRENAMIENTO ---
                Button(
                    onClick = {
                        onUpdateSet(weightText.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, rir)
                        onFinishSession()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50) // verde vibrante
                    )
                ) {
                    Text(
                        "🏁  TERMINAR ENTRENAMIENTO",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
                // Opción secundaria: hacer otra serie antes de terminar
                TextButton(
                    onClick = {
                        onUpdateSet(weightText.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, rir)
                        onReady()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Hacer otra serie (iniciar descanso)",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 13.sp
                    )
                }
            } else {
                // --- Ejercicio normal: botón principal = LISTO / DESCANSO ---
                Button(
                    onClick = {
                        onUpdateSet(weightText.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, rir)
                        onReady()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        "✓  LISTO — INICIAR DESCANSO",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                // Opción secundaria: pasar al siguiente ejercicio sin descanso
                TextButton(
                    onClick = {
                        onUpdateSet(weightText.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, rir)
                        onFinishExercise()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Siguiente Ejercicio",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 13.sp
                    )
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
    onToggleNextAction: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212).copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
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

            Spacer(Modifier.height(32.dp))

            // Toggle: ¿Qué hacemos al terminar el descanso?
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 6.dp) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "AL TERMINAR EL DESCANSO",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Opción: otra serie
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (restNextAction == "serie") Primary
                                    else Glass
                                )
                                .border(
                                    1.dp,
                                    if (restNextAction == "serie") Primary else GlassBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onToggleNextAction("serie") }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "💪  Otra serie",
                                color = if (restNextAction == "serie") Color.Black else Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        // Opción: siguiente ejercicio
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (restNextAction == "ejercicio") Color(0xFF3D8EFF)
                                    else Glass
                                )
                                .border(
                                    1.dp,
                                    if (restNextAction == "ejercicio") Color(0xFF3D8EFF) else GlassBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onToggleNextAction("ejercicio") }
                                .padding(vertical = 12.dp),
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

            Spacer(Modifier.height(48.dp))

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
