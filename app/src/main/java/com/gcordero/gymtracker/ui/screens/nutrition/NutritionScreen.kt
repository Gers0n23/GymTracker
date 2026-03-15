package com.gcordero.gymtracker.ui.screens.nutrition

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcordero.gymtracker.domain.model.MealType
import com.gcordero.gymtracker.domain.model.NutritionLog
import com.gcordero.gymtracker.ui.screens.metrics.ScannedMacros
import com.gcordero.gymtracker.ui.screens.metrics.ScanState
import kotlin.math.roundToInt

// ── Design tokens ─────────────────────────────────────────────────────────────
private val Bg           = Color(0xFF0D0D0D)
private val CardSurface  = Color(0xFF161616)
private val CardSurface2 = Color(0xFF1A1A2E)
private val Indigo       = Color(0xFF6366F1)
private val IndigoDim    = Color(0x266366F1)
private val Green        = Color(0xFF34D399)
private val Amber        = Color(0xFFFBBF24)
private val OrangeProt   = Color(0xFFFF7043)
private val Txt1         = Color(0xFFF0F0F0)
private val Txt2         = Color(0xFF888888)
private val Txt3         = Color(0xFF4A4A4A)
private val Border       = Color(0x12FFFFFF)
private val Red          = Color(0xFFF87171)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel = viewModel()
) {
    val context      = LocalContext.current
    val logs              by viewModel.logs.collectAsState()
    val goals             by viewModel.goals.collectAsState()
    val scanState         by viewModel.scanState.collectAsState()
    val pendingMeal       by viewModel.pendingMealType.collectAsState()
    val caloriesBurned    by viewModel.caloriesBurnedToday.collectAsState()

    // Dialog / sheet states
    var showAddSheet        by remember { mutableStateOf(false) }
    var showImageSourceSheet by remember { mutableStateOf(false) }
    var showTextInputDialog  by remember { mutableStateOf(false) }
    var logToDelete          by remember { mutableStateOf<NutritionLog?>(null) }
    var logToEdit            by remember { mutableStateOf<NutritionLog?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel.scanFood(it) }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            runCatching {
                val bmp = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                bmp?.let { b -> viewModel.scanFood(b) }
            }
        }
    }

    // Camera permission launcher
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    // Grouped logs by meal type
    val logsByMeal = remember(logs) {
        logs.groupBy { runCatching { MealType.valueOf(it.mealType) }.getOrDefault(MealType.SNACK) }
    }

    // Totals
    val totalCal  = logs.sumOf { it.calories }
    val totalProt = logs.sumOf { it.proteinG }
    val totalCarb = logs.sumOf { it.carbsG }
    val totalFat  = logs.sumOf { it.fatG }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Nutrición",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Indigo
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Bg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Daily summary card ─────────────────────────────────────────
            item {
                DailySummaryCard(
                    totalCal      = totalCal,
                    targetCal     = goals.calories,
                    totalProt     = totalProt,
                    targetProt    = goals.proteinG,
                    totalCarb     = totalCarb,
                    targetCarb    = goals.carbsG,
                    totalFat      = totalFat,
                    targetFat     = goals.fatG,
                    caloriesBurned = caloriesBurned
                )
            }

            // ── Meal sections ──────────────────────────────────────────────
            MealType.entries.forEach { mealType ->
                val mealLogs = logsByMeal[mealType] ?: emptyList()
                val mealCal  = mealLogs.sumOf { it.calories }

                item(key = "header_${mealType.name}") {
                    MealSectionHeader(
                        mealType = mealType,
                        totalCal = mealCal,
                        onAddClick = {
                            viewModel.setPendingMealType(mealType)
                            showAddSheet = true
                        }
                    )
                }

                if (mealLogs.isEmpty()) {
                    item(key = "empty_${mealType.name}") {
                        EmptyMealPlaceholder()
                    }
                } else {
                    mealLogs.forEach { log ->
                        item(key = log.id) {
                            MealLogItem(
                                log = log,
                                onTap = { logToEdit = log },
                                onLongPress = { logToDelete = log }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // ── Loading overlay ────────────────────────────────────────────────────
    if (scanState is ScanState.Loading) {
        Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardSurface2),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = Indigo, modifier = Modifier.size(40.dp))
                    Text("Analizando...", fontSize = 12.sp, color = Txt2)
                }
            }
        }
    }

    // ── Add meal bottom sheet ──────────────────────────────────────────────
    if (showAddSheet) {
        AddMealBottomSheet(
            mealType = pendingMeal,
            onDismiss = { showAddSheet = false },
            onPhoto = {
                showAddSheet = false
                showImageSourceSheet = true
            },
            onText = {
                showAddSheet = false
                showTextInputDialog = true
            }
        )
    }

    // ── Image source sheet ─────────────────────────────────────────────────
    if (showImageSourceSheet) {
        ImageSourceSheet(
            onDismiss = { showImageSourceSheet = false },
            onCamera = {
                showImageSourceSheet = false
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) cameraLauncher.launch(null)
                else cameraPermLauncher.launch(Manifest.permission.CAMERA)
            },
            onGallery = {
                showImageSourceSheet = false
                galleryLauncher.launch("image/*")
            }
        )
    }

    // ── Text input dialog ──────────────────────────────────────────────────
    if (showTextInputDialog) {
        TextFoodDialog(
            onDismiss = { showTextInputDialog = false },
            onAnalyze = { text ->
                showTextInputDialog = false
                viewModel.analyzeText(text)
            }
        )
    }

    // ── Confirm meal dialog (scan success) ─────────────────────────────────
    if (scanState is ScanState.Success) {
        ConfirmMealDialog(
            initial  = (scanState as ScanState.Success).result,
            mealType = pendingMeal,
            onConfirm = { macros, meal ->
                viewModel.logNutrition(macros, meal)
            },
            onReanalyze = { text -> viewModel.analyzeText(text) },
            onDismiss   = { viewModel.dismissScan() }
        )
    }

    // ── Error dialog ───────────────────────────────────────────────────────
    if (scanState is ScanState.Error) {
        var showRetryText by remember { mutableStateOf(false) }
        var retryInput    by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.dismissScan() },
            containerColor = CardSurface2,
            title = { Text("Error al analizar", color = Txt1, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        (scanState as ScanState.Error).message,
                        color = Txt2,
                        fontSize = 13.sp
                    )
                    OutlinedButton(
                        onClick = { showRetryText = !showRetryText },
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Indigo.copy(alpha = 0.4f))
                    ) {
                        Text("Describir con texto", color = Indigo, fontSize = 13.sp)
                    }
                    AnimatedVisibility(visible = showRetryText) {
                        OutlinedTextField(
                            value = retryInput,
                            onValueChange = { retryInput = it },
                            label = { Text("Ej: 2 huevos revueltos con tostada") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Indigo,
                                focusedLabelColor = Indigo,
                                unfocusedBorderColor = Txt3,
                                unfocusedTextColor = Txt1,
                                focusedTextColor = Txt1
                            )
                        )
                    }
                }
            },
            confirmButton = {
                if (showRetryText) {
                    Button(
                        onClick = {
                            if (retryInput.isNotBlank()) viewModel.analyzeText(retryInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                        enabled = retryInput.isNotBlank()
                    ) { Text("Analizar", color = Color.White) }
                } else {
                    Button(
                        onClick = { viewModel.dismissScan() },
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo)
                    ) { Text("OK", color = Color.White) }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissScan() }) {
                    Text("Cancelar", color = Txt2)
                }
            }
        )
    }

    // ── Edit log dialog ────────────────────────────────────────────────────
    logToEdit?.let { log ->
        EditLogDialog(
            log = log,
            onSave = { macros, meal ->
                viewModel.updateLog(log.id, macros, meal)
                logToEdit = null
            },
            onDismiss = { logToEdit = null }
        )
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────
    logToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            containerColor = CardSurface2,
            title = { Text("Eliminar comida", color = Txt1, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Eliminar \"${log.description.replaceFirstChar { it.uppercase() }}\"?",
                    color = Txt2
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLog(log.id)
                        logToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) { Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancelar", color = Txt2)
                }
            }
        )
    }
}

