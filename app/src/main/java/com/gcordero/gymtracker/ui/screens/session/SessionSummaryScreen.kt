package com.gcordero.gymtracker.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcordero.gymtracker.domain.model.SetRecord
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.theme.GlassBorder
import com.gcordero.gymtracker.ui.theme.Primary
import com.gcordero.gymtracker.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryScreen(
    session: WorkoutSession,
    onFinish: () -> Unit,
    viewModel: SessionSummaryViewModel = viewModel()
) {
    val sets by viewModel.sets.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    
    var rpe by remember { mutableFloatStateOf(8f) }
    var sleepQuality by remember { mutableIntStateOf(4) }
    var energyLevel by remember { mutableIntStateOf(3) }
    var showResults by remember { mutableStateOf(false) }

    LaunchedEffect(session.id) {
        viewModel.loadSets(session.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showResults) "Detalle de Sesión" else "Resumen de Sesión", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (showResults) {
                        IconButton(onClick = { showResults = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!showResults) {
                item {
                    Text(
                        text = "¡Buen Trabajo! 🏆",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Has completado tu sesión de hoy.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("VOLUMEN TOTAL", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                "${session.totalWeightLifted.toInt()} kg",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Estado de la Sesión", fontWeight = FontWeight.Bold, color = Color.White)
                            
                            // RPE
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Esfuerzo Percibido (RPE)", fontSize = 13.sp, color = Color.Gray)
                                    Text("${rpe.toInt()}", color = Primary, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = rpe,
                                    onValueChange = { rpe = it },
                                    valueRange = 1f..10f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Muy fácil", fontSize = 9.sp, color = Color.Gray)
                                    Text("Al límite", fontSize = 9.sp, color = Color.Gray)
                                }
                            }

                            // Sleep
                            Column {
                                Text("Calidad de Sueño", fontSize = 12.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    (1..5).forEach { i ->
                                        Text(
                                            "😴",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(if (sleepQuality >= i) Primary.copy(alpha = 0.2f) else Color.Transparent)
                                                .border(1.dp, if (sleepQuality >= i) Primary else Color.Transparent, RoundedCornerShape(4.dp))
                                            ,
                                            fontSize = 20.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Energy
                            Column {
                                Text("Nivel de Energía", fontSize = 12.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    (1..5).forEach { i ->
                                        Text(
                                            "⚡",
                                            fontSize = 20.sp,
                                            modifier = Modifier.padding(2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { showResults = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("VER DETALLES Y GUARDAR", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            } else {
                // Final Results Screen
                item {
                    PerformanceCard(rpe.toInt())
                }

                item {
                    Text(
                        "Detalle de Ejercicios",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val grouped = sets.groupBy { it.exerciseName }
                items(grouped.entries.toList()) { entry ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(entry.key, fontWeight = FontWeight.Bold, color = Primary)
                            entry.value.forEach { set ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Serie ${set.setNumber}", fontSize = 12.sp, color = Color.Gray)
                                    Text("${set.weight.toInt()}kg x ${set.reps}", fontWeight = FontWeight.Bold, color = Color.White)
                                    if (set.rir != null) {
                                        Text("RIR ${set.rir}", fontSize = 12.sp, color = Secondary)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { 
                            viewModel.saveFinal(session, rpe.toInt(), "Sueño: $sleepQuality, Energía: $energyLevel")
                            onFinish() 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                        } else {
                            Text("GUARDAR Y FINALIZAR", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceCard(rpe: Int) {
    val (note, color) = when {
        rpe >= 9 -> "¡Sesión Intensa! Has dado el máximo hoy. 🔥" to Color(0xFFCF6679)
        rpe >= 7 -> "¡Gran rendimiento! Un entrenamiento muy sólido. 💪" to Primary
        rpe >= 4 -> "Buen trabajo. Mantuviste la constancia. 👍" to Secondary
        else -> "Sesión de recuperación. Lo importante es no faltar. 🧘" to Color.Gray
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = color.copy(alpha = 0.1f),
        borderColor = color.copy(alpha = 0.5f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NOTA DE RENDIMIENTO", fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = note,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}
