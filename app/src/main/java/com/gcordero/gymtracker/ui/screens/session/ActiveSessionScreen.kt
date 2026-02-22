package com.gcordero.gymtracker.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.theme.Glass
import com.gcordero.gymtracker.ui.theme.GlassBorder
import com.gcordero.gymtracker.ui.theme.Primary
import coil.compose.AsyncImage

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

    LaunchedEffect(routineId) {
        viewModel.startSession(routineId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sesión en curso", fontSize = 12.sp, color = Color.Gray)
                        Text("Entrenamiento", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        restTimer?.let { RestTimerBadge(seconds = it) }
                        TimerBadge(seconds = timerSeconds)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Button(
                onClick = { 
                    viewModel.finishSession(routineId, "Rutina") 
                    onFinish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("FINALIZAR SESIÓN", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(exercises) { exercise ->
                ExerciseSessionItem(
                    exercise = exercise,
                    sets = sets[exercise.id] ?: emptyList(),
                    onAddSet = { viewModel.addSet(exercise.id, exercise.name) },
                    onUpdateSet = { index, weight, reps, rir ->
                        viewModel.updateSet(exercise.id, index, weight, reps, rir)
                        if (weight > 0 && reps > 0) {
                            viewModel.startRestTimer()
                        }
                    }
                )
            }
        }
    }
}

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

@Composable
fun RestTimerBadge(seconds: Int) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFD600).copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "⏳ Descanse: %02d:%02d".format(seconds / 60, seconds % 60),
            color = Color(0xFFFFD600),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ExerciseSessionItem(
    exercise: Exercise,
    sets: List<SetRecord>,
    onAddSet: () -> Unit,
    onUpdateSet: (Int, Double, Int, Int?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (exercise.mediaUrl.isNotEmpty()) {
                    AsyncImage(
                        model = exercise.mediaUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Glass),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text(exercise.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(exercise.muscleGroup, fontSize = 12.sp, color = Color.Gray)
                }
            }
            TextButton(onClick = { /* Ver Historial */ }) {
                Text("HISTORIAL", fontSize = 10.sp, color = Primary)
            }
        }

        Spacer(Modifier.height(8.dp))

        sets.forEachIndexed { index, set ->
            SetRow(
                index = index + 1,
                set = set,
                onUpdate = { w, r, rir -> onUpdateSet(index, w, r, rir) }
            )
            Spacer(Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = onAddSet,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text("AGREGAR SERIE", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun SetRow(
    index: Int,
    set: SetRecord,
    onUpdate: (Double, Int, Int?) -> Unit
) {
    var weightText by remember { mutableStateOf(if (set.weight > 0) set.weight.toString() else "") }
    var repsText by remember { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    var rir by remember { mutableStateOf(set.rir) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "$index", modifier = Modifier.width(24.dp), color = Color.Gray, fontSize = 14.sp)
                
                MiniTextField(
                    value = weightText,
                    onValueChange = { 
                        weightText = it
                        onUpdate(it.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, rir)
                    },
                    placeholder = "kg",
                    modifier = Modifier.weight(1f)
                )

                MiniTextField(
                    value = repsText,
                    onValueChange = { 
                        repsText = it
                        onUpdate(weightText.toDoubleOrNull() ?: 0.0, it.toIntOrNull() ?: 0, rir)
                    },
                    placeholder = "reps",
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (repsText.isNotEmpty() && weightText.isNotEmpty()) Primary else Color.Transparent)
                        .border(1.dp, Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (repsText.isNotEmpty() && weightText.isNotEmpty()) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text("Reps in Reserve (RIR)", fontSize = 10.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0, 1, 2, 3, 4).forEach { value ->
                    RirChip(
                        value = value,
                        isSelected = rir == value,
                        onClick = { 
                            rir = value
                            onUpdate(weightText.toDoubleOrNull() ?: 0.0, repsText.toIntOrNull() ?: 0, value)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MiniTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 12.sp) },
        modifier = modifier
            .height(48.dp)
            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
fun RirChip(
    value: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Primary else Glass)
            .border(1.dp, if (isSelected) Primary else GlassBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (value == 4) "4+" else "$value",
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
