package com.example.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: ClubViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (isAdmin: Boolean) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val loginError by viewModel.loginError.collectAsState()

    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.background, colors.primaryContainer.copy(alpha = 0.3f))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Club branding header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SportsSoccer,
                    contentDescription = "Soccer icon",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(50.dp)
                )
            }

            Text(
                text = "Socio Club",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Gestión de Cuotas & Pagos",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Custom error message
            if (loginError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Error icon",
                            tint = colors.error
                        )
                        Text(
                            text = loginError ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.onErrorContainer)
                        )
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                placeholder = { Text("ejemplo@club.com") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email icon") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_email_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Lock icon") },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input"),
                shape = RoundedCornerShape(14.dp)
            )

            // Login Trigger
            Button(
                onClick = {
                    viewModel.login(email, password) { success ->
                        if (success) {
                            val user = viewModel.currentUser.value
                            onLoginSuccess(user?.role == "ADMIN")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_login_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ingresar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Informational Centralized Login Note
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "info",
                        tint = colors.primary
                    )
                    Text(
                        text = "El registro de socios está centralizado. La administración genera tu cuenta y contraseña inicial, la cual podrás cambiar al ingresar.",
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.onPrimaryContainer),
                        textAlign = TextAlign.Start
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = colors.onBackground.copy(alpha = 0.15f)
            )

            // Dynamic Demo Shortcuts Card
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚡ Acceso Rápido De Prueba",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = colors.primary)
                    )
                    Text(
                        text = "Usa estos accesos directos para probar ambos roles inmediatamente:",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = colors.onBackground.copy(alpha = 0.7f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.login("socio@club.com", "123") { success ->
                                    if (success) onLoginSuccess(false)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("demo_socio_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.secondary,
                                contentColor = colors.onSecondary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Socio Demo", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.login("admin@club.com", "123") { success ->
                                    if (success) onLoginSuccess(true)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("demo_admin_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.tertiary,
                                contentColor = colors.onTertiary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Admin Demo", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: ClubViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: (isAdmin: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    val registerError by viewModel.registerError.collectAsState()
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.background, colors.primaryContainer.copy(alpha = 0.3f))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = "Register logo",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Nueva Cuenta Club",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Únete para registrar tu carnet digital y estar al día",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            if (registerError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Error notification",
                            tint = colors.error
                        )
                        Text(
                            text = registerError ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.onErrorContainer)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre Completo") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "User icon") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_name_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                placeholder = { Text("socio@ejemplo.com") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email icon") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_email_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Teléfono / WhatsApp") },
                placeholder = { Text("+54 9 11 ...") },
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = "Phone icon") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_phone_input"),
                shape = RoundedCornerShape(14.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Establecer Contraseña") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Lock icon") },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_password_input"),
                shape = RoundedCornerShape(14.dp)
            )

            // Role selection with beautiful card switcher
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAdmin) "🔑 Cuenta de Administrador" else "👤 Cuenta de Socio General",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = colors.primary)
                        )
                        Text(
                            text = if (isAdmin) "Permite registrar cuotas y pagos" else "Permite chequear deudas y carnet",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = isAdmin,
                        onCheckedChange = { isAdmin = it },
                        modifier = Modifier.testTag("register_admin_toggle")
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.register(name, email, phone, password, isAdmin) { success ->
                        if (success) {
                            onRegisterSuccess(isAdmin)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_register_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Icon(Icons.Filled.HowToReg, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrarme", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("¿Ya posees cuenta?", color = colors.onBackground.copy(alpha = 0.7f))
                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.testTag("navigate_to_login_button")
                ) {
                    Text("Iniciar Sesión", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