// ── Daily summary card ─────────────────────────────────────────────────────────

@Composable
private fun DailySummaryCard(
    totalCal: Int,
    targetCal: Int,
    totalProt: Double,
    targetProt: Int,
    totalCarb: Double,
    targetCarb: Int,
    totalFat: Double,
    targetFat: Int,
    caloriesBurned: Int = 0
) {
    val netCal  = totalCal - caloriesBurned
    val calPct  = if (targetCal > 0) (totalCal.toFloat() / targetCal).coerceIn(0f, 1f) else 0f
    val protPct = if (targetProt > 0) (totalProt.toFloat() / targetProt).coerceIn(0f, 1f) else 0f
    val carbPct = if (targetCarb > 0) (totalCarb.toFloat() / targetCarb).coerceIn(0f, 1f) else 0f
    val fatPct  = if (targetFat > 0) (totalFat.toFloat() / targetFat).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface2)
            .border(1.dp, Border, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Calorie hero
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
                        "$totalCal",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Indigo,
                        lineHeight = 36.sp
                    )
                    Text(
                        "/ $targetCal kcal",
                        fontSize = 14.sp,
                        color = Txt2,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text("calorías consumidas hoy", fontSize = 11.sp, color = Txt3)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(IndigoDim)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${(calPct * 100).roundToInt()}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Indigo
                )
            }
        }

        // Calorie bar
        LinearProgressIndicator(
            progress = { calPct },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Indigo,
            trackColor = Color.White.copy(alpha = 0.07f)
        )

        // Calorías quemadas + balance neto
        if (caloriesBurned > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrangeProt.copy(alpha = 0.08f))
                    .border(1.dp, OrangeProt.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Quemadas hoy", fontSize = 11.sp, color = Txt2)
                    Text(
                        "−$caloriesBurned kcal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeProt
                    )
                }
                Box(modifier = Modifier.height(36.dp).width(1.dp).background(OrangeProt.copy(alpha = 0.2f)))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Balance neto", fontSize = 11.sp, color = Txt2)
                    val balanceColor = when {
                        netCal > targetCal * 1.1 -> Red
                        netCal < 0              -> Green
                        else                    -> Txt1
                    }
                    Text(
                        "$netCal kcal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                }
            }
        }

        // Macro bars
        MacroProgressBar("Proteína", totalProt, targetProt.toDouble(), "g", OrangeProt, protPct)
        MacroProgressBar("Carbos",   totalCarb, targetCarb.toDouble(), "g", Amber,     carbPct)
        MacroProgressBar("Grasas",   totalFat,  targetFat.toDouble(),  "g", Green,     fatPct)
    }
}

