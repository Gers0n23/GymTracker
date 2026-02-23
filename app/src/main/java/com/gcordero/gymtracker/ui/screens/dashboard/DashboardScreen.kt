package com.gcordero.gymtracker.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcordero.gymtracker.ui.navigation.Screen
import java.util.Calendar

// ── Local design tokens ───────────────────────────────────────────────────────
private val Bg           = Color(0xFF0D0D0D)
private val CardSurface  = Color(0xFF161616)
private val CardSurface2 = Color(0xFF1E1E1E)
private val Indigo       = Color(0xFF6366F1)
private val IndigoLight  = Color(0xFF818CF8)
private val IndigoDim    = Color(0x266366F1)
private val Green        = Color(0xFF34D399)
private val Amber        = Color(0xFFFBBF24)
private val Red          = Color(0xFFF87171)
private val Txt1         = Color(0xFFF0F0F0)
private val Txt2         = Color(0xFF888888)
private val Txt3         = Color(0xFF4A4A4A)
private val Border       = Color(0x12FFFFFF)

// ── Domain ────────────────────────────────────────────────────────────────────
enum class MuscleStatus { TODAY, READY, RECOVERING, FATIGUED }

data class MuscleGroup(
    val id: String,
    val name: String,
    val status: MuscleStatus,
    val restInfo: String,
    val volumes: List<Int>
)

enum class TipType { WEIGHT, REPS, SETS }
data class SmartTip(val exercise: String, val message: String, val type: TipType)

data class TodayWorkout(
    val dayTag: String,
    val routineName: String,
    val exercises: List<String>,
    val extraCount: Int,
    val estimatedMinutes: Int
)

private fun getTodayWorkout(): TodayWorkout? = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY    -> TodayWorkout("LUNES · HOY",     "Espalda + Bíceps + Core",          listOf("Jalón al Pecho", "Remo con Barra", "Curl de Bíceps"), 4, 55)
    Calendar.TUESDAY   -> TodayWorkout("MARTES · HOY",    "Piernas — Cuádriceps + Glúteos",   listOf("Sentadilla", "Prensa", "Extensiones"),                1, 50)
    Calendar.WEDNESDAY -> TodayWorkout("MIÉRCOLES · HOY", "Cardio + Core Postural",            listOf("Caminata", "Elíptica", "Crunch"),                     6, 60)
    Calendar.THURSDAY  -> TodayWorkout("JUEVES · HOY",    "Pecho + Tríceps + Hombros + Core", listOf("Press Banca", "Fondos", "Press Militar"),             5, 60)
    Calendar.FRIDAY    -> TodayWorkout("VIERNES · HOY",   "Piernas — Isquios + Glúteos",       listOf("P. Muerto Rumano", "Curl Femoral", "Hip Thrust"),     2, 50)
    else               -> null  // Sábado / Domingo — descanso
}

