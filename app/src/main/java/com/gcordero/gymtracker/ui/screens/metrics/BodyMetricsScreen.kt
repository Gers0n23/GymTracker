package com.gcordero.gymtracker.ui.screens.metrics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcordero.gymtracker.data.repository.ExerciseCatalogRepository
import com.gcordero.gymtracker.data.util.ExerciseCatalogSeeder
import kotlinx.coroutines.launch
import com.gcordero.gymtracker.domain.model.BodyMetric
import com.gcordero.gymtracker.ui.components.GlassCard
import com.gcordero.gymtracker.ui.theme.AmberAccent
import com.gcordero.gymtracker.ui.theme.GreenAccent
import com.gcordero.gymtracker.ui.theme.Primary
import com.gcordero.gymtracker.ui.theme.PrimaryDim
import com.gcordero.gymtracker.ui.theme.RedAccent
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun imcCategory(imc: Double): Pair<String, Color> = when {
    imc < 18.5 -> "Bajo peso"  to Color(0xFF42A5F5)
    imc < 25.0 -> "Normal"     to GreenAccent
    imc < 30.0 -> "Sobrepeso"  to AmberAccent
    else       -> "Obesidad"   to RedAccent
}

private val dateFmt = SimpleDateFormat("d MMM", Locale("es"))
private fun fmtDate(metric: BodyMetric) =
    runCatching { dateFmt.format(metric.timestamp.toDate()) }.getOrElse { "—" }

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(
    viewModel: BodyMetricsViewModel = viewModel(),
    insightVm: ProgressInsightViewModel = viewModel()
) {
    val metrics      by viewModel.metrics.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val heightCm     by viewModel.heightCm.collectAsState()
    val age          by viewModel.age.collectAsState()
    val isMale       by viewModel.isMale.collectAsState()
    val isProfileSet by viewModel.isProfileSet.collectAsState()
    val goal         by viewModel.goal.collectAsState()

    val insightState   by insightVm.insightState.collectAsState()
    val selectedWindow by insightVm.selectedWindow.collectAsState()

    var showAddDialog     by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var metricToEdit      by remember { mutableStateOf<BodyMetric?>(null) }
    var metricToDelete    by remember { mutableStateOf<BodyMetric?>(null) }

    val latestMetric   = metrics.firstOrNull()
    val previousMetric = metrics.getOrNull(1)

    val macros = remember(latestMetric?.weightKg, heightCm, age, isMale, goal) {
        BodyMetricsViewModel.calculateMacros(
            latestMetric?.weightKg ?: 75.0,
            heightCm, age, isMale, goal
        )
    }

    val bodyAnalysis = remember(latestMetric, heightCm, isMale) {
        latestMetric?.let { BodyMetricsViewModel.calculateBodyAnalysis(it, heightCm, isMale) }
    }
    val hasBodyAnalysis = bodyAnalysis != null && (
        bodyAnalysis.navyFatPct != null || bodyAnalysis.rcc != null ||
        bodyAnalysis.rce != null || bodyAnalysis.ffmi != null ||
        bodyAnalysis.chestToWaist != null || bodyAnalysis.bodyShape != null
    )

    LaunchedEffect(isProfileSet) {
        if (!isProfileSet) showProfileDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Composición Corporal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                },
                actions = {
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Perfil",
                            tint = Color(0xFF555555)
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
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nueva", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── Hero card ─────────────────────────────────────────────
                item {
                    if (latestMetric != null) {
                        HeroMetricCard(latest = latestMetric, previous = previousMetric)
                    } else {
                        EmptyStateCard(onAddClick = { showAddDialog = true })
                    }
                }

                // ── Profile banner ────────────────────────────────────────
                if (!isProfileSet) {
                    item { ProfileMissingBanner(onClick = { showProfileDialog = true }) }
                }

                // ── Macros recomendados ───────────────────────────────────
                item { SectionHeader("MACROS RECOMENDADOS") }
                item { GoalSelector(selected = goal, onSelect = { viewModel.setGoal(it) }) }
                item { MacrosRow(macros = macros) }

                // ── Weight chart ──────────────────────────────────────────
                if (metrics.size >= 2) {
                    item { SectionHeader("EVOLUCIÓN DE PESO") }
                    item { WeightChartCard(metrics = metrics) }
                }

                // ── Medidas corporales ─────────────────────────────────────
                if (latestMetric != null && latestMetric.hasAnyMeasurement()) {
                    item { SectionHeader("MEDIDAS ACTUALES") }
                    item { MeasurementsCard(latest = latestMetric, previous = metrics.getOrNull(1)) }
                }

                // ── Análisis derivado ──────────────────────────────────────
                if (hasBodyAnalysis && bodyAnalysis != null) {
                    item { SectionHeader("ANÁLISIS DE MEDIDAS") }
                    item { BodyAnalysisSection(analysis = bodyAnalysis, isMale = isMale) }
                }

                // ── AI Analysis ───────────────────────────────────────────
                item { SectionHeader("ANÁLISIS CON IA") }
                item {
                    AiAnalysisSection(
                        insightState   = insightState,
                        selectedWindow = selectedWindow,
                        onWindowSelect = { insightVm.selectWindow(it) },
                        onAnalyze      = { insightVm.analyze() }
                    )
                }

                // ── History ───────────────────────────────────────────────
                if (metrics.isNotEmpty()) {
                    item { SectionHeader("HISTORIAL") }
                    items(metrics.take(7)) { metric ->
                        val idx  = metrics.indexOf(metric)
                        val prev = metrics.getOrNull(idx + 1)
                        MetricHistoryItem(
                            metric         = metric,
                            previousMetric = prev,
                            onEdit         = { metricToEdit = it },
                            onDelete       = { metricToDelete = it }
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    // ── Standard dialogs ──────────────────────────────────────────────────────
    if (showProfileDialog) {
        ProfileSetupDialog(
            currentHeightCm = heightCm,
            currentAge      = age,
            currentIsMale   = isMale,
            onDismiss = { showProfileDialog = false },
            onConfirm = { h, a, m ->
                viewModel.updateProfile(h, a, m)
                showProfileDialog = false
            }
        )
    }
    if (showAddDialog) {
        MetricFormDialog(
            title     = "Nueva Medición",
            initial   = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { f ->
                viewModel.addMetric(
                    f.weight, f.fat, f.muscle,
                    f.neck, f.chest, f.waist, f.hip, f.bicep, f.forearm, f.thigh, f.calf
                )
                showAddDialog = false
            }
        )
    }

    if (metricToEdit != null) {
        MetricFormDialog(
            title     = "Editar Medición",
            subtitle  = "Medición del ${fmtDate(metricToEdit!!)}",
            initial   = metricToEdit,
            onDismiss = { metricToEdit = null },
            onConfirm = { f ->
                viewModel.updateMetric(
                    metricToEdit!!, f.weight, f.fat, f.muscle,
                    f.neck, f.chest, f.waist, f.hip, f.bicep, f.forearm, f.thigh, f.calf
                )
                metricToEdit = null
            }
        )
    }

    if (metricToDelete != null) {
        AlertDialog(
            onDismissRequest = { metricToDelete = null },
            title = { Text("¿Eliminar medición?") },
            text  = {
                Text(
                    "Se eliminará la medición del ${fmtDate(metricToDelete!!)}. Esta acción no se puede deshacer.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMetric(metricToDelete!!.id)
                        metricToDelete = null
                    }
                ) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { metricToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF555555),
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyStateCard(onAddClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 28.dp,
        containerColor = Color(0xFF16162A),
        borderColor = Primary.copy(alpha = 0.2f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("⚖️", fontSize = 36.sp)
            Text("Sin mediciones aún", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                "Registra tu primera medición para ver tu\nevolución y recomendaciones personalizadas",
                color = Color(0xFF888888),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(
                onClick = onAddClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = PrimaryDim,
                    contentColor = Primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Primera medición", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Hero metric card ──────────────────────────────────────────────────────────

@Composable
private fun HeroMetricCard(latest: BodyMetric, previous: BodyMetric?) {
    val delta       = if (previous != null) latest.weightKg - previous.weightKg else null
    val deltaUp     = (delta ?: 0.0) > 0
    val imcValue    = latest.imc ?: 0.0
    val (imcLabel, imcColor) =
        if (imcValue > 0) imcCategory(imcValue) else ("Sin datos" to Color(0xFF666666))
    val musclePct = latest.musclePercentage
    val fatPct    = latest.fatPercentage

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF23234A), Color(0xFF16162A))
                )
            )
            .border(1.dp, Primary.copy(alpha = 0.32f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = PrimaryDim, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "ÚLTIMA MEDICIÓN · ${fmtDate(latest)}".uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                if (delta != null) {
                    val deltaColor = if (deltaUp) RedAccent else GreenAccent
                    Surface(color = deltaColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                if (deltaUp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = deltaColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text("%.1f kg".format(abs(delta)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = deltaColor)
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.1f kg".format(latest.weightKg),
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 58.sp,
                    textAlign = TextAlign.Center
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricMiniCard(
                    modifier = Modifier.weight(1f),
                    emoji = if (imcValue > 0) "📉" else "➖",
                    value = if (imcValue > 0) "%.1f".format(imcValue) else "—",
                    label = imcLabel,
                    color = imcColor
                )
                MetricMiniCard(
                    modifier = Modifier.weight(1f),
                    emoji = "💪",
                    value = if (musclePct != null) "%.1f kg".format(latest.weightKg * musclePct / 100) else "—",
                    label = "Músculo",
                    color = if (musclePct != null) Primary else Color.Gray
                )
                MetricMiniCard(
                    modifier = Modifier.weight(1f),
                    emoji = "📊",
                    value = if (fatPct != null) "%.1f kg".format(latest.weightKg * fatPct / 100) else "—",
                    label = "Grasa",
                    color = if (fatPct != null) AmberAccent else Color.Gray
                )
            }
        }
    }
}

@Composable
private fun MetricMiniCard(modifier: Modifier = Modifier, emoji: String, value: String, label: String, color: Color) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp)
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
        }
    }
}

// ── Profile missing banner ────────────────────────────────────────────────────

@Composable
private fun ProfileMissingBanner(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = AmberAccent.copy(alpha = 0.07f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Configura tu perfil", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                Text("Agrega estatura, edad y sexo para macros exactos", fontSize = 11.sp, color = Color(0xFF888888))
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF555555))
        }
    }
}

// ── Goal selector ─────────────────────────────────────────────────────────────

@Composable
private fun GoalSelector(selected: String, onSelect: (String) -> Unit) {
    val goals = listOf(
        Triple("muscle",   "💪", "Ganar\nMúsculo"),
        Triple("maintain", "⚖️", "Mante-\nnimiento"),
        Triple("cut",      "🔥", "Perder\nGrasa")
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        goals.forEach { (id, emoji, label) ->
            val isSelected = selected == id
            Surface(
                modifier = Modifier.weight(1f).clickable { onSelect(id) },
                color  = if (isSelected) PrimaryDim else Color.White.copy(alpha = 0.04f),
                shape  = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) Primary else Color.White.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                ) {
                    Text(emoji, fontSize = 18.sp)
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Primary else Color(0xFF888888),
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

// ── Macros row ────────────────────────────────────────────────────────────────

@Composable
private fun MacrosRow(macros: MacroRecommendation) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroCard(Modifier.weight(1f), "🥩", "${macros.proteinG}g",  "Proteína", Color(0xFFFF7043))
        MacroCard(Modifier.weight(1f), "🌾", "${macros.carbsG}g",   "Carbos",   AmberAccent)
        MacroCard(Modifier.weight(1f), "🥑", "${macros.fatG}g",     "Grasas",   GreenAccent)
        MacroCard(Modifier.weight(1f), "🔥", "${macros.calories}",  "kcal/día", Primary)
    }
}

@Composable
private fun MacroCard(modifier: Modifier, emoji: String, value: String, label: String, color: Color) {
    GlassCard(
        modifier = modifier,
        padding = 10.dp,
        containerColor = Color(0xFF16162A),
        borderColor = color.copy(alpha = 0.22f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(label, fontSize = 9.sp, color = Color(0xFF888888), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ── Weight chart ──────────────────────────────────────────────────────────────

@Composable
private fun WeightChartCard(metrics: List<BodyMetric>) {
    val chartPoints = metrics.take(8).reversed()
    val weights     = chartPoints.map { it.weightKg }
    val minW        = weights.minOrNull() ?: 0.0
    val maxW        = weights.maxOrNull() ?: 100.0

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 16.dp,
        containerColor = Color(0xFF16162A),
        borderColor = Primary.copy(alpha = 0.18f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mín  ${"%.1f".format(minW)} kg", fontSize = 10.sp, color = GreenAccent)
                Text("Máx  ${"%.1f".format(maxW)} kg", fontSize = 10.sp, color = AmberAccent)
            }
            WeightLineChart(chartPoints = chartPoints)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val n = chartPoints.size
                chartPoints.forEachIndexed { i, metric ->
                    val showLabel = i == 0 || i == n - 1 || (n > 4 && i == n / 2)
                    if (showLabel) {
                        Text(fmtDate(metric), fontSize = 9.sp, color = Color(0xFF555555))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightLineChart(chartPoints: List<BodyMetric>) {
    if (chartPoints.size < 2) return

    val weights = chartPoints.map { it.weightKg.toFloat() }
    val minW    = weights.minOrNull() ?: 0f
    val maxW    = weights.maxOrNull() ?: 1f
    val range   = (maxW - minW).coerceAtLeast(0.5f)

    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        val w    = size.width
        val h    = size.height
        val padV = h * 0.12f
        val n    = weights.size

        val points = weights.mapIndexed { i, weight ->
            Offset(
                x = i * w / (n - 1).toFloat(),
                y = h - padV - ((weight - minW) / range * (h - 2 * padV))
            )
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, h)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Primary.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f, endY = h
            )
        )

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = linePath,
            color = Primary,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        points.forEach { pt ->
            drawCircle(color = Primary, radius = 3.5.dp.toPx(), center = pt)
            drawCircle(color = Color(0xFF16162A), radius = 1.8.dp.toPx(), center = pt)
        }
    }
}

// ── Measurements helpers ───────────────────────────────────────────────────────

private fun BodyMetric.hasAnyMeasurement() =
    neckCm != null || chestCm != null || waistCm != null || hipCm != null ||
    bicepCm != null || forearmCm != null || thighCm != null || calfCm != null

@Composable
private fun MeasurementsCard(latest: BodyMetric, previous: BodyMetric?) {
    val items = listOf(
        Triple("Cuello",      latest.neckCm,    previous?.neckCm),
        Triple("Pecho",       latest.chestCm,   previous?.chestCm),
        Triple("Cintura",     latest.waistCm,   previous?.waistCm),
        Triple("Cadera",      latest.hipCm,     previous?.hipCm),
        Triple("Bíceps",      latest.bicepCm,   previous?.bicepCm),
        Triple("Antebrazo",   latest.forearmCm, previous?.forearmCm),
        Triple("Muslo",       latest.thighCm,   previous?.thighCm),
        Triple("Pantorrilla", latest.calfCm,    previous?.calfCm)
    ).filter { it.second != null }

    if (items.isEmpty()) return

    GlassCard(
        modifier       = Modifier.fillMaxWidth(),
        padding        = 16.dp,
        containerColor = Color(0xFF16162A),
        borderColor    = Primary.copy(alpha = 0.18f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(2).forEach { pair ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { (label, value, prev) ->
                        MeasurementChip(
                            modifier = Modifier.weight(1f),
                            label    = label,
                            valueCm  = value!!,
                            prevCm   = prev
                        )
                    }
                    // fill remaining space if odd number
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MeasurementChip(
    modifier: Modifier,
    label: String,
    valueCm: Double,
    prevCm: Double?
) {
    val delta   = if (prevCm != null) valueCm - prevCm else null
    val dColor  = when {
        delta == null || delta == 0.0 -> Color(0xFF555555)
        // For waist smaller is better; for everything else bigger is better
        label == "Cintura" -> if (delta < 0) GreenAccent else RedAccent
        else               -> if (delta > 0) GreenAccent else RedAccent
    }

    Surface(
        modifier = modifier,
        color    = Color.White.copy(alpha = 0.04f),
        shape    = RoundedCornerShape(10.dp),
        border   = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier              = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement   = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, fontSize = 10.sp, color = Color(0xFF888888), fontWeight = FontWeight.Medium)
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("%.1f cm".format(valueCm), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (delta != null && abs(delta) >= 0.1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (delta > 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint     = dColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text("%.1f".format(abs(delta)), fontSize = 10.sp, color = dColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── History item ──────────────────────────────────────────────────────────────

@Composable
private fun MetricHistoryItem(
    metric: BodyMetric,
    previousMetric: BodyMetric?,
    onEdit: (BodyMetric) -> Unit,
    onDelete: (BodyMetric) -> Unit
) {
    val delta    = if (previousMetric != null) metric.weightKg - previousMetric.weightKg else null
    var showMenu by remember { mutableStateOf(false) }

    // Compact badge list of recorded measurements
    val measureBadges = buildList {
        metric.waistCm?.let   { add("Cin. ${"%.0f".format(it)} cm") }
        metric.bicepCm?.let   { add("Bíc. ${"%.0f".format(it)} cm") }
        metric.thighCm?.let   { add("Mus. ${"%.0f".format(it)} cm") }
        metric.chestCm?.let   { add("Pec. ${"%.0f".format(it)} cm") }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 12.dp,
        containerColor = Color(0xFF13131F),
        borderColor = Color.White.copy(alpha = 0.07f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(color = PrimaryDim, shape = RoundedCornerShape(7.dp)) {
                        Text(
                            text = fmtDate(metric),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                    Text("%.1f kg".format(metric.weightKg), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (metric.fatPercentage != null) {
                        Text("%.0f%% grasa".format(metric.fatPercentage), fontSize = 11.sp, color = Color(0xFF888888))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (delta != null) {
                        val positive = delta > 0
                        val dColor   = if (positive) RedAccent else GreenAccent
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (positive) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = dColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("%.1f".format(abs(delta)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = dColor)
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = Color(0xFF555555),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { showMenu = false; onEdit(metric) }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                onClick = { showMenu = false; onDelete(metric) }
                            )
                        }
                    }
                }
            }
            // Measurement badges row
            if (measureBadges.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    measureBadges.forEach { badge ->
                        Surface(
                            color  = Primary.copy(alpha = 0.07f),
                            shape  = RoundedCornerShape(5.dp),
                            border = BorderStroke(0.5.dp, Primary.copy(alpha = 0.2f))
                        ) {
                            Text(
                                badge,
                                fontSize = 9.sp,
                                color    = Primary.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Body Analysis Section ─────────────────────────────────────────────────────

@Composable
private fun BodyAnalysisSection(analysis: BodyAnalysis, isMale: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ── Row 1: Grasa US Navy + RCC ────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            analysis.navyFatPct?.let { fat ->
                val (label, color) = fatCategory(fat, isMale)
                AnalysisCard(
                    modifier    = Modifier.weight(1f),
                    title       = "% Grasa (US Navy)",
                    value       = "%.1f%%".format(fat),
                    subtitle    = label,
                    accentColor = color,
                    tooltip     = "Calculado con cuello, cintura${if (!isMale) ", cadera" else ""} y estatura"
                )
            }
            analysis.rcc?.let { rcc ->
                val ideal  = if (isMale) 0.90 else 0.80
                val (label, color) = when {
                    rcc < ideal            -> "Ideal" to GreenAccent
                    rcc < ideal + 0.09     -> "Moderado" to AmberAccent
                    else                   -> "Alto riesgo" to RedAccent
                }
                AnalysisCard(
                    modifier    = Modifier.weight(1f),
                    title       = "Cintura / Cadera",
                    value       = "%.2f".format(rcc),
                    subtitle    = label,
                    accentColor = color,
                    tooltip     = "< ${if (isMale) "0.90" else "0.80"} ideal"
                )
            }
        }

        // ── Row 2: RCE + FFMI ────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            analysis.rce?.let { rce ->
                val (label, color) = when {
                    rce < 0.43  -> "Muy delgado" to Color(0xFF42A5F5)
                    rce < 0.50  -> "Saludable"   to GreenAccent
                    rce < 0.60  -> "Sobrepeso"   to AmberAccent
                    else        -> "Obesidad"     to RedAccent
                }
                AnalysisCard(
                    modifier    = Modifier.weight(1f),
                    title       = "Cintura / Estatura",
                    value       = "%.2f".format(rce),
                    subtitle    = label,
                    accentColor = color,
                    tooltip     = "< 0.50 saludable para todas las edades"
                )
            }
            analysis.ffmi?.let { ffmi ->
                val (label, color) = when {
                    ffmi < 18.0 -> "Bajo"     to Color(0xFF42A5F5)
                    ffmi < 20.0 -> "Promedio" to Color(0xFF888888)
                    ffmi < 22.0 -> "Bueno"    to GreenAccent
                    ffmi < 25.0 -> "Atlético" to Primary
                    else        -> "Elite"    to AmberAccent
                }
                AnalysisCard(
                    modifier    = Modifier.weight(1f),
                    title       = "FFMI",
                    value       = "%.1f".format(ffmi),
                    subtitle    = label,
                    accentColor = color,
                    tooltip     = "Fat-Free Mass Index · > 25 límite natural"
                )
            }
        }

        // ── Lean mass ─────────────────────────────────────────────────────
        analysis.leanMassKg?.let { lean ->
            GlassCard(
                modifier       = Modifier.fillMaxWidth(),
                containerColor = Color(0xFF16162A),
                borderColor    = Primary.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Masa magra estimada", fontSize = 11.sp, color = Color(0xFF888888))
                        Text("%.1f kg".format(lean), fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold, color = Primary)
                    }
                    Text("💪", fontSize = 28.sp)
                }
            }
        }

        // ── Proporciones estéticas ────────────────────────────────────────
        val hasProportions = analysis.chestToWaist != null ||
            analysis.bicepToNeck != null || analysis.thighToCalf != null
        if (hasProportions) {
            AestheticProportionsCard(analysis)
        }

        // ── Silueta ───────────────────────────────────────────────────────
        analysis.bodyShape?.let { shape ->
            BodyShapeCard(shape, isMale)
        }
    }
}

@Composable
private fun AnalysisCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    tooltip: String
) {
    GlassCard(
        modifier       = modifier,
        padding        = 14.dp,
        containerColor = Color(0xFF13131F),
        borderColor    = accentColor.copy(alpha = 0.25f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 10.sp, color = Color(0xFF888888), fontWeight = FontWeight.Medium)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Surface(
                color = accentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(5.dp)
            ) {
                Text(
                    subtitle, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(tooltip, fontSize = 9.sp, color = Color(0xFF555555), lineHeight = 13.sp)
        }
    }
}

@Composable
private fun AestheticProportionsCard(analysis: BodyAnalysis) {
    GlassCard(
        modifier       = Modifier.fillMaxWidth(),
        padding        = 16.dp,
        containerColor = Color(0xFF16162A),
        borderColor    = AmberAccent.copy(alpha = 0.2f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("✨", fontSize = 14.sp)
                Text("Proporciones Estéticas", fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
            }

            analysis.chestToWaist?.let { ratio ->
                // Ideal ≥ 1.45, golden ratio 1.618
                ProportionBar(
                    label   = "Pecho / Cintura",
                    ratio   = ratio,
                    min     = 1.0,
                    ideal   = 1.618,
                    display = "%.2f".format(ratio),
                    hint    = when {
                        ratio >= 1.55 -> "Excelente — forma V pronunciada"
                        ratio >= 1.45 -> "Bueno — buena forma V"
                        ratio >= 1.30 -> "Moderado"
                        else          -> "Mejorable — reducir cintura o ganar pecho"
                    }
                )
            }

            analysis.bicepToNeck?.let { ratio ->
                // Ideal: bíceps ≈ cuello (ratio ~1.0–1.10)
                ProportionBar(
                    label   = "Bíceps / Cuello",
                    ratio   = ratio,
                    min     = 0.7,
                    ideal   = 1.05,
                    display = "%.2f".format(ratio),
                    hint    = when {
                        ratio >= 1.05 -> "Excelente — brazos bien desarrollados"
                        ratio >= 0.95 -> "Bueno"
                        ratio >= 0.85 -> "En desarrollo"
                        else          -> "Brazos pequeños vs cuello — trabajar brazos"
                    }
                )
            }

            analysis.thighToCalf?.let { ratio ->
                // Ideal ~1.75
                ProportionBar(
                    label   = "Muslo / Pantorrilla",
                    ratio   = ratio,
                    min     = 1.2,
                    ideal   = 1.75,
                    display = "%.2f".format(ratio),
                    hint    = when {
                        ratio in 1.65..1.85 -> "Excelente — proporción ideal de piernas"
                        ratio >= 1.50        -> "Bueno"
                        ratio < 1.50         -> "Mejorable — trabajar cuádriceps/isquios"
                        else                 -> "Mejorable — trabajar pantorrillas"
                    }
                )
            }
        }
    }
}

@Composable
private fun ProportionBar(
    label: String,
    ratio: Double,
    min: Double,
    ideal: Double,
    display: String,
    hint: String
) {
    val progress = ((ratio - min) / (ideal * 1.2 - min)).coerceIn(0.0, 1.0).toFloat()
    val color = when {
        ratio >= ideal * 0.95 -> GreenAccent
        ratio >= ideal * 0.80 -> AmberAccent
        else                  -> Color(0xFF555555)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Bottom
        ) {
            Text(label, fontSize = 11.sp, color = Color(0xFF888888))
            Text(display, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.07f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(color.copy(alpha = 0.5f), color)),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
        Text(hint, fontSize = 10.sp, color = Color(0xFF666666), lineHeight = 14.sp)
    }
}

@Composable
private fun BodyShapeCard(shape: BodyShape, isMale: Boolean) {
    val description = when (shape) {
        BodyShape.V_SHAPE   -> if (isMale)
            "Hombros y pecho anchos con cintura estrecha. El físico más asociado con la estética masculina atlética."
            else "Hombros anchos con caderas más estrechas."
        BodyShape.HOURGLASS -> "Proporciones equilibradas entre pecho y cadera con cintura bien definida. Considerada la silueta femenina más estética."
        BodyShape.PEAR      -> "Caderas más anchas que los hombros. Acumulas más masa en la parte inferior del cuerpo."
        BodyShape.APPLE     -> "Mayor acumulación en la zona abdominal. Reducir cintura es prioritario para la salud cardiovascular."
        BodyShape.RECTANGLE -> "Proporciones similares entre hombros, cintura y caderas. Potencial para definir una V definida con entrenamiento."
    }

    GlassCard(
        modifier       = Modifier.fillMaxWidth(),
        padding        = 16.dp,
        containerColor = Color(0xFF13131F),
        borderColor    = Primary.copy(alpha = 0.15f)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(shape.emoji, fontSize = 32.sp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Silueta actual", fontSize = 10.sp, color = Color(0xFF888888))
                Text(shape.labelEs, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text(description, fontSize = 11.sp,
                    color = Color(0xFF999999), lineHeight = 16.sp)
            }
        }
    }
}

private fun fatCategory(fat: Double, isMale: Boolean): Pair<String, Color> = if (isMale) {
    when {
        fat < 6  -> "Esencial"   to Color(0xFF42A5F5)
        fat < 14 -> "Atlético"   to GreenAccent
        fat < 18 -> "Fitness"    to Primary
        fat < 25 -> "Promedio"   to AmberAccent
        else     -> "Alto"       to RedAccent
    }
} else {
    when {
        fat < 14 -> "Esencial"   to Color(0xFF42A5F5)
        fat < 21 -> "Atlético"   to GreenAccent
        fat < 25 -> "Fitness"    to Primary
        fat < 32 -> "Promedio"   to AmberAccent
        else     -> "Alto"       to RedAccent
    }
}

// ── AI Analysis Section ───────────────────────────────────────────────────────

@Composable
private fun AiAnalysisSection(
    insightState: InsightState,
    selectedWindow: AnalysisWindow,
    onWindowSelect: (AnalysisWindow) -> Unit,
    onAnalyze: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Time window chip selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalysisWindow.entries.forEach { window ->
                val isSelected = window == selectedWindow
                FilterChip(
                    selected = isSelected,
                    onClick  = { onWindowSelect(window) },
                    label    = { Text(window.labelEs, fontSize = 12.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryDim,
                        selectedLabelColor     = Primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled             = true,
                        selected            = isSelected,
                        selectedBorderColor = Primary,
                        borderColor         = Color.White.copy(alpha = 0.12f)
                    )
                )
            }
        }

        // Analyze button
        Button(
            onClick  = onAnalyze,
            enabled  = insightState !is InsightState.Loading,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = Primary),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.Black
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Analizar mi progreso",
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        // State-driven result
        when (insightState) {
            is InsightState.Idle    -> { /* nothing */ }
            is InsightState.Loading -> InsightLoadingCard()
            is InsightState.Error   -> InsightErrorCard(insightState.message)
            is InsightState.Success -> InsightResultCards(insightState.insight)
        }
    }
}

@Composable
private fun InsightLoadingCard() {
    GlassCard(
        modifier       = Modifier.fillMaxWidth(),
        containerColor = Color(0xFF16162A),
        borderColor    = Primary.copy(alpha = 0.2f)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier    = Modifier.size(20.dp),
                color       = Primary,
                strokeWidth = 2.dp
            )
            Text(
                "Analizando tu progreso...",
                color    = Color(0xFF888888),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun InsightErrorCard(message: String) {
    GlassCard(
        modifier       = Modifier.fillMaxWidth(),
        containerColor = Color(0xFF1A1010),
        borderColor    = RedAccent.copy(alpha = 0.3f)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint     = RedAccent,
                modifier = Modifier.size(18.dp)
            )
            Text(message, color = RedAccent, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun InsightResultCards(insight: com.gcordero.gymtracker.domain.model.ProgressInsight) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // General summary banner
        GlassCard(
            modifier       = Modifier.fillMaxWidth(),
            containerColor = Color(0xFF1A1A2E),
            borderColor    = Primary.copy(alpha = 0.25f)
        ) {
            Row(
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("✨", fontSize = 16.sp)
                Text(
                    insight.resumenGeneral,
                    color      = Color(0xFFCCCCCC),
                    fontSize   = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }

        // One card per category
        insight.categorias.forEach { category ->
            InsightCategoryCard(category)
        }
    }
}

@Composable
private fun InsightCategoryCard(category: com.gcordero.gymtracker.domain.model.InsightCategory) {
    val (emoji, accentColor) = when (category.titulo) {
        "Cuerpo"        -> "🏋️" to Primary
        "Alimentación"  -> "🥗" to GreenAccent
        "Entrenamiento" -> "💪" to AmberAccent
        else            -> "📊" to Color(0xFF888888)
    }

    GlassCard(
        modifier       = Modifier.fillMaxWidth(),
        padding        = 16.dp,
        containerColor = Color(0xFF13131F),
        borderColor    = accentColor.copy(alpha = 0.2f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Title row with colored left accent bar
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .background(accentColor, RoundedCornerShape(2.dp))
                )
                Text(emoji, fontSize = 16.sp)
                Text(
                    category.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = Color.White
                )
            }

            // Summary paragraph
            Text(
                category.resumen,
                color      = Color(0xFFAAAAAA),
                fontSize   = 12.sp,
                lineHeight = 18.sp
            )

            // Action bullets
            if (category.acciones.isNotEmpty()) {
                HorizontalDivider(
                    color     = accentColor.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    category.acciones.forEach { action ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.Top
                        ) {
                            Text(
                                "→",
                                color      = accentColor,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.padding(top = 1.dp)
                            )
                            Text(
                                action.texto,
                                color      = Color(0xFF999999),
                                fontSize   = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSetupDialog(
    currentHeightCm: Int,
    currentAge: Int,
    currentIsMale: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Boolean) -> Unit
) {
    var heightText by remember { mutableStateOf(if (currentHeightCm > 0) "$currentHeightCm" else "") }
    var ageText    by remember { mutableStateOf(if (currentAge > 0)      "$currentAge"      else "") }
    var isMale     by remember { mutableStateOf(currentIsMale) }

    var seedStatus by remember { mutableStateOf("idle") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tu Perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Necesitamos estos datos para calcular tu BMR y recomendaciones de macros.",
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                    lineHeight = 16.sp
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = { Text("Estatura (cm)") },
                    placeholder = { Text("Ej: 175") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it },
                    label = { Text("Edad") },
                    placeholder = { Text("Ej: 28") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true to "♂  Hombre", false to "♀  Mujer").forEach { (male, label) ->
                        val sel = isMale == male
                        Surface(
                            modifier = Modifier.weight(1f).clickable { isMale = male },
                            color  = if (sel) PrimaryDim else Color.White.copy(alpha = 0.05f),
                            shape  = RoundedCornerShape(10.dp),
                            border = BorderStroke(if (sel) 1.5.dp else 1.dp, if (sel) Primary else Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = label,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) Primary else Color(0xFF888888),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                Text(
                    "HERRAMIENTAS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555),
                    letterSpacing = 1.5.sp
                )

                val (seedLabel, seedColor, seedEnabled) = when (seedStatus) {
                    "loading" -> Triple("Poblando catálogo...", Color(0xFF888888), false)
                    "ok"      -> Triple("✓ Catálogo listo", GreenAccent, false)
                    "error"   -> Triple("⚠️ Error — reintentar", RedAccent, true)
                    else      -> Triple("🏋️  Poblar catálogo de ejercicios", Primary, true)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            seedStatus = "loading"
                            val result = ExerciseCatalogSeeder.seedIfEmpty(ExerciseCatalogRepository())
                            seedStatus = if (result.isSuccess) "ok" else "error"
                        }
                    },
                    enabled = seedEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, seedColor.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = seedColor)
                ) {
                    if (seedStatus == "loading") {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = seedColor, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(seedLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = heightText.toIntOrNull()
                    val a = ageText.toIntOrNull()
                    if (h != null && h in 100..250 && a != null && a in 10..120) {
                        onConfirm(h, a, isMale)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text("Guardar", color = Color.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Omitir") }
        }
    )
}

// ── Metric form dialog (shared by Add and Edit) ───────────────────────────────

private data class MetricFormInput(
    val weight: Double,
    val fat: Double?,
    val muscle: Double?,
    val neck: Double?,
    val chest: Double?,
    val waist: Double?,
    val hip: Double?,
    val bicep: Double?,
    val forearm: Double?,
    val thigh: Double?,
    val calf: Double?
)

@Composable
private fun MetricFormDialog(
    title: String,
    subtitle: String? = null,
    initial: BodyMetric? = null,
    onDismiss: () -> Unit,
    onConfirm: (MetricFormInput) -> Unit
) {
    val scrollState = rememberScrollState()

    var weight  by remember { mutableStateOf(initial?.weightKg?.let { "%.1f".format(it) } ?: "") }
    var fat     by remember { mutableStateOf(initial?.fatPercentage?.let { "%.1f".format(it) } ?: "") }
    var muscle  by remember { mutableStateOf(initial?.musclePercentage?.let { "%.1f".format(it) } ?: "") }
    var neck    by remember { mutableStateOf(initial?.neckCm?.let { "%.1f".format(it) } ?: "") }
    var chest   by remember { mutableStateOf(initial?.chestCm?.let { "%.1f".format(it) } ?: "") }
    var waist   by remember { mutableStateOf(initial?.waistCm?.let { "%.1f".format(it) } ?: "") }
    var hip     by remember { mutableStateOf(initial?.hipCm?.let { "%.1f".format(it) } ?: "") }
    var bicep   by remember { mutableStateOf(initial?.bicepCm?.let { "%.1f".format(it) } ?: "") }
    var forearm by remember { mutableStateOf(initial?.forearmCm?.let { "%.1f".format(it) } ?: "") }
    var thigh   by remember { mutableStateOf(initial?.thighCm?.let { "%.1f".format(it) } ?: "") }
    var calf    by remember { mutableStateOf(initial?.calfCm?.let { "%.1f".format(it) } ?: "") }

    fun fmt(s: String) = s.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = Color(0xFF888888))
                }

                // ── Composición corporal ──────────────────────────────────
                FormSectionLabel("COMPOSICIÓN CORPORAL")
                MetricField(weight, { weight = it }, "Peso (kg) *", "Ej: 78.5")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricField(fat,    { fat    = it }, "% Grasa",   "Ej: 18",
                        modifier = Modifier.weight(1f))
                    MetricField(muscle, { muscle = it }, "% Músculo", "Ej: 42",
                        modifier = Modifier.weight(1f))
                }

                // ── Medidas con cinta (cm) ────────────────────────────────
                FormSectionLabel("MEDIDAS CON CINTA (cm)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricField(neck,  { neck  = it }, "Cuello",  "Ej: 38", modifier = Modifier.weight(1f))
                    MetricField(chest, { chest = it }, "Pecho",   "Ej: 98", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricField(waist, { waist = it }, "Cintura", "Ej: 80", modifier = Modifier.weight(1f))
                    MetricField(hip,   { hip   = it }, "Cadera",  "Ej: 95", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricField(bicep,   { bicep   = it }, "Bíceps",     "Ej: 35", modifier = Modifier.weight(1f))
                    MetricField(forearm, { forearm = it }, "Antebrazo",  "Ej: 28", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricField(thigh, { thigh = it }, "Muslo",       "Ej: 55", modifier = Modifier.weight(1f))
                    MetricField(calf,  { calf  = it }, "Pantorrilla", "Ej: 38", modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = fmt(weight)
                    if (w != null && w > 0) {
                        onConfirm(MetricFormInput(
                            weight  = w,
                            fat     = fmt(fat),
                            muscle  = fmt(muscle),
                            neck    = fmt(neck),
                            chest   = fmt(chest),
                            waist   = fmt(waist),
                            hip     = fmt(hip),
                            bicep   = fmt(bicep),
                            forearm = fmt(forearm),
                            thigh   = fmt(thigh),
                            calf    = fmt(calf)
                        ))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text("Guardar", color = Color.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF555555),
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun MetricField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value          = value,
        onValueChange  = onValueChange,
        label          = { Text(label, fontSize = 11.sp) },
        placeholder    = { Text(placeholder, fontSize = 11.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier       = modifier,
        singleLine     = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            focusedLabelColor  = Primary
        )
    )
}
