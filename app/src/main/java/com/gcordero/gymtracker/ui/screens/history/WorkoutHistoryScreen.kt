package com.gcordero.gymtracker.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcordero.gymtracker.domain.model.WorkoutSession
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.navigation.SessionHolder
import com.gcordero.gymtracker.ui.theme.Primary
import com.gcordero.gymtracker.ui.theme.Secondary
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    onBackClick: () -> Unit,
    onSessionClick: (String) -> Unit, // receives sessionId
    viewModel: WorkoutHistoryViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Historial de Entrenamientos",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Primary
                    )
                }
                sessions.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Sin entrenamientos registrados",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Text(
                            "Completa tu primera sesión para verla aquí.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sessions) { session ->
                            SessionHistoryItem(
                                session = session,
                                onClick = {
                                    SessionHolder.selectedSession = session
                                    onSessionClick(session.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionHistoryItem(
    session: WorkoutSession,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "ES"))
    val dateStr = dateFormat.format(session.startTime.toDate())

    val durationMinutes = if (session.endTime != null) {
        val diffMs = session.endTime.toDate().time - session.startTime.toDate().time
        (diffMs / 60000).toInt()
    } else null

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.routineName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (session.totalWeightLifted > 0) {
                        InfoChip(
                            label = "${session.totalWeightLifted.toInt()} kg",
                            color = Primary
                        )
                    }
                    if (durationMinutes != null && durationMinutes > 0) {
                        InfoChip(
                            label = "${durationMinutes} min",
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
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Primary
            )
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