private val tipsByDay: Map<Int, List<SmartTip>> = mapOf(
    Calendar.MONDAY to listOf(
        SmartTip("Jalón al Pecho",     "Sube 2.5 kg — completaste todas las reps al 100% las últimas 3 sesiones",  TipType.WEIGHT),
        SmartTip("Curl de Bíceps",     "Aumenta a 12 reps — llegaste a 10/10 sin esfuerzo la última sesión",       TipType.REPS),
        SmartTip("Remo con Barra",     "Añade 1 serie extra — volumen en espalda por debajo del objetivo semanal", TipType.SETS)
    ),
    Calendar.TUESDAY to listOf(
        SmartTip("Sentadilla con Barra",      "Sube 2.5 kg — completaste todas las series al 100% las últimas 3 sesiones",    TipType.WEIGHT),
        SmartTip("Extensiones de Cuádriceps", "Aumenta a 12 reps — llegaste a 10/10 sin esfuerzo en la última sesión",        TipType.REPS),
        SmartTip("Prensa de Piernas",         "Añade 1 serie extra — volumen en cuádriceps por debajo del objetivo semanal",  TipType.SETS)
    ),
    Calendar.WEDNESDAY to listOf(
        SmartTip("Crunch Abdominal",    "Aumenta a 15 reps — completaste 12/12 sin esfuerzo la última sesión",    TipType.REPS),
        SmartTip("Plancha Isométrica",  "Extiende a 45 s — llevas 3 sesiones completando los 30 s sin problema",  TipType.WEIGHT),
        SmartTip("Elíptica",            "Añade 5 min de intervalo — tu resistencia cardiovascular ha mejorado",   TipType.SETS)
    ),
    Calendar.THURSDAY to listOf(
        SmartTip("Press de Banca",      "Sube 2.5 kg — completaste todas las series al 100% las últimas 3 sesiones", TipType.WEIGHT),
        SmartTip("Press Militar",       "Aumenta a 10 reps — llegaste a 8/8 en la última sesión",                    TipType.REPS),
        SmartTip("Fondos en Paralelas", "Añade 1 serie extra — volumen en tríceps por debajo del objetivo semanal",  TipType.SETS)
    ),
    Calendar.FRIDAY to listOf(
        SmartTip("Peso Muerto Rumano",  "Sube 5 kg — completaste todas las series al 100% las últimas 2 sesiones",  TipType.WEIGHT),
        SmartTip("Hip Thrust",          "Aumenta a 12 reps — llegaste a 10/10 sin esfuerzo la última sesión",       TipType.REPS),
        SmartTip("Curl Femoral",        "Añade 1 serie extra — volumen en isquios por debajo del objetivo semanal", TipType.SETS)
    )
)

// ── Mock data ─────────────────────────────────────────────────────────────────
private val muscleGroups = mapOf(
    "shoulders"  to MuscleGroup("shoulders",  "Hombros",        MuscleStatus.RECOVERING, "18h para recuperación completa",       listOf(65, 72, 68, 55)),
    "chest"      to MuscleGroup("chest",      "Pecho",          MuscleStatus.FATIGUED,   "Entrena en ~32h — no recomendado hoy", listOf(85, 78, 90, 95)),
    "biceps"     to MuscleGroup("biceps",     "Bíceps",         MuscleStatus.RECOVERING, "12h para recuperación completa",       listOf(30, 35, 32, 28)),
    "core"       to MuscleGroup("core",       "Core / Abdomen", MuscleStatus.READY,      "Totalmente recuperado",                listOf(20, 25, 22, 30)),
    "quads"      to MuscleGroup("quads",      "Cuádriceps",     MuscleStatus.TODAY,      "Objetivo de hoy — músculos frescos",   listOf(110, 98, 115, 120)),
    "calves"     to MuscleGroup("calves",     "Gemelos",        MuscleStatus.READY,      "Totalmente recuperado",                listOf(40, 38, 45, 42)),
    "back"       to MuscleGroup("back",       "Espalda",        MuscleStatus.READY,      "Totalmente recuperado",                listOf(180, 165, 190, 195)),
    "triceps"    to MuscleGroup("triceps",    "Tríceps",        MuscleStatus.RECOVERING, "8h para recuperación completa",        listOf(55, 60, 52, 58)),
    "glutes"     to MuscleGroup("glutes",     "Glúteos",        MuscleStatus.TODAY,      "Objetivo de hoy — músculos frescos",   listOf(95, 88, 100, 105)),
    "hamstrings" to MuscleGroup("hamstrings", "Isquiotibiales", MuscleStatus.READY,      "Totalmente recuperado",                listOf(70, 65, 75, 80))
)


// Tap zones: cx, cy, rx, ry (in dp), groupId
private data class TapZone(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val id: String)

private val frontTapZones = listOf(
    TapZone(25f, 46f, 15f, 13f, "shoulders"),
    TapZone(83f, 46f, 15f, 13f, "shoulders"),
    TapZone(45f, 57f, 17f, 15f, "chest"),
    TapZone(63f, 57f, 17f, 15f, "chest"),
    TapZone(20f, 66f, 10f, 17f, "biceps"),
    TapZone(88f, 66f, 10f, 17f, "biceps"),
    TapZone(54f, 82f, 18f, 15f, "core"),
    TapZone(40f, 132f, 12f, 27f, "quads"),
    TapZone(68f, 132f, 12f, 27f, "quads"),
    TapZone(39f, 172f, 11f, 18f, "calves"),
    TapZone(69f, 172f, 11f, 18f, "calves")
)

