package com.gcordero.gymtracker.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.zIndex

// ── Design tokens ─────────────────────────────────────────────────────────────
private val Bg          = Color(0xFF0D0D0D)
private val CardSurface = Color(0xFF161616)
private val Indigo      = Color(0xFF6366F1)
private val IndigoLight = Color(0xFF818CF8)
private val IndigoDim   = Color(0x266366F1)
private val Txt1        = Color(0xFFF0F0F0)
private val Txt2        = Color(0xFF888888)
private val Border      = Color(0x12FFFFFF)
private val BorderSel   = Color(0xFF6366F1)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Text(
                "GymTracker",
                fontSize   = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = IndigoLight
            )

            // Step indicator
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 1).forEach { i ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (step == i) 32.dp else 16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (step == i) Indigo else Color(0xFF333333))
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (step == 0) {
                Step0Profile(
                    isMale      = uiState.isMale,
                    age         = uiState.age,
                    heightCm    = uiState.heightCm,
                    goalIndex   = uiState.goalIndex,
                    onSetMale   = viewModel::setIsMale,
                    onSetAge    = viewModel::setAge,
                    onSetHeight = viewModel::setHeightCm,
                    onSetGoal   = viewModel::setGoalIndex,
                    onNext      = { step = 1 }
                )
            } else {
                Step1Plan(
                    selectedPlan = uiState.selectedPlan,
                    isLoading    = uiState.isLoading,
                    error        = uiState.error,
                    onSelectPlan = viewModel::setSelectedPlan,
                    onBack       = { step = 0 },
                    onFinish     = viewModel::finish
                )
            }
        }

        // ── Loading overlay ─────────────────────────────────────────────────
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC0D0D0D))
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color       = Indigo,
                        strokeWidth = 3.dp,
                        modifier    = Modifier.size(48.dp)
                    )
                    Text(
                        "Creando tu rutina...",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Txt1
                    )
                    Text(
                        "Esto puede tardar unos segundos",
                        fontSize = 12.sp,
                        color    = Txt2
                    )
                }
            }
        }
    }
}

// ── Step 0: Profile ───────────────────────────────────────────────────────────

