package com.gcordero.gymtracker.ui.screens.metrics

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcordero.gymtracker.data.repository.ExerciseCatalogRepository
import com.gcordero.gymtracker.data.util.ExerciseCatalogSeeder
import kotlinx.coroutines.launch
import com.gcordero.gymtracker.domain.model.BodyMetric
import com.gcordero.gymtracker.domain.model.NutritionLog
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
    val context      = LocalContext.current
    val metrics      by viewModel.metrics.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val heightCm     by viewModel.heightCm.collectAsState()
    val age          by viewModel.age.collectAsState()
    val isMale       by viewModel.isMale.collectAsState()
    val isProfileSet by viewModel.isProfileSet.collectAsState()
    val goal         by viewModel.goal.collectAsState()
    val scanState    by viewModel.scanState.collectAsState()
    val todayLogs    by viewModel.todayLogs.collectAsState()

    // Today's macros
    val todayProtein  by viewModel.todayProteinG.collectAsState()
    val todayCarbs    by viewModel.todayCarbsG.collectAsState()
    val todayFat      by viewModel.todayFatG.collectAsState()
    val todayFiber    by viewModel.todayFiberG.collectAsState()
    val todayCalories by viewModel.todayCalories.collectAsState()

    var showAddDialog           by remember { mutableStateOf(false) }
    var showProfileDialog       by remember { mutableStateOf(false) }
    var showManualMacroDialog   by remember { mutableStateOf(false) }
    var showImageSourceSheet    by remember { mutableStateOf(false) }

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

    // ── Camera launcher (returns Bitmap preview directly, no FileProvider needed) ──
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { viewModel.scanFood(it) }
    }

    // ── Gallery launcher ──────────────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            runCatching {
                val bmp = android.graphics.BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(uri)
                )
                bmp?.let { viewModel.scanFood(it) }
            }
        }
    }

    // ── Camera permission launcher ────────────────────────────────────────────
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
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

                // ── Nutrición de hoy ──────────────────────────────────────
                item { SectionHeader("NUTRICIÓN DE HOY") }
                item {
                    NutritionTodayCard(
                        calories     = todayCalories,
                        proteinG     = todayProtein,
                        carbsG       = todayCarbs,
                        fatG         = todayFat,
                        fiberG       = todayFiber,
                        targetCal    = macros.calories,
                        targetProtein = macros.proteinG,
                        targetCarbs  = macros.carbsG,
                        targetFat    = macros.fatG,
                        onScanClick  = { showImageSourceSheet = true },
                        onManualClick = { showManualMacroDialog = true },
                        onReset      = { viewModel.resetTodayMacros() },
                        scanState    = scanState
                    )
                }

                // ── Historial de comidas del día ──────────────────────────
                if (todayLogs.isNotEmpty()) {
                    item { SectionHeader("COMIDAS REGISTRADAS HOY") }
                    items(todayLogs) { log ->
                        NutritionLogItem(log = log)
                    }
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

    // ── Image source bottom sheet ─────────────────────────────────────────────
    if (showImageSourceSheet) {
        ImageSourceSheet(
            onDismiss  = { showImageSourceSheet = false },
            onCamera   = {
                showImageSourceSheet = false
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) cameraLauncher.launch(null)
                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onGallery  = {
                showImageSourceSheet = false
                galleryLauncher.launch("image/*")
            }
        )
    }

    // ── Scan result dialog ────────────────────────────────────────────────────
    if (scanState is ScanState.Success) {
        ScanResultDialog(
            result    = (scanState as ScanState.Success).result,
            onConfirm = { viewModel.logNutrition(it) },
            onDismiss = { viewModel.dismissScan() }
        )
    }
    if (scanState is ScanState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissScan() },
            title = { Text("Error al escanear") },
            text  = { Text((scanState as ScanState.Error).message) },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissScan() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("OK", color = Color.Black) }
            }
        )
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
        AddMetricDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { weight, fat, muscle ->
                viewModel.addMetric(weight, fat, muscle)
                showAddDialog = false
            }
        )
    }
    if (showManualMacroDialog) {
        ManualMacroDialog(
            onDismiss = { showManualMacroDialog = false },
            onConfirm = { p, c, f, kcal, fiber ->
                viewModel.addMacrosManual(p, c, f, kcal, fiber)
                showManualMacroDialog = false
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

// ── Nutrition today card ──────────────────────────────────────────────────────

@Composable
private fun NutritionTodayCard(
    calories: Int,
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
    fiberG: Double,
    targetCal: Int,
    targetProtein: Int,
    targetCarbs: Int,
    targetFat: Int,
    onScanClick: () -> Unit,
    onManualClick: () -> Unit,
    onReset: () -> Unit,
    scanState: ScanState
) {
    val calProgress     = if (targetCal > 0) (calories.toFloat() / targetCal).coerceIn(0f, 1f) else 0f
    val proteinProgress = if (targetProtein > 0) (proteinG.toFloat() / targetProtein).coerceIn(0f, 1f) else 0f
    val carbsProgress   = if (targetCarbs > 0) (carbsG.toFloat() / targetCarbs).coerceIn(0f, 1f) else 0f
    val fatProgress     = if (targetFat > 0) (fatG.toFloat() / targetFat).coerceIn(0f, 1f) else 0f

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 16.dp,
        containerColor = Color(0xFF16162A),
        borderColor = Primary.copy(alpha = 0.22f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Header + action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "$calories",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary,
                            lineHeight = 30.sp
                        )
                        Text(
                            text = "/ $targetCal kcal",
                            fontSize = 14.sp,
                            color = Color(0xFF888888),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text("calorías consumidas hoy", fontSize = 11.sp, color = Color(0xFF666666))
                }
                Surface(
                    color = Primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${(calProgress * 100).roundToInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            // Calories progress bar
            LinearProgressIndicator(
                progress = { calProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Primary,
                trackColor = Color.White.copy(alpha = 0.07f)
            )

            // Macro progress rows
            MacroProgressRow("🥩 Proteína", proteinG, targetProtein.toDouble(), "g", Color(0xFFFF7043), proteinProgress)
            MacroProgressRow("🌾 Carbos",   carbsG,   targetCarbs.toDouble(),   "g", AmberAccent,      carbsProgress)
            MacroProgressRow("🥑 Grasas",   fatG,     targetFat.toDouble(),     "g", GreenAccent,      fatProgress)
            if (fiberG > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🌿 Fibra", fontSize = 12.sp, color = Color(0xFF888888))
                    Text("%.1fg".format(fiberG), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scan food button
                Button(
                    onClick = onScanClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    enabled = scanState !is ScanState.Loading
                ) {
                    if (scanState is ScanState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Analizando...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("Escanear comida", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
                // Manual entry
                OutlinedButton(
                    onClick = onManualClick,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF888888))
                    Spacer(Modifier.width(4.dp))
                    Text("Manual", fontSize = 13.sp, color = Color(0xFF888888))
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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroProgressRow(
    label: String,
    current: Double,
    target: Double,
    unit: String,
    color: Color,
    progress: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = Color(0xFF888888))
            Text(
                "%.0f / %.0f%s".format(current, target, unit),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.07f)
        )
    }
}

// ── Nutrition log item ────────────────────────────────────────────────────────

@Composable
private fun NutritionLogItem(log: NutritionLog) {
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
                        text = "🍽",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        log.description.replaceFirstChar { it.uppercase() },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "P: %.0fg  C: %.0fg  G: %.0fg".format(log.proteinG, log.carbsG, log.fatG),
                        fontSize = 10.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
            Text(
                "${log.calories} kcal",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
    }
}

// ── Image source bottom sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageSourceSheet(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Escanear comida con IA",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Toma una foto o selecciona una imagen y Gemini estimará los macronutrientes.",
                fontSize = 12.sp,
                color = Color(0xFF888888),
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onCamera,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Usar cámara", fontWeight = FontWeight.Bold, color = Color.Black)
            }
            OutlinedButton(
                onClick = onGallery,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.Photo, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Elegir de galería", color = Primary)
            }
        }
    }
}

// ── Scan result dialog ────────────────────────────────────────────────────────

@Composable
private fun ScanResultDialog(
    result: ScannedMacros,
    onConfirm: (ScannedMacros) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* bloqueado — usar botones */ },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Comida detectada", fontWeight = FontWeight.Bold)
                Text(
                    result.description.replaceFirstChar { it.uppercase() },
                    fontSize = 13.sp,
                    color = Primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Macronutrientes estimados por Gemini:",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
                ScanResultRow("🔥 Calorías",      "${result.calories} kcal",     Primary)
                ScanResultRow("🥩 Proteína",       "%.1fg".format(result.proteinG), Color(0xFFFF7043))
                ScanResultRow("🌾 Carbohidratos",  "%.1fg".format(result.carbsG),   AmberAccent)
                ScanResultRow("🥑 Grasas",         "%.1fg".format(result.fatG),     GreenAccent)
                if (result.fiberG > 0) {
                    ScanResultRow("🌿 Fibra",      "%.1fg".format(result.fiberG),   Color(0xFF4CAF50))
                }
                Text(
                    "⚠️ Valores estimados. Pueden variar según el tamaño de la porción.",
                    fontSize = 10.sp,
                    color = Color(0xFF666666),
                    lineHeight = 13.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(result) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text("Agregar al día", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ScanResultRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFFCCCCCC))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ── Manual macro dialog ───────────────────────────────────────────────────────

@Composable
private fun ManualMacroDialog(
    onDismiss: () -> Unit,
    onConfirm: (proteinG: Double, carbsG: Double, fatG: Double, calories: Int, fiberG: Double) -> Unit
) {
    var protein  by remember { mutableStateOf("") }
    var carbs    by remember { mutableStateOf("") }
    var fat      by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var fiber    by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar macros manualmente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroTextField("🔥 Calorías (kcal)", calories) { calories = it }
                MacroTextField("🥩 Proteína (g)",    protein)  { protein = it }
                MacroTextField("🌾 Carbohidratos (g)", carbs)  { carbs = it }
                MacroTextField("🥑 Grasas (g)",       fat)     { fat = it }
                MacroTextField("🌿 Fibra (g) — opcional", fiber) { fiber = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p  = protein.toDoubleOrNull()  ?: 0.0
                    val c  = carbs.toDoubleOrNull()    ?: 0.0
                    val f  = fat.toDoubleOrNull()      ?: 0.0
                    val fi = fiber.toDoubleOrNull()    ?: 0.0
                    val kcal = calories.toIntOrNull()
                        ?: ((p * 4) + (c * 4) + (f * 9)).roundToInt()
                    onConfirm(p, c, f, kcal, fi)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) { Text("Agregar", color = Color.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun MacroTextField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, focusedLabelColor = Primary)
    )
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