private val backTapZones = listOf(
    TapZone(54f, 58f, 26f, 23f, "back"),
    TapZone(20f, 66f, 10f, 17f, "triceps"),
    TapZone(88f, 66f, 10f, 17f, "triceps"),
    TapZone(43f, 104f, 15f, 14f, "glutes"),
    TapZone(65f, 104f, 15f, 14f, "glutes"),
    TapZone(40f, 136f, 12f, 24f, "hamstrings"),
    TapZone(68f, 136f, 12f, 24f, "hamstrings")
)

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: androidx.navigation.NavHostController,
    viewModel: DashboardViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val populateState by viewModel.populateState.collectAsState()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: "test_user"
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(null) }

    LaunchedEffect(populateState) {
        when (val s = populateState) {
            is PopulateState.Loading -> snackbarHostState.showSnackbar("Cargando datos...")
            is PopulateState.Success -> { snackbarHostState.showSnackbar("¡Base de Datos Poblada!"); viewModel.resetState() }
            is PopulateState.Error   -> { snackbarHostState.showSnackbar("Error: ${s.message}"); viewModel.resetState() }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("GymTracker", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = IndigoLight)
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.populateData(userId) },
                        enabled = populateState !is PopulateState.Loading
                    ) {
                        if (populateState is PopulateState.Loading)
                            CircularProgressIndicator(Modifier.size(12.dp), color = Txt3, strokeWidth = 1.5.dp)
                        else
                            Text("DEBUG", color = Txt3, fontSize = 10.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xEB0D0D0D))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xF20D0D0D), tonalElevation = 0.dp) {
                val nc = NavigationBarItemDefaults.colors(
                    selectedIconColor = Indigo, selectedTextColor = Indigo,
                    unselectedIconColor = Txt3, unselectedTextColor = Txt3,
                    indicatorColor = Color.Transparent
                )
                NavigationBarItem(true,  { },                                                   icon = { Icon(Icons.Default.Home,   null) }, label = { Text("Inicio") },    colors = nc)
                NavigationBarItem(false, { navController.navigate(Screen.Routines.route) },      icon = { Icon(Icons.Default.Star,   null) }, label = { Text("Rutinas") },   colors = nc)
                NavigationBarItem(false, { navController.navigate(Screen.WorkoutHistory.route) }, icon = { Icon(Icons.Default.List,   null) }, label = { Text("Historial") }, colors = nc)
                NavigationBarItem(false, { navController.navigate(Screen.BodyMetrics.route) },   icon = { Icon(Icons.Default.Person, null) }, label = { Text("Perfil") },    colors = nc)
            }
        },
        containerColor = Bg
    ) { padding ->
        val todayWorkout = remember { getTodayWorkout() }
        val todayDow     = remember { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) }
        val todayTips    = remember { tipsByDay[todayDow] ?: emptyList() }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { GreetingRow() }
            item { HeroWorkoutCard(navController, todayWorkout) }
            if (todayTips.isNotEmpty()) {
                item { SmartTipsSection(todayTips) }
            }
            item { MuscleRecoveryCard(onMuscleSelected = { selectedMuscle = it }) }
            item { WeeklyProgressCard() }
            item { StatsRow() }
        }
    }

    if (selectedMuscle != null) {
        MuscleDetailBottomSheet(muscle = selectedMuscle!!, onDismiss = { selectedMuscle = null })
    }
}

// ── Greeting ──────────────────────────────────────────────────────────────────
@Composable
private fun GreetingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text("Buenos días", fontSize = 13.sp, color = Txt2)
            Text("Gerson", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Txt1, letterSpacing = (-0.5).sp)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x14FBBF24))
                .border(1.dp, Color(0x30FBBF24), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("🔥", fontSize = 16.sp)
            Text("4",    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Amber)
            Text("días", fontSize = 9.sp, color = Txt2, letterSpacing = 0.5.sp)
        }
    }
}

