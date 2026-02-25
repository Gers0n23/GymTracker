package com.gcordero.gymtracker.ui.screens.routines

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.gcordero.gymtracker.domain.model.Exercise
import com.gcordero.gymtracker.domain.model.ExerciseType
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.theme.Primary
import com.gcordero.gymtracker.ui.theme.PrimaryDim

private fun dayLabel(days: List<Int>): String {
    val name = when (days.firstOrNull()) {
        1 -> "Lunes"; 2 -> "Martes"; 3 -> "Miércoles"
        4 -> "Jueves"; 5 -> "Viernes"; 6 -> "Sábado"; 7 -> "Domingo"
        else -> return ""
    }
    return name.uppercase()
}

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
                        val day = dayLabel(routine?.daysOfWeek ?: emptyList())
                        if (day.isNotEmpty()) {
                            Text(
                                text = day,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Primary.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                        }
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
                    IconButton(onClick = { showAddExerciseDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir Ejercicio", tint = Primary)
                    }
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
        bottomBar = {
            if (exercises.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { onStartWorkout(routineId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("EMPEZAR ENTRENAMIENTO", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(exercises) { index, exercise ->
                        ExerciseItem(
                            exercise = exercise,
                            index = index,
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
            onConfirm = { name, muscle, media, targetSets, exerciseType ->
                viewModel.addExercise(routineId, name, muscle, media, targetSets, exerciseType)
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
    index: Int,
    onEdit: (Exercise) -> Unit,
    onDelete: (Exercise) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 14.dp,
        containerColor = Color(0xFF16162A),
        borderColor = Primary.copy(alpha = 0.22f),
        onClick = { expanded = !expanded }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge numérico de orden
                Surface(
                    color = PrimaryDim,
                    shape = CircleShape,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Nombre + grupo muscular + tipo
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = exercise.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        lineHeight = 19.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (exercise.muscleGroup.isNotEmpty()) {
                            Text(
                                text = exercise.muscleGroup,
                                fontSize = 12.sp,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                        val (typeLabel, typeColor) = when (exercise.type) {
                            ExerciseType.TIMED -> "⏱ Tiempo" to Color(0xFFFFB300)
                            ExerciseType.CARDIO -> "🏃 Cardio" to Color(0xFF42A5F5)
                            ExerciseType.STRENGTH -> null to null
                        }
                        if (typeLabel != null && typeColor != null) {
                            Surface(
                                color = typeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 9.sp,
                                    color = typeColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(6.dp))

                // Derecha: badge de series + flecha expand + menú
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (exercise.targetSets > 0 && exercise.type != ExerciseType.CARDIO) {
                        Surface(
                            color = Color.White.copy(alpha = 0.07f),
                            shape = RoundedCornerShape(7.dp)
                        ) {
                            Text(
                                text = "${exercise.targetSets}×",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF888888)
                            )
                        }
                    }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(18.dp)
                    )
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = Color(0xFF666666),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { showMenu = false; onEdit(exercise) }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                onClick = { showMenu = false; onDelete(exercise) }
                            )
                        }
                    }
                }
            }

            // Contenido expandible
            AnimatedVisibility(visible = expanded) {
                Column {
                    if (exercise.notes.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = exercise.notes,
                            fontSize = 12.sp,
                            color = Color(0xFFAAAAAA),
                            lineHeight = 17.sp
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

                        Spacer(Modifier.height(14.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exercise.mediaUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // URL inválida
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.05f)
                        ) {
                            Box {
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = "Vista previa",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
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
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text("Toca para reproducir", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
    onConfirm: (String, String, String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }
    var targetSets by remember { mutableIntStateOf(3) }
    var selectedType by remember { mutableStateOf(ExerciseType.STRENGTH) }

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
                // Selector de tipo de ejercicio
                ExerciseTypeSelector(selectedType = selectedType, onTypeSelected = { selectedType = it })
                // Series objetivo (oculto para CARDIO, que siempre es 1 sesión)
                if (selectedType != ExerciseType.CARDIO) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Series objetivo", fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (targetSets > 1) targetSets-- }, modifier = Modifier.size(36.dp)) {
                                Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "$targetSets",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(onClick = { if (targetSets < 8) targetSets++ }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Más series")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val sets = if (selectedType == ExerciseType.CARDIO) 1 else targetSets
                        onConfirm(name, muscle, mediaUrl, sets, selectedType.name)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Añadir", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ExerciseTypeSelector(
    selectedType: ExerciseType,
    onTypeSelected: (ExerciseType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tipo de ejercicio", fontSize = 14.sp, color = Color.Gray)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ExerciseType.STRENGTH to "💪 Fuerza",
                ExerciseType.TIMED to "⏱ Tiempo",
                ExerciseType.CARDIO to "🏃 Cardio"
            ).forEach { (type, label) ->
                val isSelected = selectedType == type
                val color = when (type) {
                    ExerciseType.STRENGTH -> Primary
                    ExerciseType.TIMED -> Color(0xFFFFB300)
                    ExerciseType.CARDIO -> Color(0xFF42A5F5)
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTypeSelected(type) },
                    color = if (isSelected) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) color else Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) color else Color.White.copy(alpha = 0.5f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
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
    var targetSets by remember { mutableIntStateOf(exercise.targetSets.coerceAtLeast(1)) }
    var selectedType by remember { mutableStateOf(exercise.type) }

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
                ExerciseTypeSelector(selectedType = selectedType, onTypeSelected = { selectedType = it })
                if (selectedType != ExerciseType.CARDIO) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Series objetivo", fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (targetSets > 1) targetSets-- }, modifier = Modifier.size(36.dp)) {
                                Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "$targetSets",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(onClick = { if (targetSets < 8) targetSets++ }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Más series")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val sets = if (selectedType == ExerciseType.CARDIO) 1 else targetSets
                        onConfirm(exercise.copy(
                            name = name,
                            muscleGroup = muscle,
                            notes = notes,
                            mediaUrl = mediaUrl,
                            targetSets = sets,
                            exerciseType = selectedType.name
                        ))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Guardar", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
