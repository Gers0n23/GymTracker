package com.gcordero.gymtracker.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.navigation.SessionHolder
import com.gcordero.gymtracker.ui.theme.Primary
import com.gcordero.gymtracker.ui.theme.Secondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    onBackClick: () -> Unit,
    onSessionClick: (String) -> Unit,
    viewModel: WorkoutHistoryViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val availableRoutines by viewModel.availableRoutines.collectAsState()
    val selectedRoutines by viewModel.selectedRoutines.collectAsState()
    val dateFrom by viewModel.dateFrom.collectAsState()
    val dateTo by viewModel.dateTo.collectAsState()
    val hasActiveFilters by viewModel.hasActiveFilters.collectAsState()

    var filtersExpanded by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<WorkoutSession?>(null) }

    // Date picker state
    var showDateFromPicker by remember { mutableStateOf(false) }
    var showDateToPicker by remember { mutableStateOf(false) }

    val datePickerStateFrom = rememberDatePickerState(
        initialSelectedDateMillis = dateFrom?.time
    )
    val datePickerStateTo = rememberDatePickerState(
        initialSelectedDateMillis = dateTo?.time
    )

    if (showDateFromPicker) {
        DatePickerDialog(
            onDismissRequest = { showDateFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDateFrom(
                        datePickerStateFrom.selectedDateMillis?.let { utcMidnightToLocalDate(it) }
                    )
                    showDateFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateFromPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerStateFrom)
        }
    }

    if (showDateToPicker) {
        DatePickerDialog(
            onDismissRequest = { showDateToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDateTo(
                        datePickerStateTo.selectedDateMillis?.let { utcMidnightToLocalDate(it) }
                    )
                    showDateToPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateToPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerStateTo)
        }
    }

    // Delete confirmation dialog
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eliminar sesión") },
            text = { Text("¿Eliminar esta sesión? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(deleteTarget!!.id)
                        deleteTarget = null
                    }
                ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Historial",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Primary)
                    }
                },
                actions = {
                    // Filter toggle button
                    TextButton(
                        onClick = { filtersExpanded = !filtersExpanded }
                    ) {
                        Text(
                            text = if (hasActiveFilters) "Filtros ●" else "Filtros",
                            color = if (hasActiveFilters) Primary else Color.Gray,
                            fontSize = 13.sp
                        )
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
        ) {
            // Collapsible filter section
            AnimatedVisibility(
                visible = filtersExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FilterPanel(
                    availableRoutines = availableRoutines,
                    selectedRoutines = selectedRoutines,
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    onToggleRoutine = { viewModel.toggleRoutineFilter(it) },
                    onDateFromClick = { showDateFromPicker = true },
                    onDateToClick = { showDateToPicker = true },
                    onClearFilters = {
                        viewModel.clearFilters()
                    }
                )
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Primary
                        )
                    }
                }
                sessions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                if (hasActiveFilters) "Sin resultados para los filtros aplicados"
                                else "Sin entrenamientos registrados",
                                color = Color.Gray,
                                fontSize = 15.sp
                            )
                            if (hasActiveFilters) {
                                TextButton(onClick = { viewModel.clearFilters() }) {
                                    Text("Limpiar filtros", color = Primary)
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(sessions) { session ->
                            SessionHistoryItem(
                                session = session,
                                onClick = {
                                    SessionHolder.selectedSession = session
                                    onSessionClick(session.id)
                                },
                                onEdit = {
                                    SessionHolder.selectedSession = session
                                    onSessionClick(session.id)
                                },
                                onDelete = { deleteTarget = session }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(
    availableRoutines: List<String>,
    selectedRoutines: Set<String>,
    dateFrom: Date?,
    dateTo: Date?,
    onToggleRoutine: (String) -> Unit,
    onDateFromClick: () -> Unit,
    onDateToClick: () -> Unit,
    onClearFilters: () -> Unit
) {
    val dateFormatShort = SimpleDateFormat("dd/MM/yy", Locale("es", "ES"))

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date range row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fecha:", color = Color.Gray, fontSize = 12.sp)
                FilterChip(
                    selected = dateFrom != null,
                    onClick = onDateFromClick,
                    label = {
                        Text(
                            if (dateFrom != null) "Desde ${dateFormatShort.format(dateFrom)}" else "Desde",
                            fontSize = 11.sp
                        )
                    }
                )
                FilterChip(
                    selected = dateTo != null,
                    onClick = onDateToClick,
                    label = {
                        Text(
                            if (dateTo != null) "Hasta ${dateFormatShort.format(dateTo)}" else "Hasta",
                            fontSize = 11.sp
                        )
                    }
                )
            }

            // Routine chips
            if (availableRoutines.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rutina:", color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableRoutines) { routine ->
                            FilterChip(
                                selected = selectedRoutines.contains(routine),
                                onClick = { onToggleRoutine(routine) },
                                label = {
                                    Text(
                                        routine.take(20) + if (routine.length > 20) "…" else "",
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Clear filters
            TextButton(
                onClick = onClearFilters,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("Limpiar filtros", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SessionHistoryItem(
    session: WorkoutSession,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
    val dateStr = dateFormat.format(session.startTime.toDate())

    val durationMinutes = session.endTime?.let {
        val diffMs = it.toDate().time - session.startTime.toDate().time
        (diffMs / 60000).toInt()
    }

    var menuExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Date + routine name on same line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = dateStr,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = session.routineName,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Chips row (only if there's something to show)
                val hasChips = session.totalWeightLifted > 0 ||
                    (durationMinutes != null && durationMinutes > 0) ||
                    session.rpe != null

                if (hasChips) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (session.totalWeightLifted > 0) {
                            InfoChip(
                                label = "${session.totalWeightLifted.toInt()} kg",
                                color = Primary
                            )
                        }
                        if (durationMinutes != null && durationMinutes > 0) {
                            InfoChip(
                                label = "$durationMinutes min",
                                color = Secondary
                            )
                        }
                        if (session.rpe != null) {
                            InfoChip(
                                label = "RPE ${session.rpe}",
                                color = Color(0xFFFFD600)
                            )
                        }
                    }
                }
            }

            // 3-dot overflow menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InfoChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * El DatePicker de Material3 devuelve milisegundos en UTC midnight
 * (ej: 2026-02-23 00:00:00 UTC). Si el dispositivo está en UTC-6,
 * Date(utcMillis) sería las 18:00 del día anterior.
 * Esta función convierte los millis UTC al inicio del mismo día calendar
 * en la timezone local del dispositivo.
 */
private fun utcMidnightToLocalDate(utcMillis: Long): Date {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utcCal.timeInMillis = utcMillis
    return Calendar.getInstance().apply {
        set(
            utcCal[Calendar.YEAR],
            utcCal[Calendar.MONTH],
            utcCal[Calendar.DAY_OF_MONTH],
            0, 0, 0
        )
        set(Calendar.MILLISECOND, 0)
    }.time
}