@Composable
private fun MacroProgressBar(
    label: String,
    current: Double,
    target: Double,
    unit: String,
    color: Color,
    progress: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = Txt2)
            Text(
                "%.0f / %.0f%s".format(current, target, unit),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.07f)
        )
    }
}

// ── Meal section header ────────────────────────────────────────────────────────

@Composable
private fun MealSectionHeader(
    mealType: MealType,
    totalCal: Int,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(mealType.emoji, fontSize = 18.sp)
            Text(
                mealType.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Txt1
            )
            if (totalCal > 0) {
                Text(
                    "$totalCal kcal",
                    fontSize = 11.sp,
                    color = Txt3
                )
            }
        }
        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IndigoDim)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Agregar comida",
                tint = Indigo,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Empty meal placeholder ─────────────────────────────────────────────────────

@Composable
private fun EmptyMealPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Agrega tu primera comida",
            fontSize = 12.sp,
            color = Txt3
        )
    }
}

// ── Meal log item ──────────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MealLogItem(
    log: NutritionLog,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(IndigoDim),
                contentAlignment = Alignment.Center
            ) {
                Text("🍽", fontSize = 16.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    log.description.replaceFirstChar { it.uppercase() },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Txt1,
                    maxLines = 1
                )
                Text(
                    "P: %.0fg  C: %.0fg  G: %.0fg".format(log.proteinG, log.carbsG, log.fatG),
                    fontSize = 10.sp,
                    color = Txt2
                )
            }
        }
        Text(
            "${log.calories} kcal",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Indigo
        )
    }
}

// ── Add meal bottom sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMealBottomSheet(
    mealType: MealType,
    onDismiss: () -> Unit,
    onPhoto: () -> Unit,
    onText: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardSurface2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "${mealType.emoji}  Agregar a ${mealType.label}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Txt1
            )
            Text(
                "Elige cómo registrar tu comida:",
                fontSize = 12.sp,
                color = Txt2
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onPhoto,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Foto con IA", fontWeight = FontWeight.Bold, color = Color.White)
            }
            OutlinedButton(
                onClick = onText,
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Indigo.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Indigo,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Describir con texto", color = Indigo)
            }
        }
    }
}