@Composable
private fun Step0Profile(
    isMale: Boolean,
    age: String,
    heightCm: String,
    goalIndex: Int,
    onSetMale: (Boolean) -> Unit,
    onSetAge: (String) -> Unit,
    onSetHeight: (String) -> Unit,
    onSetGoal: (Int) -> Unit,
    onNext: () -> Unit
) {
    Text(
        "Cuéntanos sobre ti",
        fontSize   = 22.sp,
        fontWeight = FontWeight.Bold,
        color      = Txt1,
        textAlign  = TextAlign.Center
    )
    Text(
        "Personaliza tu experiencia de entrenamiento",
        fontSize  = 13.sp,
        color     = Txt2,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(4.dp))

    // Sex toggle
    SectionLabel("Sexo biológico")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(true to "Hombre", false to "Mujer").forEach { (isMaleOpt, label) ->
            SelectableCard(
                modifier  = Modifier.weight(1f),
                label     = label,
                emoji     = if (isMaleOpt) "♂" else "♀",
                selected  = isMale == isMaleOpt,
                onClick   = { onSetMale(isMaleOpt) }
            )
        }
    }

    // Age + Height
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OnboardingField(
            modifier     = Modifier.weight(1f),
            value        = age,
            onValueChange = onSetAge,
            label        = "Edad",
            placeholder  = "25",
            suffix       = "años"
        )
        OnboardingField(
            modifier     = Modifier.weight(1f),
            value        = heightCm,
            onValueChange = onSetHeight,
            label        = "Altura",
            placeholder  = "175",
            suffix       = "cm"
        )
    }

    // Goal
    SectionLabel("Objetivo principal")
    val goals = listOf(
        "Hipertrofia"   to "💪",
        "Fuerza"        to "🏋️",
        "Definición"    to "🔥",
        "Salud general" to "❤️"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        goals.chunked(2).forEachIndexed { rowIdx, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEachIndexed { colIdx, (label, emoji) ->
                    val idx = rowIdx * 2 + colIdx
                    SelectableCard(
                        modifier = Modifier.weight(1f),
                        label    = label,
                        emoji    = emoji,
                        selected = goalIndex == idx,
                        onClick  = { onSetGoal(idx) }
                    )
                }
                // Fill last row if odd number
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick  = onNext,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = Indigo),
        shape    = RoundedCornerShape(13.dp)
    ) {
        Text("Continuar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

// ── Step 1: Plan ──────────────────────────────────────────────────────────────

@Composable
private fun Step1Plan(
    selectedPlan: OnboardingPlan,
    isLoading: Boolean,
    error: String?,
    onSelectPlan: (OnboardingPlan) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    Text(
        "Elige tu plan de entrenamiento",
        fontSize   = 22.sp,
        fontWeight = FontWeight.Bold,
        color      = Txt1,
        textAlign  = TextAlign.Center
    )
    Text(
        "Puedes cambiarlo o personalizarlo después",
        fontSize  = 13.sp,
        color     = Txt2,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(4.dp))

    val plans = listOf(
        OnboardingPlan.DAYS_5    to Triple("5 días / semana", "Rutina completa por grupos musculares: Espalda+Bíceps, Piernas (x2), Cardio+Core, Pecho+Hombros+Tríceps.", "🗓️"),
        OnboardingPlan.PPL       to Triple("Push / Pull / Legs", "3 días: Push (Lunes), Pull (Miércoles), Piernas (Viernes). Ideal para equilibrio entre volumen y descanso.", "🔄"),
        OnboardingPlan.FULL_BODY to Triple("Full Body — 3 días", "Una rutina para Lunes, Miércoles y Viernes. Trabaja todo el cuerpo cada sesión. Perfecto para principiantes.", "⚡"),
        OnboardingPlan.NONE      to Triple("Empezar desde cero", "No creo ninguna rutina ahora. La configuraré manualmente.", "✏️")
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        plans.forEach { (plan, triple) ->
            val (title, desc, emoji) = triple
            PlanCard(
                emoji    = emoji,
                title    = title,
                desc     = desc,
                selected = selectedPlan == plan,
                onClick  = { onSelectPlan(plan) }
            )
        }
    }

    if (error != null) {
        Text(error, color = Color(0xFFF87171), fontSize = 13.sp, textAlign = TextAlign.Center)
    }

    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick  = onBack,
            modifier = Modifier.weight(1f).height(52.dp),
            enabled  = !isLoading,
            shape    = RoundedCornerShape(13.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = IndigoLight),
            border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
        ) {
            Text("Atrás", fontWeight = FontWeight.Medium)
        }
        Button(
            onClick  = onFinish,
            modifier = Modifier.weight(2f).height(52.dp),
            enabled  = !isLoading,
            colors   = ButtonDefaults.buttonColors(containerColor = Indigo),
            shape    = RoundedCornerShape(13.dp)
        ) {
            if (isLoading)
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else
                Text("¡Empezar!", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize   = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color      = Txt2,
        letterSpacing = 0.6.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SelectableCard(
    modifier: Modifier,
    label: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg     = if (selected) IndigoDim  else CardSurface
    val border = if (selected) BorderSel  else Border

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Txt1)
    }
}

@Composable
private fun PlanCard(
    emoji: String,
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg     = if (selected) IndigoDim  else CardSurface
    val border = if (selected) BorderSel  else Border

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 22.sp, modifier = Modifier.padding(top = 2.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Txt1)
            Spacer(Modifier.height(3.dp))
            Text(desc, fontSize = 11.sp, color = Txt2, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun OnboardingField(
    modifier: Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    suffix: String
) {
    OutlinedTextField(
        value         = value,
        onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) onValueChange(it) },
        label         = { Text(label) },
        placeholder   = { Text(placeholder, color = Txt2) },
        suffix        = { Text(suffix, color = Txt2, fontSize = 12.sp) },
        singleLine    = true,
        modifier      = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = Indigo,
            unfocusedBorderColor    = Color(0x33FFFFFF),
            focusedTextColor        = Txt1,
            unfocusedTextColor      = Txt1,
            cursorColor             = Indigo,
            focusedContainerColor   = CardSurface,
            unfocusedContainerColor = CardSurface,
            focusedLabelColor       = IndigoLight,
            unfocusedLabelColor     = Txt2
        )
    )
}
