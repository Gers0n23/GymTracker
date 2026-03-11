package com.gcordero.gymtracker.ui.screens.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcordero.gymtracker.domain.model.CatalogExercise
import com.gcordero.gymtracker.domain.model.ExerciseType
import com.gcordero.gymtracker.ui.theme.Glass
import com.gcordero.gymtracker.ui.theme.GlassBorder
import com.gcordero.gymtracker.ui.theme.Primary

/**
 * BottomSheet modal para seleccionar un ejercicio del catálogo global.
 * Muestra una barra de búsqueda, chips de filtro por grupo muscular y una lista de resultados.
 * Al seleccionar un ejercicio pide configurar series y peso inicial antes de confirmar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerBottomSheet(
    exercises: List<CatalogExercise>,
    muscleGroups: List<String>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (exercise: CatalogExercise, targetSets: Int, initialWeight: Double) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }
    var selectedExercise by remember { mutableStateOf<CatalogExercise?>(null) }

    val filteredExercises = remember(exercises, searchQuery, selectedMuscle) {
        val q = searchQuery.trim().lowercase()
        exercises.filter { ex ->
            val matchesMuscle = selectedMuscle == null
                || ex.muscleGroup.equals(selectedMuscle, ignoreCase = true)
            val matchesQuery = q.isEmpty()
                || ex.name.lowercase().contains(q)
                || ex.aliases.any { it.lowercase().contains(q) }
                || ex.muscleGroup.lowercase().contains(q)
                || ex.equipment.lowercase().contains(q)
            matchesMuscle && matchesQuery
        }.sortedWith(compareBy {
            if (q.isEmpty() || it.name.lowercase().contains(q)) "0_${it.name}" else "1_${it.name}"
        })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1E1E2E),
        tonalElevation = 0.dp
    ) {
        if (selectedExercise != null) {
            ExerciseConfigPanel(
                exercise = selectedExercise!!,
                onBack = { selectedExercise = null },
                onConfirm = { sets, weight ->
                    onConfirm(selectedExercise!!, sets, weight)
                }
            )
        } else {
            ExerciseListPanel(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                muscleGroups = muscleGroups,
                selectedMuscle = selectedMuscle,
                onMuscleSelected = { selectedMuscle = if (selectedMuscle == it) null else it },
                exercises = filteredExercises,
                isLoading = isLoading,
                onExerciseSelected = { selectedExercise = it }
            )
        }
    }
}

@Composable
private fun ExerciseListPanel(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    muscleGroups: List<String>,
    selectedMuscle: String?,
    onMuscleSelected: (String) -> Unit,
    exercises: List<CatalogExercise>,
    isLoading: Boolean,
    onExerciseSelected: (CatalogExercise) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
    ) {
        // Título
        Text(
            "Seleccionar Ejercicio",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
        )

        // Barra de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Buscar por nombre, músculo o máquina…", color = Color.Gray, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = Glass,
                unfocusedContainerColor = Glass,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        // Chips de grupos musculares
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            muscleGroups.forEach { muscle ->
                val isSelected = selectedMuscle == muscle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Primary else Glass)
                        .border(1.dp, if (isSelected) Primary else GlassBorder, RoundedCornerShape(20.dp))
                        .clickable { onMuscleSelected(muscle) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        muscle,
                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (exercises.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No se encontraron ejercicios", color = Color.Gray)
            }
        } else {
            Text(
                "${exercises.size} ejercicio${if (exercises.size != 1) "s" else ""}",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(exercises) { exercise ->
                    ExerciseCatalogItem(
                        exercise = exercise,
                        onClick = { onExerciseSelected(exercise) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ExerciseCatalogItem(
    exercise: CatalogExercise,
    onClick: () -> Unit
) {
    val typeColor = when (exercise.type) {
        ExerciseType.CARDIO -> Color(0xFF1565C0)
        ExerciseType.TIMED -> Color(0xFFF57C00)
        else -> Primary.copy(alpha = 0.8f)
    }
    val typeLabel = when (exercise.type) {
        ExerciseType.CARDIO -> "Cardio"
        ExerciseType.TIMED -> "Tiempo"
        else -> "Fuerza"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Glass)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                exercise.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    append(exercise.muscleGroup)
                    if (exercise.equipment.isNotEmpty()) append(" • ${exercise.equipment}")
                },
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(typeColor.copy(alpha = 0.15f))
                .border(1.dp, typeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(typeLabel, color = typeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExerciseConfigPanel(
    exercise: CatalogExercise,
    onBack: () -> Unit,
    onConfirm: (targetSets: Int, initialWeight: Double) -> Unit
) {
    var targetSets by remember { mutableIntStateOf(3) }
    var initialWeightText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Column {
            Text("Configurar ejercicio", fontSize = 13.sp, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
            Text(
                buildString {
                    append(exercise.muscleGroup)
                    if (exercise.equipment.isNotEmpty()) append(" • ${exercise.equipment}")
                },
                fontSize = 13.sp,
                color = Color.Gray
            )
        }

        HorizontalDivider(color = GlassBorder)

        // Series objetivo
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Series objetivo", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                Text("Cuántas veces repetirás el ejercicio", fontSize = 12.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { if (targetSets > 1) targetSets-- }, modifier = Modifier.size(40.dp)) {
                    Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
                Text(
                    "$targetSets",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(onClick = { if (targetSets < 10) targetSets++ }, modifier = Modifier.size(40.dp)) {
                    Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
            }
        }

        // Peso inicial (solo para ejercicios de fuerza)
        if (exercise.type == ExerciseType.STRENGTH) {
            Column {
                Text("Peso inicial (kg)", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                Text("Opcional. Se usará en tu primera sesión con este ejercicio", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = initialWeightText,
                    onValueChange = { initialWeightText = it },
                    placeholder = { Text("0.0", color = Color.Gray) },
                    suffix = { Text("kg", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = Glass,
                        unfocusedContainerColor = Glass,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Text("← Volver")
            }
            Button(
                onClick = {
                    onConfirm(targetSets, initialWeightText.toDoubleOrNull() ?: 0.0)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Añadir a rutina", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