// ── Image source sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageSourceSheet(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardSurface2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Escanear comida con IA",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Txt1
            )
            Text(
                "Toma una foto o selecciona una imagen y Gemini estimará los macronutrientes.",
                fontSize = 12.sp,
                color = Txt2,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onCamera,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Usar cámara", fontWeight = FontWeight.Bold, color = Color.White)
            }
            OutlinedButton(
                onClick = onGallery,
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Indigo.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = null,
                    tint = Indigo,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Elegir de galería", color = Indigo)
            }
        }
    }
}

// ── Text food dialog ───────────────────────────────────────────────────────────

@Composable
private fun TextFoodDialog(
    onDismiss: () -> Unit,
    onAnalyze: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface2,
        title = { Text("Describir comida", color = Txt1, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Describe lo que comiste y Gemini estimará los macros.",
                    fontSize = 12.sp,
                    color = Txt2,
                    lineHeight = 16.sp
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Ej: 2 huevos revueltos con tostada integral") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Indigo,
                        focusedLabelColor = Indigo,
                        unfocusedBorderColor = Txt3,
                        unfocusedTextColor = Txt1,
                        focusedTextColor = Txt1
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onAnalyze(text) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo)
            ) { Text("Analizar", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Txt2) }
        }
    )
}

// ── Confirm meal dialog ────────────────────────────────────────────────────────

