package com.gcordero.gymtracker.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.theme.Primary
import com.gcordero.gymtracker.ui.theme.Secondary
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    session: WorkoutSession,
    onBackClick: () -> Unit,
    viewModel: SessionDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val editableSets by viewModel.editableSets.collectAsState()

    LaunchedEffect(session.id) {
        viewModel.setSession(session)
        viewModel.loadSession(session.id)
    }

    // Snackbar on save success
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Cambios guardados correctamente")
            viewModel.clearSaveSuccess()
        }
    }

    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("es", "ES"))
    val dateStr = dateFormat.format(session.startTime.toDate())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            session.routineName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Text(
                            dateStr,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && editableSets.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveChanges() },
                    containerColor = Primary,
                    contentColor = Color.Black,
                    icon = {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    text = {
                        Text(
                            if (uiState.isSaving) "Guardando..." else "Guardar Cambios",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Primary
                    )
                }
                editableSets.isEmpty() -> {
                    Text(
                        "No hay series registradas en esta sesión.",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Summary header
                        item {
                            SessionSummaryCard(session = uiState.session ?: session)
                        }

                        // Exercise groups
                        editableSets.entries.forEach { (exerciseName, sets) ->
                            item {
                                ExerciseEditCard(
                                    exerciseName = exerciseName,
                                    sets = sets,
                                    onWeightChange = { idx, w ->
                                        viewModel.updateSetWeight(exerciseName, idx, w)
                                    },
                                    onRepsChange = { idx, r ->
                                        viewModel.updateSetReps(exerciseName, idx, r)
                                    },
                                    onRirChange = { idx, rir ->
                                        viewModel.updateSetRir(exerciseName, idx, rir)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionSummaryCard(session: WorkoutSession) {
    val durationMinutes = if (session.endTime != null) {
        val diffMs = session.endTime.toDate().time - session.startTime.toDate().time
        (diffMs / 60000).toInt()
    } else null

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Resumen de Sesión",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatItem(
                    label = "Volumen Total",
                    value = "${session.totalWeightLifted.toInt()} kg",
                    color = Primary
                )
                if (durationMinutes != null && durationMinutes > 0) {
                    SummaryStatItem(
                        label = "Duración",
                        value = "${durationMinutes} min",
                        color = Secondary
                    )
                }
                if (session.rpe != null) {
                    SummaryStatItem(
                        label = "RPE",
                        value = "${session.rpe}/10",
                        color = Color(0xFFFFD600)
                    )
                }
            }
            if (session.comments.isNotEmpty()) {
                Divider(color = Color.White.copy(alpha = 0.1f))
                Text(
                    session.comments,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SummaryStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun ExerciseEditCard(
    exerciseName: String,
    sets: List<SetRecord>,
    onWeightChange: (Int, Double) -> Unit,
    onRepsChange: (Int, Int) -> Unit,
    onRirChange: (Int, Int?) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                exerciseName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Divider(color = Color.White.copy(alpha = 0.1f))

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Serie", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(40.dp))
                Text("Peso (kg)", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(80.dp))
                Text("Reps", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(60.dp))
                Text("RIR", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(50.dp))
            }

            sets.forEachIndexed { index, set ->
                EditableSetRow(
                    setNumber = set.setNumber,
                    weight = set.weight,
                    reps = set.reps,
                    rir = set.rir,
                    onWeightChange = { onWeightChange(index, it) },
                    onRepsChange = { onRepsChange(index, it) },
                    onRirChange = { onRirChange(index, it) }
                )
            }
        }
    }
}

@Composable
fun EditableSetRow(
    setNumber: Int,
    weight: Double,
    reps: Int,
    rir: Int?,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onRirChange: (Int?) -> Unit
) {
    var weightText by remember(weight) { mutableStateOf(if (weight > 0) weight.toString() else "") }
    var repsText by remember(reps) { mutableStateOf(if (reps > 0) reps.toString() else "") }
    var rirText by remember(rir) { mutableStateOf(rir?.toString() ?: "") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Set number badge
        Surface(
            color = Primary.copy(alpha = 0.2f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.width(40.dp)
        ) {
            Text(
                text = "$setNumber",
                modifier = Modifier.padding(6.dp),
                color = Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Weight field
        OutlinedTextField(
            value = weightText,
            onValueChange = { v ->
                weightText = v
                v.toDoubleOrNull()?.let { onWeightChange(it) }
            },
            modifier = Modifier.width(80.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )

        // Reps field
        OutlinedTextField(
            value = repsText,
            onValueChange = { v ->
                repsText = v
                v.toIntOrNull()?.let { onRepsChange(it) }
            },
            modifier = Modifier.width(60.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Secondary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )

        // RIR field
        OutlinedTextField(
            value = rirText,
            onValueChange = { v ->
                rirText = v
                onRirChange(v.toIntOrNull())
            },
            modifier = Modifier.width(50.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFFD600),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            placeholder = { Text("-", color = Color.Gray, fontSize = 14.sp) }
        )
    }
}
