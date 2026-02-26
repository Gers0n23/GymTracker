package com.gcordero.gymtracker.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

private val Bg          = Color(0xFF0D0D0D)
private val Indigo      = Color(0xFF6366F1)
private val IndigoLight = Color(0xFF818CF8)
private val Txt2        = Color(0xFF888888)
private val Red         = Color(0xFFF87171)
private val GoogleBlue  = Color(0xFF4285F4)

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState         by viewModel.uiState.collectAsState()
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager    = LocalFocusManager.current
    val context         = LocalContext.current

    // Launcher para Google Sign-In
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn
                    .getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                viewModel.handleGoogleSignInResult(account?.idToken)
            } catch (_: ApiException) {
                // Usuario canceló o error silencioso — el ViewModel ya está en Idle
            }
        }
    }

    LaunchedEffect(email, password) {
        if (uiState is AuthUiState.Error) viewModel.resetState()
    }

    Box(
        modifier         = Modifier.fillMaxSize().background(Bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                "GymTracker",
                fontSize   = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = IndigoLight
            )
            Text("Inicia sesión para continuar", fontSize = 14.sp, color = Txt2)

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value         = email,
                onValueChange = { email = it },
                label         = { Text("Correo electrónico") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                colors = authFieldColors()
            )

            OutlinedTextField(
                value               = password,
                onValueChange       = { password = it },
                label               = { Text("Contraseña") },
                singleLine          = true,
                modifier            = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus(); viewModel.login(email, password) }
                ),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "Ocultar" else "Ver", color = Txt2, fontSize = 12.sp)
                    }
                },
                colors = authFieldColors()
            )

            if (uiState is AuthUiState.Error) {
                Text((uiState as AuthUiState.Error).message, color = Red, fontSize = 13.sp)
            }

            Button(
                onClick  = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = uiState !is AuthUiState.Loading,
                colors   = ButtonDefaults.buttonColors(containerColor = Indigo),
                shape    = RoundedCornerShape(12.dp)
            ) {
                if (uiState is AuthUiState.Loading)
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else
                    Text("Iniciar sesión", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            // ── Divisor ──────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x22FFFFFF))
                Text("  o  ", color = Txt2, fontSize = 13.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x22FFFFFF))
            }

            // ── Botón Google ─────────────────────────────────────────────────
            OutlinedButton(
                onClick  = { googleLauncher.launch(viewModel.createGoogleSignInIntent(context)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = uiState !is AuthUiState.Loading,
                border   = BorderStroke(1.dp, Color(0x44FFFFFF)),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF0F0F0))
            ) {
                Text("G", color = GoogleBlue, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text("Continuar con Google", fontWeight = FontWeight.Medium, fontSize = 15.sp)
            }

            TextButton(onClick = onNavigateToRegister) {
                Text("¿No tienes cuenta? Regístrate", color = IndigoLight, fontSize = 14.sp)
            }
        }
    }
}

@Composable
internal fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = Color(0xFF6366F1),
    unfocusedBorderColor    = Color(0x33FFFFFF),
    focusedTextColor        = Color(0xFFF0F0F0),
    unfocusedTextColor      = Color(0xFFF0F0F0),
    cursorColor             = Color(0xFF6366F1),
    focusedContainerColor   = Color(0xFF161616),
    unfocusedContainerColor = Color(0xFF161616),
    focusedLabelColor       = Color(0xFF818CF8),
    unfocusedLabelColor     = Color(0xFF888888)
)
