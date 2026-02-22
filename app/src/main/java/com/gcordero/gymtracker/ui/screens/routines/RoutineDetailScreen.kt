package com.gcordero.gymtracker.ui.screens.routines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    routineId: String,
    onBackClick: () -> Unit,
    onStartWorkout: (String) -> Unit,
    viewModel: RoutineDetailViewModel = viewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(routineId) {
        viewModel.loadRoutineDetail(routineId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Rutina", color = Primary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Primary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Ejercicio")
            }
        },
        bottomBar = {
            if (exercises.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { onStartWorkout(routineId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("EMPEZAR ENTRENAMIENTO", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
            } else if (exercises.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No hay ejercicios en esta rutina", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exercises) { exercise ->
                        ExerciseItem(exercise = exercise)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, muscle ->
                viewModel.addExercise(routineId, name, muscle)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExerciseItem(exercise: Exercise) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = exercise.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = exercise.muscleGroup,
                fontSize = 14.sp,
                color = Primary
            )
            if (exercise.notes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = exercise.notes,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Ejercicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Ejercicio") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = muscle,
                    onValueChange = { muscle = it },
                    label = { Text("Grupo Muscular (Ej: Pecho, Espalda)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, muscle) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Añadir", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
