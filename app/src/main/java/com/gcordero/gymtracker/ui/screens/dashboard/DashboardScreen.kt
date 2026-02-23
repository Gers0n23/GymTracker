package com.gcordero.gymtracker.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.theme.Primary
import com.gcordero.gymtracker.ui.theme.Secondary

import android.util.Log
import com.gcordero.gymtracker.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: androidx.navigation.NavHostController) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "GymTracker",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                },
                actions = {
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    val userId = auth.currentUser?.uid ?: "test_user"
                    
                    TextButton(onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Conectando con Firestore...")
                            val result = com.gcordero.gymtracker.data.util.DataPopulator.populateInitialData(userId)
                            if (result.isSuccess) {
                                snackbarHostState.showSnackbar("¡Base de Datos Poblada con Éxito!")
                            } else {
                                val error = result.exceptionOrNull()
                                snackbarHostState.showSnackbar("Error: ${error?.localizedMessage ?: "Fallo desconocido"}")
                            }
                        }
                    }) {
                        Text("DEBUG", color = Color.Gray, fontSize = 10.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0x0DFFFFFF),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        selectedTextColor = Primary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Routines.route) },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("Rutinas") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.WorkoutHistory.route) },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Historial") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        selectedTextColor = Primary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.BodyMetrics.route) },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Perfil") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        selectedTextColor = Primary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Hola, Gerson 💪",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            // Recovery Heatmap Section
            item {
                GridDashboardItem(
                    title = "Recuperación Muscular",
                    subtitle = "Estado anatómico actual"
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RecoveryHeatmap()
                    }
                }
            }

            // Quick Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Volumen Semanal",
                        value = "45,200 kg",
                        color = Primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Días Activos",
                        value = "4/5",
                        color = Secondary
                    )
                }
            }

            // Start Training Button
            item {
                Button(
                    onClick = { navController.navigate(Screen.Routines.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("INICIAR ENTRENAMIENTO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GridDashboardItem(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    GlassCard(modifier = modifier) {
        Column {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun RecoveryHeatmap() {
    val muscleStatus = remember {
        listOf(
            "Pecho" to Color(0xFF00E676),
            "Espalda" to Color(0xFFFFD600),
            "Piernas" to Color(0xFFFF5252),
            "Hombros" to Color(0xFF00E676),
            "Brazos" to Color(0xFFFFD600)
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val bodyColor = Color.White.copy(alpha = 0.05f)
        
        // --- Dibujo de silueta humana decorativa ---
        // Cabeza
        drawCircle(
            bodyColor,
            radius = 12.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(centerX, 25.dp.toPx())
        )
        
        // Torso
        val torsoOutline = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - 25.dp.toPx(), 40.dp.toPx())
            lineTo(centerX + 25.dp.toPx(), 40.dp.toPx())
            lineTo(centerX + 20.dp.toPx(), 110.dp.toPx())
            lineTo(centerX - 20.dp.toPx(), 110.dp.toPx())
            close()
        }
        drawPath(torsoOutline, bodyColor)
        
        // Brazos
        drawRoundRect(
            bodyColor,
            topLeft = androidx.compose.ui.geometry.Offset(centerX - 55.dp.toPx(), 45.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(20.dp.toPx(), 60.dp.toPx()),
            cornerRadius = CornerRadius(10.dp.toPx())
        )
        drawRoundRect(
            bodyColor,
            topLeft = androidx.compose.ui.geometry.Offset(centerX + 35.dp.toPx(), 45.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(20.dp.toPx(), 60.dp.toPx()),
            cornerRadius = CornerRadius(10.dp.toPx())
        )
        
        // Piernas
        drawRoundRect(
            bodyColor,
            topLeft = androidx.compose.ui.geometry.Offset(centerX - 22.dp.toPx(), 115.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(18.dp.toPx(), 60.dp.toPx()),
            cornerRadius = CornerRadius(8.dp.toPx())
        )
        drawRoundRect(
            bodyColor,
            topLeft = androidx.compose.ui.geometry.Offset(centerX + 4.dp.toPx(), 115.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(18.dp.toPx(), 60.dp.toPx()),
            cornerRadius = CornerRadius(8.dp.toPx())
        )
        
        muscleStatus.forEach { pair ->
            val muscle = pair.first
            val color = pair.second
            val offset = when(muscle) {
                "Pecho" -> androidx.compose.ui.geometry.Offset(centerX, 60.dp.toPx())
                "Hombros" -> androidx.compose.ui.geometry.Offset(centerX - 35.dp.toPx(), 55.dp.toPx())
                "Brazos" -> androidx.compose.ui.geometry.Offset(centerX - 50.dp.toPx(), 90.dp.toPx())
                "Piernas" -> androidx.compose.ui.geometry.Offset(centerX - 15.dp.toPx(), 140.dp.toPx())
                "Espalda" -> androidx.compose.ui.geometry.Offset(centerX + 35.dp.toPx(), 55.dp.toPx())
                else -> androidx.compose.ui.geometry.Offset(centerX, 60.dp.toPx())
            }
            
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = 15.dp.toPx(),
                center = offset
            )
            drawCircle(
                color = color,
                radius = 6.dp.toPx(),
                center = offset
            )
        }
    }
}