// ── Hero workout card ─────────────────────────────────────────────────────────
@Composable
private fun HeroWorkoutCard(
    navController: androidx.navigation.NavHostController,
    workout: TodayWorkout?
) {
    // Rest day
    if (workout == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .border(1.dp, Border, RoundedCornerShape(20.dp))
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🛌", fontSize = 32.sp)
                Text("Día de descanso", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Txt1)
                Text("Hoy toca recuperar — lo estás haciendo bien", fontSize = 12.sp, color = Txt2)
            }
        }
        return
    }

    val chipList = workout.exercises + if (workout.extraCount > 0) listOf("+${workout.extraCount} más") else emptyList()
    val totalExercises = workout.exercises.size + workout.extraCount

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0D0D28), Color(0xFF141030), Color(0xFF1A1040))))
            .border(1.dp, Color(0x48818CF8), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        // Glow spot top-right
        Box(
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .background(Brush.radialGradient(listOf(Color(0x386366F1), Color.Transparent)), CircleShape)
        )
        Column {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(IndigoDim)
                    .border(1.dp, Color(0x336366F1), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(workout.dayTag, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = IndigoLight, letterSpacing = 1.4.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(workout.routineName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Txt1, letterSpacing = (-0.4).sp)
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$totalExercises ejercicios", fontSize = 12.sp, color = Txt2)
                Text("·",                          fontSize = 12.sp, color = Txt2)
                Text("~${workout.estimatedMinutes} min", fontSize = 12.sp, color = Txt2)
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chipList.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x10FFFFFF))
                            .border(1.dp, Border, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(chip, fontSize = 11.sp, color = Txt2)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate(Screen.Routines.route) },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo)
            ) {
                Text("▶  Iniciar Entrenamiento", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

// ── Smart tips ────────────────────────────────────────────────────────────────
@Composable
private fun SmartTipsSection(tips: List<SmartTip>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RECOMENDACIONES PARA HOY", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Txt2, letterSpacing = 0.8.sp)
            Text("Basadas en tu historial",  fontSize = 10.sp, color = Txt3)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tips.forEach { SmartTipCard(it) }
        }
    }
}

@Composable
private fun SmartTipCard(tip: SmartTip) {
    val (symbol, iconBg, iconFg) = when (tip.type) {
        TipType.WEIGHT -> Triple("↑", Color(0x1F34D399), Green)
        TipType.REPS   -> Triple("+", IndigoDim,         IndigoLight)
        TipType.SETS   -> Triple("≡", Color(0x1FFBBF24), Amber)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = iconFg)
        }
        Column {
            Text(tip.exercise, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Txt1)
            Spacer(Modifier.height(2.dp))
            Text(tip.message, fontSize = 11.sp, color = Txt2, lineHeight = 16.sp)
        }
    }
}

// ── Muscle recovery card ──────────────────────────────────────────────────────
@Composable
private fun MuscleRecoveryCard(onMuscleSelected: (MuscleGroup) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, Border, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RECUPERACIÓN MUSCULAR", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Txt2, letterSpacing = 0.8.sp)
            Text("Toca un grupo", fontSize = 10.sp, color = Txt3)
        }
        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Hoy" to Indigo, "Listo" to Green, "Cargando" to Amber, "Fatigado" to Red).forEach { (label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                    Text(label, fontSize = 10.sp, color = Txt2)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        // Front + Back side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Frontal",   fontSize = 9.sp, color = Txt3, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(6.dp))
                BodyCanvas(tapZones = frontTapZones, drawZones = { drawFrontZones() }, onMuscleSelected = onMuscleSelected)
            }
            Spacer(Modifier.width(20.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Posterior", fontSize = 9.sp, color = Txt3, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(6.dp))
                BodyCanvas(tapZones = backTapZones, drawZones = { drawBackZones() }, onMuscleSelected = onMuscleSelected)
            }
        }
    }
}

