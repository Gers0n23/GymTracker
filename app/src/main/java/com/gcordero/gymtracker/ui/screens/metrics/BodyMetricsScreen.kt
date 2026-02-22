package com.gcordero.gymtracker.ui.screens.metrics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.theme.Primary
import com.gcordero.gymtracker.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(
    viewModel: BodyMetricsViewModel = viewModel()
) {
    val metrics by viewModel.metrics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val latestMetric = metrics.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Composición Corporal", color = Primary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Primary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Métrica")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Peso",
                        value = "${latestMetric?.weightKg ?: "--"} kg",
                        color = Color.White
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "IMC",
                        value = "%.1f".format(latestMetric?.imc ?: 0.0),
                        color = Secondary
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Grasa",
                        value = "${latestMetric?.fatPercentage ?: "--"}%",
                        color = Color(0xFF00E676)
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Músculo",
                        value = "${latestMetric?.musclePercentage ?: "--"}%",
                        color = Primary
                    )
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Evolución (Gráficos próximamente)", color = Color.Gray, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Gráfico de Vico", color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMetricDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { weight, fat, muscle ->
                viewModel.addMetric(weight, fat, muscle)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    GlassCard(modifier = modifier) {
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMetricDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, Double?, Double?) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Medición") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("% Grasa (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = muscle,
                    onValueChange = { muscle = it },
                    label = { Text("% Músculo (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val w = weight.toDoubleOrNull()
                    if (w != null) {
                        onConfirm(w, fat.toDoubleOrNull(), muscle.toDoubleOrNull())
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