@Composable
private fun ConfirmMealDialog(
    initial: ScannedMacros,
    mealType: MealType,
    onConfirm: (ScannedMacros, MealType) -> Unit,
    onReanalyze: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var description   by remember(initial) { mutableStateOf(initial.description) }
    var calories      by remember(initial) { mutableStateOf(initial.calories.toString()) }
    var protein       by remember(initial) { mutableStateOf("%.1f".format(initial.proteinG)) }
    var carbs         by remember(initial) { mutableStateOf("%.1f".format(initial.carbsG)) }
    var fat           by remember(initial) { mutableStateOf("%.1f".format(initial.fatG)) }
    var fiber         by remember(initial) { mutableStateOf("%.1f".format(initial.fiberG)) }
    var selectedMeal  by remember { mutableStateOf(mealType) }
    var showRetry     by remember { mutableStateOf(false) }
    var retryText     by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { /* bloqueado — usar botones */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress    = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface2)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Comida detectada",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Txt1
            )

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Indigo,
                    focusedLabelColor = Indigo,
                    unfocusedBorderColor = Txt3,
                    unfocusedTextColor = Txt1,
                    focusedTextColor = Txt1
                )
            )

            // Macro fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroEditField(
                    modifier = Modifier.weight(1f),
                    label = "kcal",
                    value = calories,
                    onChange = { calories = it },
                    color = Indigo
                )
                MacroEditField(
                    modifier = Modifier.weight(1f),
                    label = "Prot (g)",
                    value = protein,
                    onChange = { protein = it },
                    color = OrangeProt
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroEditField(
                    modifier = Modifier.weight(1f),
                    label = "Carbs (g)",
                    value = carbs,
                    onChange = { carbs = it },
                    color = Amber
                )
                MacroEditField(
                    modifier = Modifier.weight(1f),
                    label = "Grasas (g)",
                    value = fat,
                    onChange = { fat = it },
                    color = Green
                )
            }
            MacroEditField(
                modifier = Modifier.fillMaxWidth(),
                label = "Fibra (g)",
                value = fiber,
                onChange = { fiber = it },
                color = Txt2
            )

            // Meal type selector
            Text("Comida del día", fontSize = 11.sp, color = Txt2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MealType.entries.forEach { mt ->
                    val sel = selectedMeal == mt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) IndigoDim else CardSurface)
                            .border(
                                1.dp,
                                if (sel) Indigo else Border,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedMeal = mt }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(mt.emoji, fontSize = 14.sp)
                            Text(
                                mt.label,
                                fontSize = 8.sp,
                                color = if (sel) Indigo else Txt3,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Retry section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardSurface)
                    .clickable { showRetry = !showRetry }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "No es correcto",
                    fontSize = 12.sp,
                    color = Txt2
                )
                Icon(
                    if (showRetry) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Txt3,
                    modifier = Modifier.size(16.dp)
                )
            }
            AnimatedVisibility(
                visible = showRetry,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = retryText,
                        onValueChange = { retryText = it },
                        label = { Text("Describe la comida correcta", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Indigo,
                            focusedLabelColor = Indigo,
                            unfocusedBorderColor = Txt3,
                            unfocusedTextColor = Txt1,
                            focusedTextColor = Txt1
                        )
                    )
                    Button(
                        onClick = {
                            if (retryText.isNotBlank()) {
                                onReanalyze(retryText)
                            }
                        },
                        enabled = retryText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reanalizar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Txt3),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Cancelar", color = Txt2) }

                Button(
                    onClick = {
                        val macros = ScannedMacros(
                            description = description.ifBlank { "Comida" },
                            calories    = calories.toIntOrNull() ?: initial.calories,
                            proteinG    = protein.toDoubleOrNull() ?: initial.proteinG,
                            carbsG      = carbs.toDoubleOrNull() ?: initial.carbsG,
                            fatG        = fat.toDoubleOrNull() ?: initial.fatG,
                            fiberG      = fiber.toDoubleOrNull() ?: initial.fiberG
                        )
                        onConfirm(macros, selectedMeal)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Agregar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Edit log dialog ────────────────────────────────────────────────────────────

@Composable
private fun EditLogDialog(
    log: NutritionLog,
    onSave: (ScannedMacros, MealType) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMeal = runCatching { MealType.valueOf(log.mealType) }.getOrDefault(MealType.SNACK)

    var description  by remember { mutableStateOf(log.description) }
    var calories     by remember { mutableStateOf(log.calories.toString()) }
    var protein      by remember { mutableStateOf("%.1f".format(log.proteinG)) }
    var carbs        by remember { mutableStateOf("%.1f".format(log.carbsG)) }
    var fat          by remember { mutableStateOf("%.1f".format(log.fatG)) }
    var fiber        by remember { mutableStateOf("%.1f".format(log.fiberG)) }
    var selectedMeal by remember { mutableStateOf(initialMeal) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface2)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Editar comida",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Txt1
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Indigo,
                    focusedLabelColor = Indigo,
                    unfocusedBorderColor = Txt3,
                    unfocusedTextColor = Txt1,
                    focusedTextColor = Txt1
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroEditField(Modifier.weight(1f), "kcal",     calories, { calories = it }, Indigo)
                MacroEditField(Modifier.weight(1f), "Prot (g)", protein,  { protein  = it }, OrangeProt)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroEditField(Modifier.weight(1f), "Carbs (g)",  carbs, { carbs = it }, Amber)
                MacroEditField(Modifier.weight(1f), "Grasas (g)", fat,   { fat   = it }, Green)
            }
            MacroEditField(Modifier.fillMaxWidth(), "Fibra (g)", fiber, { fiber = it }, Txt2)

            Text("Comida del día", fontSize = 11.sp, color = Txt2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MealType.entries.forEach { mt ->
                    val sel = selectedMeal == mt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) IndigoDim else CardSurface)
                            .border(1.dp, if (sel) Indigo else Border, RoundedCornerShape(8.dp))
                            .clickable { selectedMeal = mt }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(mt.emoji, fontSize = 14.sp)
                            Text(
                                mt.label,
                                fontSize = 8.sp,
                                color = if (sel) Indigo else Txt3,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Txt3),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Cancelar", color = Txt2) }

                Button(
                    onClick = {
                        onSave(
                            ScannedMacros(
                                description = description.ifBlank { "Comida" },
                                calories    = calories.toIntOrNull() ?: log.calories,
                                proteinG    = protein.toDoubleOrNull() ?: log.proteinG,
                                carbsG      = carbs.toDoubleOrNull() ?: log.carbsG,
                                fatG        = fat.toDoubleOrNull() ?: log.fatG,
                                fiberG      = fiber.toDoubleOrNull() ?: log.fiberG
                            ),
                            selectedMeal
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MacroEditField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onChange: (String) -> Unit,
    color: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp, color = color) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = color,
            focusedLabelColor = color,
            unfocusedBorderColor = Txt3,
            unfocusedTextColor = Txt1,
            focusedTextColor = Txt1
        )
    )
}