@Composable
private fun BodyCanvas(
    tapZones: List<TapZone>,
    drawZones: DrawScope.() -> Unit,
    onMuscleSelected: (MuscleGroup) -> Unit
) {
    Canvas(
        modifier = Modifier
            .width(108.dp)
            .height(216.dp)
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val hit = tapZones.firstOrNull { z ->
                        val cx = z.cx.dp.toPx()
                        val cy = z.cy.dp.toPx()
                        val rx = z.rx.dp.toPx()
                        val ry = z.ry.dp.toPx()
                        val dx = (tap.x - cx) / rx
                        val dy = (tap.y - cy) / ry
                        (dx * dx + dy * dy) <= 1f
                    }
                    hit?.let { muscleGroups[it.id]?.let(onMuscleSelected) }
                }
            }
    ) {
        drawBodySilhouette()
        drawZones()
    }
}

// ── Weekly progress ───────────────────────────────────────────────────────────
@Composable
private fun WeeklyProgressCard() {
    val days      = listOf("L", "M", "X", "J", "V", "S", "D")
    val heights   = listOf(0.62f, 0f, 0.78f, 1.0f, 0.55f, 0.08f, 0.08f)
    val todayIdx  = 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, Border, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("ÚLTIMOS 7 DÍAS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Txt2, letterSpacing = 0.8.sp)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("45,200 kg", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Txt1)
                Text("+8%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Green)
            }
        }
        // Bars via Canvas
        Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            val gap      = 5.dp.toPx()
            val barW     = (size.width - gap * 6) / 7f
            heights.forEachIndexed { i, frac ->
                val x     = i * (barW + gap)
                val color = when {
                    i == todayIdx -> Indigo.copy(alpha = 0.38f)
                    frac > 0.1f  -> Indigo.copy(alpha = 0.55f)
                    else         -> CardSurface2
                }
                val barH  = if (i == todayIdx) 4.dp.toPx() else (size.height * frac)
                val y     = size.height - barH
                drawRoundRect(color, topLeft = Offset(x, y), size = Size(barW, barH), cornerRadius = CornerRadius(4.dp.toPx()))
            }
        }
        Spacer(Modifier.height(8.dp))
        // Day labels
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            days.forEachIndexed { i, d ->
                Text(
                    d,
                    fontSize = 10.sp,
                    fontWeight = if (i == todayIdx) FontWeight.Bold else FontWeight.Normal,
                    color = if (i == todayIdx) Indigo else Txt3
                )
            }
        }
    }
}

// ── Stats row ─────────────────────────────────────────────────────────────────
@Composable
private fun StatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            Triple("12",  "Sesiones\n/ mes",    Txt1),
            Triple("3",   "PRs esta\nsemana",   Amber),
            Triple("4/5", "Días\nactivos",      Green)
        ).forEach { (value, label, color) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardSurface)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                Spacer(Modifier.height(3.dp))
                Text(label, fontSize = 9.sp, color = Txt2, letterSpacing = 0.5.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
            }
        }
    }
}

// ── Muscle detail bottom sheet ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleDetailBottomSheet(muscle: MuscleGroup, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 4.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Txt3))
            }
        }
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 40.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(muscle.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Txt1)
                val (badge, bg, fg) = when (muscle.status) {
                    MuscleStatus.TODAY      -> Triple("Hoy",      IndigoDim,           IndigoLight)
                    MuscleStatus.READY      -> Triple("Listo",    Color(0x1F34D399),   Green)
                    MuscleStatus.RECOVERING -> Triple("Cargando", Color(0x1FFBBF24),   Amber)
                    MuscleStatus.FATIGUED   -> Triple("Fatigado", Color(0x1FF87171),   Red)
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(badge, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
                }
            }
            Text(muscle.restInfo, fontSize = 12.sp, color = Txt2)
            Spacer(Modifier.height(20.dp))
            Text("VOLUMEN · ÚLTIMAS 4 SEMANAS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Txt2, letterSpacing = 0.9.sp)
            Spacer(Modifier.height(12.dp))
            MiniVolumeChart(muscle.volumes)
        }
    }
}

