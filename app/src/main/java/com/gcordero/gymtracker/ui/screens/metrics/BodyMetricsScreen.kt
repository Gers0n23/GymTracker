package com.gcordero.gymtracker.ui.screens.metrics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlin.math.roundToInt

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
    viewModel: BodyMetricsViewModel = viewModel()
) {
    val metrics      by viewModel.metrics.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val heightCm     by viewModel.heightCm.collectAsState()
    val age          by viewModel.age.collectAsState()
    val isMale       by viewModel.isMale.collectAsState()
    val isProfileSet by viewModel.isProfileSet.collectAsState()
    val goal         by viewModel.goal.collectAsState()
    val todayProtein by viewModel.todayProteinG.collectAsState()

    var showAddDialog          by remember { mutableStateOf(false) }
    var showProfileDialog      by remember { mutableStateOf(false) }
    var showCustomProteinDialog by remember { mutableStateOf(false) }

    val latestMetric   = metrics.firstOrNull()
    val previousMetric = metrics.getOrNull(1)

    val macros = remember(latestMetric?.weightKg, heightCm, age, isMale, goal) {
        BodyMetricsViewModel.calculateMacros(
            latestMetric?.weightKg ?: 75.0,
            heightCm, age, isMale, goal
        )
    }

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

                // ── Macros ────────────────────────────────────────────────
                item { SectionHeader("MACROS RECOMENDADOS") }
                item { GoalSelector(selected = goal, onSelect = { viewModel.setGoal(it) }) }
                item { MacrosRow(macros = macros) }

                // ── Protein tracker ───────────────────────────────────────
                item { SectionHeader("PROTEÍNA DEL DÍA") }
                item {
                    ProteinTrackerCard(
                        consumed = todayProtein,
                        goal     = macros.proteinG,
                        onQuickAdd   = { viewModel.addProtein(it) },
                        onCustomAdd  = { showCustomProteinDialog = true },
                        onReset      = { viewModel.resetProtein() }
                    )
                }

                // ── Weight chart ──────────────────────────────────────────
                if (metrics.size >= 2) {
                    item { SectionHeader("EVOLUCIÓN DE PESO") }
                    item { WeightChartCard(metrics = metrics) }
                }

                // ── History ───────────────────────────────────────────────
                if (metrics.isNotEmpty()) {
                    item { SectionHeader("HISTORIAL") }
                    items(metrics.take(7)) { metric ->
                        val idx  = metrics.indexOf(metric)
                        val prev = metrics.getOrNull(idx + 1)
                        MetricHistoryItem(metric = metric, previousMetric = prev)
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
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
        AddMetricDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { weight, fat, muscle ->
                viewModel.addMetric(weight, fat, muscle)
                showAddDialog = false
            }
        )
    }
    if (showCustomProteinDialog) {
        CustomProteinDialog(
            onDismiss = { showCustomProteinDialog = false },
            onConfirm = { grams ->
                viewModel.addProtein(grams)
                showCustomProteinDialog = false
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E1E42), Color(0xFF16162A))
                )
            )
            .border(1.dp, Primary.copy(alpha = 0.32f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Date chip
            Surface(
                color = PrimaryDim,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = "ÚLTIMA MEDICIÓN · ${fmtDate(latest)}".uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Weight + delta
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "%.1f kg".format(latest.weightKg),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 42.sp
                )
                if (delta != null) {
                    val deltaColor = if (deltaUp) RedAccent else GreenAccent
                    Surface(
                        color = deltaColor.copy(alpha = 0.13f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                if (deltaUp) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = deltaColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "%.1f kg".format(abs(delta)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = deltaColor
                            )
                        }
                    }
                }
            }

            // IMC row
            if (imcValue > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "IMC  ${"%.1f".format(imcValue)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        color = imcColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = imcLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = imcColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Muscle / Fat chips
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val musclePct = latest.musclePercentage
                val fatPct    = latest.fatPercentage
                if (musclePct != null) {
                    CompositionChip(
                        emoji = "💪",
                        value = "%.1f kg".format(latest.weightKg * musclePct / 100),
                        label = "Músculo",
                        color = Primary
                    )
                }
                if (fatPct != null) {
                    CompositionChip(
                        emoji = "📊",
                        value = "%.1f kg".format(latest.weightKg * fatPct / 100),
                        label = "Grasa",
                        color = AmberAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun CompositionChip(emoji: String, value: String, label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(emoji, fontSize = 14.sp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(label, fontSize = 9.sp, color = Color(0xFF888888))
            }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        goals.forEach { (id, emoji, label) ->
            val isSelected = selected == id
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(id) },
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MacroCard(Modifier.weight(1f), "🥩", "${macros.proteinG}g",   "Proteína", Color(0xFFFF7043))
        MacroCard(Modifier.weight(1f), "🌾", "${macros.carbsG}g",    "Carbos",   AmberAccent)
        MacroCard(Modifier.weight(1f), "🥑", "${macros.fatG}g",      "Grasas",   GreenAccent)
        MacroCard(Modifier.weight(1f), "🔥", "${macros.calories}",   "kcal/día", Primary)
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

// ── Protein tracker ───────────────────────────────────────────────────────────

@Composable
private fun ProteinTrackerCard(
    consumed: Int,
    goal: Int,
    onQuickAdd:  (Int) -> Unit,
    onCustomAdd: () -> Unit,
    onReset:     () -> Unit
) {
    val progress = if (goal > 0) consumed.toFloat() / goal else 0f
    val progressColor = when {
        progress >= 0.8f -> GreenAccent
        progress >= 0.5f -> AmberAccent
        else             -> RedAccent
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 16.dp,
        containerColor = Color(0xFF16162A),
        borderColor = progressColor.copy(alpha = 0.28f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${consumed}g",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = progressColor,
                            lineHeight = 30.sp
                        )
                        Text(
                            text = "/ ${goal}g",
                            fontSize = 14.sp,
                            color = Color(0xFF888888),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text("proteína consumida hoy", fontSize = 11.sp, color = Color(0xFF666666))
                }
                Surface(
                    color = progressColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = progressColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = Color.White.copy(alpha = 0.07f)
            )

            // Quick-add + reset row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(10, 20, 30, 50).forEach { g ->
                    Surface(
                        modifier = Modifier.clickable { onQuickAdd(g) },
                        color = Color.White.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "+${g}g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCCCCCC),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                // Custom amount
                Surface(
                    modifier = Modifier.clickable { onCustomAdd() },
                    color = PrimaryDim,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Primary, modifier = Modifier.size(12.dp))
                        Text("Otro", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                    }
                }
                // Reset
                Surface(
                    modifier = Modifier.clickable { onReset() },
                    color = Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "↺",
                        fontSize = 15.sp,
                        color = Color(0xFF555555),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
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

            // Min / max row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mín  ${"%.1f".format(minW)} kg", fontSize = 10.sp, color = GreenAccent)
                Text("Máx  ${"%.1f".format(maxW)} kg", fontSize = 10.sp, color = AmberAccent)
            }

            WeightLineChart(chartPoints = chartPoints)

            // Date labels (first, middle, last)
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

        // Gradient fill under curve
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

        // Line
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = linePath,
            color = Primary,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Dots
        points.forEach { pt ->
            drawCircle(color = Primary, radius = 3.5.dp.toPx(), center = pt)
            drawCircle(color = Color(0xFF16162A), radius = 1.8.dp.toPx(), center = pt)
        }
    }
}

// ── History item ──────────────────────────────────────────────────────────────

@Composable
private fun MetricHistoryItem(metric: BodyMetric, previousMetric: BodyMetric?) {
    val delta = if (previousMetric != null) metric.weightKg - previousMetric.weightKg else null

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 12.dp,
        containerColor = Color(0xFF13131F),
        borderColor = Color.White.copy(alpha = 0.07f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                // Sex selector
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

@Composable
fun AddMetricDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, Double?, Double?) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var fat    by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Medición") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso (kg) *") },
                    placeholder = { Text("Ej: 78.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("% Grasa corporal (opcional)") },
                    placeholder = { Text("Ej: 18") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
                OutlinedTextField(
                    value = muscle,
                    onValueChange = { muscle = it },
                    label = { Text("% Masa muscular (opcional)") },
                    placeholder = { Text("Ej: 42") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weight.toDoubleOrNull()
                    if (w != null && w > 0) onConfirm(w, fat.toDoubleOrNull(), muscle.toDoubleOrNull())
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
private fun CustomProteinDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Proteína") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Gramos de proteína") },
                placeholder = { Text("Ej: 45") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val g = text.toIntOrNull()
                    if (g != null && g > 0) onConfirm(g)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text("Añadir", color = Color.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
