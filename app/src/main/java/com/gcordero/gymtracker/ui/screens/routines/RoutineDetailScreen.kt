package com.gcordero.gymtracker.ui.screens.routines

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
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
    val routine by viewModel.routine.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showEditRoutineDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var showDeleteRoutineConfirm by remember { mutableStateOf(false) }
    var showHeaderMenu by remember { mutableStateOf(false) }

    LaunchedEffect(routineId) {
        viewModel.loadRoutineDetail(routineId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            routine?.name ?: "Cargando...", 
                            color = Primary, 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        if (routine?.description?.isNotEmpty() == true) {
                            Text(
                                routine?.description ?: "", 
                                color = Color.Gray, 
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Primary)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showHeaderMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Primary)
                        }
                        DropdownMenu(
                            expanded = showHeaderMenu,
                            onDismissRequest = { showHeaderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar Rutina") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showHeaderMenu = false
                                    showEditRoutineDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar Rutina", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                onClick = {
                                    showHeaderMenu = false
                                    showDeleteRoutineConfirm = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExerciseDialog = true },
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
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
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
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No hay ejercicios en esta rutina", color = Color.Gray)
                    Text("¡Añade uno para empezar!", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exercises) { exercise ->
                        ExerciseItem(
                            exercise = exercise,
                            onEdit = { exerciseToEdit = it },
                            onDelete = { viewModel.deleteExercise(it.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onConfirm = { name, muscle, media ->
                viewModel.addExercise(routineId, name, muscle, media)
                showAddExerciseDialog = false
            }
        )
    }

    if (showEditRoutineDialog && routine != null) {
        EditRoutineDialog(
            routine = routine!!,
            onDismiss = { showEditRoutineDialog = false },
            onConfirm = { name, desc ->
                viewModel.updateRoutine(routineId, name, desc)
                showEditRoutineDialog = false
            }
        )
    }

    if (exerciseToEdit != null) {
        EditExerciseDialog(
            exercise = exerciseToEdit!!,
            onDismiss = { exerciseToEdit = null },
            onConfirm = { updatedExercise ->
                viewModel.updateExercise(updatedExercise)
                exerciseToEdit = null
            }
        )
    }

    if (showDeleteRoutineConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteRoutineConfirm = false },
            title = { Text("¿Eliminar Rutina?") },
            text = { Text("Esta acción eliminará la rutina '${routine?.name}' definitivamente de Firestore. ¿Continuar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRoutine(routineId, onSuccess = onBackClick)
                        showDeleteRoutineConfirm = false
                    }
                ) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRoutineConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ExerciseItem(
    exercise: Exercise,
    onEdit: (Exercise) -> Unit,
    onDelete: (Exercise) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
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
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit(exercise)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                            onClick = {
                                showMenu = false
                                onDelete(exercise)
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    if (exercise.notes.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = exercise.notes,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )
                    }

                    if (exercise.mediaUrl.isNotEmpty()) {
                        val context = LocalContext.current
                        val videoId = getYoutubeVideoId(exercise.mediaUrl)
                        val thumbnailUrl = if (videoId != null) {
                            "https://img.youtube.com/vi/$videoId/0.jpg"
                        } else {
                            exercise.mediaUrl
                        }

                        Spacer(Modifier.height(16.dp))
                        // Visualizador de Multimedia (Clickable)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clickable { 
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exercise.mediaUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Manejar error si la URL es inválida
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.05f)
                        ) {
                            Box {
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = "Vista previa",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Overlay con icono de Play
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.PlayArrow, 
                                            contentDescription = "Reproducir", 
                                            tint = Primary, 
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text("Toca para reproducir", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getYoutubeVideoId(url: String): String? {
    return try {
        val uri = Uri.parse(url)
        when {
            url.contains("youtu.be/") -> uri.pathSegments.firstOrNull()
            url.contains("youtube.com/watch") -> uri.getQueryParameter("v")
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Ejercicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Ejercicio") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                OutlinedTextField(
                    value = muscle,
                    onValueChange = { muscle = it },
                    label = { Text("Grupo Muscular") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                OutlinedTextField(
                    value = mediaUrl,
                    onValueChange = { mediaUrl = it },
                    label = { Text("URL de Video/Imagen (Opcional)") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, muscle, mediaUrl) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRoutineDialog(
    routine: com.gcordero.gymtracker.domain.model.Routine,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(routine.name) }
    var description by remember { mutableStateOf(routine.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Rutina") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, description) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Guardar", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditExerciseDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onConfirm: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf(exercise.name) }
    var muscle by remember { mutableStateOf(exercise.muscleGroup) }
    var notes by remember { mutableStateOf(exercise.notes) }
    var mediaUrl by remember { mutableStateOf(exercise.mediaUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Ejercicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                OutlinedTextField(
                    value = muscle,
                    onValueChange = { muscle = it },
                    label = { Text("Grupo Muscular") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                OutlinedTextField(
                    value = mediaUrl,
                    onValueChange = { mediaUrl = it },
                    label = { Text("URL de Video/Imagen") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(exercise.copy(name = name, muscleGroup = muscle, notes = notes, mediaUrl = mediaUrl))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Guardar", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