@Composable
private fun MiniVolumeChart(volumes: List<Int>) {
    val max    = volumes.max().toFloat()
    val labels = listOf("S-3", "S-2", "S-1", "Esta sem.")
    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        volumes.forEachIndexed { i, vol ->
            val isCurrent = i == volumes.lastIndex
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val frac = vol / max
                    val barH = size.height * frac
                    drawRoundRect(
                        color = if (isCurrent) Indigo else Indigo.copy(alpha = 0.3f),
                        topLeft = Offset(0f, size.height - barH),
                        size = Size(size.width, barH),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(labels[i], fontSize = 9.sp, color = Txt3, textAlign = TextAlign.Center)
            }
        }
    }
}

// ── Canvas draw helpers ───────────────────────────────────────────────────────
private fun DrawScope.drawBodySilhouette() {
    val fill   = Color.White.copy(alpha = 0.06f)
    val stroke = Color.White.copy(alpha = 0.22f)
    val sw     = 1.dp.toPx()
    val cr3    = CornerRadius(3.dp.toPx())

    // Head
    drawCircle(fill,   radius = 13.dp.toPx(), center = Offset(54.dp.toPx(), 17.dp.toPx()))
    drawCircle(stroke, radius = 13.dp.toPx(), center = Offset(54.dp.toPx(), 17.dp.toPx()), style = Stroke(sw))
    // Neck
    drawRoundRect(fill,   Offset(48.dp.toPx(), 29.dp.toPx()), Size(12.dp.toPx(), 10.dp.toPx()), cr3)
    drawRoundRect(stroke, Offset(48.dp.toPx(), 29.dp.toPx()), Size(12.dp.toPx(), 10.dp.toPx()), cr3, style = Stroke(sw))
    // Torso
    drawRoundRect(fill,   Offset(30.dp.toPx(), 39.dp.toPx()), Size(48.dp.toPx(), 56.dp.toPx()), CornerRadius(6.dp.toPx()))
    drawRoundRect(stroke, Offset(30.dp.toPx(), 39.dp.toPx()), Size(48.dp.toPx(), 56.dp.toPx()), CornerRadius(6.dp.toPx()), style = Stroke(sw))
    // Left arm
    drawRoundRect(fill,   Offset(15.dp.toPx(), 39.dp.toPx()), Size(14.dp.toPx(), 58.dp.toPx()), CornerRadius(7.dp.toPx()))
    drawRoundRect(stroke, Offset(15.dp.toPx(), 39.dp.toPx()), Size(14.dp.toPx(), 58.dp.toPx()), CornerRadius(7.dp.toPx()), style = Stroke(sw))
    // Right arm
    drawRoundRect(fill,   Offset(79.dp.toPx(), 39.dp.toPx()), Size(14.dp.toPx(), 58.dp.toPx()), CornerRadius(7.dp.toPx()))
    drawRoundRect(stroke, Offset(79.dp.toPx(), 39.dp.toPx()), Size(14.dp.toPx(), 58.dp.toPx()), CornerRadius(7.dp.toPx()), style = Stroke(sw))
    // Pelvis
    drawRoundRect(fill,   Offset(31.dp.toPx(), 93.dp.toPx()),  Size(46.dp.toPx(), 16.dp.toPx()), CornerRadius(5.dp.toPx()))
    drawRoundRect(stroke, Offset(31.dp.toPx(), 93.dp.toPx()),  Size(46.dp.toPx(), 16.dp.toPx()), CornerRadius(5.dp.toPx()), style = Stroke(sw))
    // Left leg
    drawRoundRect(fill,   Offset(31.dp.toPx(), 107.dp.toPx()), Size(18.dp.toPx(), 75.dp.toPx()), CornerRadius(8.dp.toPx()))
    drawRoundRect(stroke, Offset(31.dp.toPx(), 107.dp.toPx()), Size(18.dp.toPx(), 75.dp.toPx()), CornerRadius(8.dp.toPx()), style = Stroke(sw))
    // Right leg
    drawRoundRect(fill,   Offset(59.dp.toPx(), 107.dp.toPx()), Size(18.dp.toPx(), 75.dp.toPx()), CornerRadius(8.dp.toPx()))
    drawRoundRect(stroke, Offset(59.dp.toPx(), 107.dp.toPx()), Size(18.dp.toPx(), 75.dp.toPx()), CornerRadius(8.dp.toPx()), style = Stroke(sw))
}

