package com.gcordero.gymtracker.ui.screens.routines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.gcordero.gymtracker.domain.model.Routine
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.theme.Primary
import com.gcordero.gymtracker.ui.theme.PrimaryDim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onRoutineClick: (String) -> Unit,
    viewModel: RoutinesViewModel = viewModel()
) {
    val routines by viewModel.routines.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val importState by viewModel.importState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<Routine?>(null) }
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }

    val sortedRoutines = remember(routines) {
        routines.sortedBy { it.daysOfWeek.firstOrNull() ?: 99 }
    }

    // Handle import result feedback
    LaunchedEffect(importState) {
        if (importState is ImportState.Success) {
            showImportDialog = false
            viewModel.clearImportState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mis Rutinas",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Importar rutina",
                            tint = Primary.copy(alpha = 0.8f)
                        )
                    }
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = PrimaryDim,
                            contentColor = Primary
                        ),
                        modifier = Modifier.padding(end = 12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Añadir rutina",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Nueva", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Primary
                )
            } else if (sortedRoutines.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No tienes rutinas aún", color = Color.Gray)
                    Text("¡Crea tu primera rutina para empezar!", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedRoutines) { routine ->
                        RoutineItem(
                            routine = routine,
                            onClick = { onRoutineClick(routine.id) },
                            onEdit = { routineToEdit = it },
                            onDelete = { routineToDelete = it }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRoutineDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, desc, groups ->
                viewModel.addRoutine(name, desc, groups)
                showAddDialog = false
            }
        )
    }

    if (showImportDialog) {
        ImportRoutineDialog(
            importState = importState,
            onDismiss = {
                showImportDialog = false
                viewModel.clearImportState()
            },
            onConfirm = { text ->
                viewModel.importRoutine(text)
            }
        )
    }

    if (routineToEdit != null) {
        EditRoutineDialog(
            routine = routineToEdit!!,
            onDismiss = { routineToEdit = null },
            onConfirm = { updatedRoutine ->
                viewModel.updateRoutine(updatedRoutine)
                routineToEdit = null
            }
        )
    }

    if (routineToDelete != null) {
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("¿Eliminar Rutina?") },
            text = { Text("¿Estás seguro de que quieres eliminar '${routineToDelete?.name}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRoutine(routineToDelete!!.id)
                        routineToDelete = null
                    }
                ) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportRoutineDialog(
    importState: ImportState,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pastedText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importar Rutina") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Pega aquí el mensaje de rutina que alguien te compartió:",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = pastedText,
                    onValueChange = { pastedText = it },
                    placeholder = { Text("Pega el texto aquí…", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    ),
                    maxLines = 8
                )
                if (importState is ImportState.Error) {
                    Text(
                        text = importState.message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
                if (importState is ImportState.Success) {
                    Text(
                        text = "¡Rutina importada con éxito!",
                        color = Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pastedText) },
                enabled = pastedText.isNotBlank() && importState !is ImportState.Success,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Importar", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun dayOfWeekLabel(day: Int): String = when (day) {
    1 -> "Lunes"
    2 -> "Martes"
    3 -> "Miércoles"
    4 -> "Jueves"
    5 -> "Viernes"
    6 -> "Sábado"
    7 -> "Domingo"
    else -> ""
}

@Composable
fun RoutineItem(
    routine: Routine,
    onClick: () -> Unit,
    onEdit: (Routine) -> Unit,
    onDelete: (Routine) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dayLabel = routine.daysOfWeek.firstOrNull()?.let { dayOfWeekLabel(it) } ?: ""

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 12.dp,
        containerColor = Color(0xFF16162A),
        borderColor = Primary.copy(alpha = 0.22f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Chip de día de la semana
                if (dayLabel.isNotEmpty()) {
                    Surface(
                        color = PrimaryDim,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = dayLabel.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Nombre de la rutina
                Text(
                    text = routine.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    lineHeight = 20.sp
                )

                // Descripción
                if (routine.description.isNotEmpty()) {
                    Text(
                        text = routine.description,
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA),
                        maxLines = 1
                    )
                }

                // Chips de grupos musculares
                if (routine.muscleGroups.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        items(routine.muscleGroups) { group ->
                            Surface(
                                color = Color.White.copy(alpha = 0.07f),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = group,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFCCCCCC),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit(routine)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDelete(routine)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoutineDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var muscleGroupsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Rutina") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                OutlinedTextField(
                    value = muscleGroupsText,
                    onValueChange = { muscleGroupsText = it },
                    label = { Text("Grupos musculares (opcional)") },
                    placeholder = { Text("Pecho, Tríceps, Hombros") },
                    supportingText = { Text("Separados por coma", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val groups = muscleGroupsText
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        onConfirm(name, description, groups)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Crear", color = Color.Black)
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
    routine: Routine,
    onDismiss: () -> Unit,
    onConfirm: (Routine) -> Unit
) {
    var name by remember { mutableStateOf(routine.name) }
    var description by remember { mutableStateOf(routine.description) }
    var muscleGroupsText by remember { mutableStateOf(routine.muscleGroups.joinToString(", ")) }

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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                OutlinedTextField(
                    value = muscleGroupsText,
                    onValueChange = { muscleGroupsText = it },
                    label = { Text("Grupos musculares") },
                    placeholder = { Text("Pecho, Tríceps, Hombros") },
                    supportingText = { Text("Separados por coma", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val groups = muscleGroupsText
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        onConfirm(routine.copy(name = name, description = description, muscleGroups = groups))
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