private fun DrawScope.drawMuscleOval(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
    val tl = Offset((cx - rx).dp.toPx(), (cy - ry).dp.toPx())
    val sz = Size((rx * 2).dp.toPx(), (ry * 2).dp.toPx())
    drawOval(color.copy(alpha = 0.45f), topLeft = tl, size = sz)
    drawOval(color.copy(alpha = 0.28f), topLeft = tl, size = sz, style = Stroke(1.dp.toPx()))
}

private fun DrawScope.drawMuscleRect(x: Float, y: Float, w: Float, h: Float, color: Color) {
    val tl = Offset(x.dp.toPx(), y.dp.toPx())
    val sz = Size(w.dp.toPx(), h.dp.toPx())
    val cr = CornerRadius(5.dp.toPx())
    drawRoundRect(color.copy(alpha = 0.45f), tl, sz, cr)
    drawRoundRect(color.copy(alpha = 0.28f), tl, sz, cr, style = Stroke(1.dp.toPx()))
}

private fun statusColor(s: MuscleStatus): Color = when (s) {
    MuscleStatus.TODAY      -> Indigo
    MuscleStatus.READY      -> Green
    MuscleStatus.RECOVERING -> Amber
    MuscleStatus.FATIGUED   -> Red
}

private fun DrawScope.drawFrontZones() {
    drawMuscleOval(25f, 46f, 11f, 9f,  statusColor(MuscleStatus.RECOVERING))  // shoulder L
    drawMuscleOval(83f, 46f, 11f, 9f,  statusColor(MuscleStatus.RECOVERING))  // shoulder R
    drawMuscleOval(45f, 57f, 13f, 11f, statusColor(MuscleStatus.FATIGUED))    // chest L
    drawMuscleOval(63f, 57f, 13f, 11f, statusColor(MuscleStatus.FATIGUED))    // chest R
    drawMuscleRect(15f, 53f, 11f, 26f, statusColor(MuscleStatus.RECOVERING))  // biceps L
    drawMuscleRect(82f, 53f, 11f, 26f, statusColor(MuscleStatus.RECOVERING))  // biceps R
    drawMuscleRect(40f, 71f, 28f, 21f, statusColor(MuscleStatus.READY))       // core
    drawMuscleRect(32f, 109f, 16f, 46f, statusColor(MuscleStatus.TODAY))      // quads L
    drawMuscleRect(60f, 109f, 16f, 46f, statusColor(MuscleStatus.TODAY))      // quads R
    drawMuscleRect(32f, 158f, 14f, 28f, statusColor(MuscleStatus.READY))      // calves L
    drawMuscleRect(62f, 158f, 14f, 28f, statusColor(MuscleStatus.READY))      // calves R
}

private fun DrawScope.drawBackZones() {
    drawMuscleOval(54f, 58f, 22f, 19f, statusColor(MuscleStatus.READY))       // back
    drawMuscleRect(15f, 53f, 11f, 26f, statusColor(MuscleStatus.RECOVERING))  // triceps L
    drawMuscleRect(82f, 53f, 11f, 26f, statusColor(MuscleStatus.RECOVERING))  // triceps R
    drawMuscleOval(43f, 104f, 11f, 10f, statusColor(MuscleStatus.TODAY))      // glutes L
    drawMuscleOval(65f, 104f, 11f, 10f, statusColor(MuscleStatus.TODAY))      // glutes R
    drawMuscleRect(32f, 116f, 16f, 42f, statusColor(MuscleStatus.READY))      // hamstrings L
    drawMuscleRect(60f, 116f, 16f, 42f, statusColor(MuscleStatus.READY))      // hamstrings R
}
