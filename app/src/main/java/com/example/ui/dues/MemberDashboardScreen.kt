package com.example.ui.dues

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClubNotification
import com.example.data.model.MemberCuotaWithDetails
import com.example.ui.viewmodel.ClubViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDashboardScreen(
    viewModel: ClubViewModel,
    onLogout: () -> Unit
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val notifications by viewModel.currentNotifications.collectAsStateWithLifecycle(initialValue = emptyList())
    val dues by viewModel.currentDues.collectAsStateWithLifecycle(initialValue = emptyList())

    val colors = MaterialTheme.colorScheme
    var currentTab by remember { mutableStateOf(0) } // 0: Account/Carnet, 1: Dues/Debts, 2: Notifications

    // State for Payment dialog
    var selectedCuotaForPayment by remember { mutableStateOf<MemberCuotaWithDetails?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hola, ${user?.name?.split(" ")?.firstOrNull() ?: "Socio"}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Club Social de Fútbol",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SportsSoccer,
                            contentDescription = "Soccer Ball",
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = colors.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = colors.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Filled.Badge, contentDescription = "Carnet") },
                    label = { Text("Carnet") },
                    modifier = Modifier.testTag("tab_carnet")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                val pendingCount = dues.count { it.status == "PENDING" }
                                if (pendingCount > 0) {
                                    Badge(containerColor = colors.error) {
                                        Text(pendingCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "Deudas")
                        }
                    },
                    label = { Text("Mis Deudas") },
                    modifier = Modifier.testTag("tab_deudas")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                val unreadCount = notifications.count { !it.isRead }
                                if (unreadCount > 0) {
                                    Badge(containerColor = colors.primary) {
                                        Text(unreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones")
                        }
                    },
                    label = { Text("Avisos") },
                    modifier = Modifier.testTag("tab_notificaciones")
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "TabContent"
        ) { targetTab ->
            when (targetTab) {
                0 -> MemberAccountTab(
                    viewModel = viewModel,
                    dues = dues,
                    onNavigateToPayTab = { currentTab = 1 }
                )
                1 -> MemberDuesTab(
                    viewModel = viewModel,
                    dues = dues,
                    onPayCuota = { selectedCuotaForPayment = it }
                )
                2 -> MemberNotificationsTab(
                    viewModel = viewModel,
                    notifications = notifications
                )
            }
        }

        // Expanded Payment Sheet Dialog flow
        selectedCuotaForPayment?.let { cuota ->
            PaymentSimulatorDialog(
                cuota = cuota,
                viewModel = viewModel,
                onDismiss = { selectedCuotaForPayment = null }
            )
        }
    }
}

@Composable
fun MemberAccountTab(
    viewModel: ClubViewModel,
    dues: List<MemberCuotaWithDetails>,
    onNavigateToPayTab: () -> Unit
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    val pendingDues = dues.filter { it.status == "PENDING" }
    val totalPendingAmount = pendingDues.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Member Plastic-Card visual implementation
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CREDANCIAL DIGITAL DE SOCIO",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = colors.primary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // High fidelity physical card simulator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.618f) // Golden ratio for cards
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(colors.primary, colors.secondary),
                                start = Offset(0f, 0f),
                                end = Offset(1000f, 1000f)
                            )
                        )
                        .border(1.5.dp, colors.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                ) {
                    // Decorative Canvas Stadium Lines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Stadium semi-circle lines
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = height / 2.0f,
                            center = Offset(width, height / 2.0f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = height / 1.3f,
                            center = Offset(0f, height / 2.0f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Card Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Card Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.SportsSoccer,
                                    contentDescription = null,
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "COMPLEJO FUTBOL",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        color = colors.onPrimary
                                    )
                                )
                            }
                            // Active status pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.tertiary.copy(alpha = 0.2f))
                                    .border(1.dp, colors.tertiary, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "ACTIVO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.tertiary
                                    )
                                )
                            }
                        }

                        // Name and ID Number
                        Column {
                            Text(
                                text = user?.name?.uppercase() ?: "SOCIO GENERAL",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onPrimary,
                                    letterSpacing = 0.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = user?.memberNumber ?: "SOC-2026-XXXX",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onPrimary.copy(alpha = 0.82f)
                                )
                            )
                        }

                        // Card Footer - simulated barcode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "MEMBRESÍA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onPrimary.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "SOCIO CLUB SOCIAL",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onPrimary
                                    )
                                )
                            }

                            // Simulated physical barcode visual
                            Row(
                                modifier = Modifier
                                    .height(24.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val barWidths = listOf(2, 4, 1, 3, 2, 4, 1, 2, 3, 1, 4, 2)
                                barWidths.forEach { width ->
                                    Box(
                                        modifier = Modifier
                                            .width(width.dp)
                                            .fillMaxHeight()
                                            .background(colors.onPrimary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary Statistics row
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Balance Financiero en el Club",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Deuda Acumulada",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = if (totalPendingAmount > 0.0) "$${String.format("%,.2f", totalPendingAmount)}" else "$0.00",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (totalPendingAmount > 0.0) colors.error else colors.primary
                                )
                            )
                        }

                        if (totalPendingAmount > 0.0) {
                            Button(
                                onClick = onNavigateToPayTab,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Pagar Deuda")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.primaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Al Día",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Contact info block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var isEditingName by remember { mutableStateOf(false) }
                    var newName by remember(user) { mutableStateOf(user?.name ?: "") }
                    var nameMsg by remember { mutableStateOf("") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Información Personal Registrada",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (isEditingName) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Nombre a mostrar") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (user != null) {
                                        viewModel.changeDisplayName(user!!.id, newName) { success, msg ->
                                            nameMsg = msg
                                            if (success) {
                                                isEditingName = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                            ) {
                                Text("Guardar Nombre")
                            }
                            TextButton(onClick = { isEditingName = false }, modifier = Modifier.weight(1f)) {
                                Text("Cancelar")
                            }
                        }
                        if (nameMsg.isNotEmpty()) {
                            Text(
                                text = nameMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primary,
                                modifier = Modifier.padding(top = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        ListItem(
                            headlineContent = { Text("Nombre Mostrado") },
                            supportingContent = { Text(user?.name ?: "") },
                            leadingContent = { Icon(Icons.Filled.Person, contentDescription = null, tint = colors.primary) },
                            trailingContent = { 
                                TextButton(onClick = { isEditingName = true }) {
                                    Text("Editar", fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }

                    ListItem(
                        headlineContent = { Text("Correo Universitario / Usuario (Fijo)") },
                        supportingContent = { Text(user?.email ?: "") },
                        leadingContent = { Icon(Icons.Filled.Email, contentDescription = null, tint = colors.primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Teléfono") },
                        supportingContent = { Text(user?.phone ?: "") },
                        leadingContent = { Icon(Icons.Filled.Phone, contentDescription = null, tint = colors.primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        // Info Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = colors.primary
                    )
                    Text(
                        text = "Recibirás recordatorios automáticos de pago cuando se acerque la fecha de vencimiento de tus cuotas activas asignadas por el club.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer
                    )
                }
            }
        }

        // Centralized Password Changing Section (Security Card)
        item {
            var oldPassword by remember { mutableStateOf("") }
            var newPassword by remember { mutableStateOf("") }
            var pwdMessage by remember { mutableStateOf("") }
            var isPwdError by remember { mutableStateOf(false) }
            var showFields by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Seguridad de la Cuenta",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        TextButton(onClick = { showFields = !showFields }) {
                            Text(if (showFields) "Cancelar" else "Cambiar Contraseña", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (showFields) {
                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            label = { Text("Contraseña Actual Entregada") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Nueva Contraseña Personal") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (pwdMessage.isNotEmpty()) {
                            Text(
                                text = pwdMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPwdError) colors.error else colors.primary,
                                modifier = Modifier.padding(top = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (user != null) {
                                    viewModel.changePassword(user!!.id, oldPassword, newPassword) { success, msg ->
                                        isPwdError = !success
                                        pwdMessage = msg
                                        if (success) {
                                            oldPassword = ""
                                            newPassword = ""
                                            showFields = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar Nueva Contraseña")
                        }
                    } else {
                        Text(
                            text = "Por su seguridad, le recomendamos cambiar la contraseña inicial provista por el administrador del club.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberDuesTab(
    viewModel: ClubViewModel,
    dues: List<MemberCuotaWithDetails>,
    onPayCuota: (MemberCuotaWithDetails) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var filterPendingOnly by remember { mutableStateOf(false) }

    val displayedDues = if (filterPendingOnly) {
        dues.filter { it.status == "PENDING" }
    } else {
        dues
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Tab header with filter switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Control de Cuota Obligatoria",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${displayedDues.size} cuotas listadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onBackground.copy(alpha = 0.6f)
                )
            }

            // Simple Filter Pill
            FilterChip(
                selected = filterPendingOnly,
                onClick = { filterPendingOnly = !filterPendingOnly },
                label = { Text("Pendientes", fontSize = 12.sp) },
                leadingIcon = {
                    if (filterPendingOnly) {
                        Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                },
                modifier = Modifier.testTag("filter_pending_chip")
            )
        }

        if (displayedDues.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Savings,
                        contentDescription = "No dues icon",
                        tint = colors.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "¡Felicitaciones! No hay cuotas pendientes que coincidan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("dues_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(displayedDues, key = { it.memberCuotaId }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.status == "PENDING") colors.surface else colors.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (item.status == "PENDING") 2.dp else 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("due_item_${item.memberCuotaId}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Category Badge + Status Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    val catLabel = when (item.category) {
                                        "MEMBERSHIP" -> "Social"
                                        "EQUIPMENT" -> "Camisetas"
                                        "TOURNAMENT" -> "Torneo"
                                        else -> "Club"
                                    }
                                    Text(
                                        text = catLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                // Status Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (item.status == "PENDING") colors.errorContainer else colors.primaryContainer.copy(alpha = 0.4f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (item.status == "PENDING") "PENDIENTE" else "AL DÍA",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (item.status == "PENDING") colors.error else colors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Title & Description
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.onBackground
                            )

                            if (item.description.isNotBlank()) {
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onBackground.copy(alpha = 0.65f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = colors.onBackground.copy(alpha = 0.08f)
                            )

                            // Amount & DueDate / Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Importe",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onBackground.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "$${String.format("%,.2f", item.amount)}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (item.status == "PENDING") colors.error else colors.primary
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    if (item.status == "PENDING") {
                                        Text(
                                            text = "Vence: ${viewModel.getFormattedDate(item.dueDate)}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = colors.onBackground.copy(alpha = 0.7f)
                                            ),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )

                                        Button(
                                            onClick = { onPayCuota(item) },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("pay_button_${item.memberCuotaId}")
                                        ) {
                                            Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pagar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text(
                                            text = "Pagado: ${item.paidDate?.let { viewModel.getFormattedDate(it) } ?: ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.primary
                                        )
                                        Text(
                                            text = "Ref: ${item.paymentReference ?: ""}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            ),
                                            color = colors.onBackground.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberNotificationsTab(
    viewModel: ClubViewModel,
    notifications: List<ClubNotification>
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = "Buzón de Comunicaciones",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Consulta avisos de pagos, cambios de horarios o avisos generales.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onBackground.copy(alpha = 0.6f)
            )
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsNone,
                        contentDescription = "Empty notifications",
                        tint = colors.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No has recibido notificaciones recientes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("notifications_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(notifications, key = { it.id }) { alert ->
                    val isGeneral = alert.memberId == null
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (alert.isRead) colors.surfaceVariant.copy(alpha = 0.3f) else colors.surface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = if (!alert.isRead) androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f)) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = if (alert.isRead) 0.dp else 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!alert.isRead) {
                                    viewModel.markNotificationAsRead(alert.id)
                                }
                            }
                            .testTag("notification_item_${alert.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Circular Category Icon
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isGeneral) colors.primaryContainer else colors.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isGeneral) Icons.Filled.Campaign else Icons.Filled.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (isGeneral) colors.primary else colors.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = alert.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (alert.isRead) colors.onBackground.copy(alpha = 0.8f) else colors.onBackground
                                    )

                                    // Tiny glowing indicator dot if unread
                                    if (!alert.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(colors.primary)
                                        )
                                    }
                                }

                                Text(
                                    text = alert.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isGeneral) "📢 Aviso General" else "👤 Privado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onBackground.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = viewModel.getFormattedDate(alert.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onBackground.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog simulating a secure connection to a banking core / card provider
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSimulatorDialog(
    cuota: MemberCuotaWithDetails,
    viewModel: ClubViewModel,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var paymentMethod by remember { mutableStateOf(0) } // 0: Credit Card, 1: Bank Transfer, 2: Office Receipt
    var cardNum by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var txHash by remember { mutableStateOf("") }

    var step by remember { mutableStateOf(0) } // 0: Form, 1: Processing, 2: Success

    Dialog(
        onDismissRequest = { if (step != 1) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            tonalElevation = 6.dp
        ) {
            AnimatedContent(
                targetState = step,
                label = "PaymentSteps"
            ) { targetStep ->
                when (targetStep) {
                    0 -> {
                        // PAYMENT SELECTION AND SUBMIT FORM
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pasarela de Pago Club",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close")
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cuota.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Obligación asignada",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.primary
                                        )
                                    }
                                    Text(
                                        text = "$${String.format("%,.2f", cuota.amount)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = colors.primary
                                    )
                                }
                            }

                            Text(
                                text = "Elige un método de pago:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            // Tabs / selectors for payment methods
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { paymentMethod = 0 },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (paymentMethod == 0) colors.primary else colors.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("Tarjeta", fontSize = 10.sp)
                                    }
                                }

                                Button(
                                    onClick = { paymentMethod = 1 },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (paymentMethod == 1) colors.primary else colors.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.CurrencyExchange, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("Transferencia", fontSize = 10.sp)
                                    }
                                }

                                Button(
                                    onClick = { paymentMethod = 2 },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (paymentMethod == 2) colors.primary else colors.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("Presencial", fontSize = 10.sp)
                                    }
                                }
                            }

                            // Sub forms for each payment method
                            when (paymentMethod) {
                                0 -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = cardNum,
                                            onValueChange = { cardNum = it.take(16) },
                                            label = { Text("Número de Tarjeta") },
                                            placeholder = { Text("Enter 16 digits") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            OutlinedTextField(
                                                value = cardExpiry,
                                                onValueChange = { cardExpiry = it.take(5) },
                                                label = { Text("MM/AA") },
                                                placeholder = { Text("12/29") },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            OutlinedTextField(
                                                value = cardCvv,
                                                onValueChange = { cardCvv = it.take(3) },
                                                label = { Text("CVV") },
                                                placeholder = { Text("123") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                        }
                                    }
                                }
                                1 -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "💳 Datos de Transferencia Bancaria",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = colors.primary)
                                        )
                                        Text(
                                            text = "Realiza la transferencia desde la app de tu banco usando estos datos:",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = "Alias: futbol.socio.club\nCBU: 0170420010000030405060\nBanco: Credicoop Social",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = colors.onBackground.copy(alpha = 0.8f)
                                        )
                                        OutlinedTextField(
                                            value = txHash,
                                            onValueChange = { txHash = it },
                                            label = { Text("Código de Operación o ID") },
                                            placeholder = { Text("Ej: TX-90321") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                                2 -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "🏢 Pago en Tesorería / Secretaría",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = colors.primary)
                                        )
                                        Text(
                                            text = "Para abonar en efectivo o con cheque, puedes presentarte en la administración del club con el siguiente concepto:",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = "Código de cobro: COMP-${cuota.memberId}-${cuota.cuotaId}",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Action button
                            Button(
                                onClick = {
                                    // Trigger loader animation step
                                    step = 1
                                    scope.launch {
                                        delay(2000) // simulated processing delay
                                        val methodStr = when (paymentMethod) {
                                            0 -> "Tarjeta terminada en *${cardNum.takeLast(4).ifBlank { "4321" }}"
                                            1 -> "Transferencia (Ref: $txHash)"
                                            else -> "Orden Presencial de Secretaría"
                                        }
                                        viewModel.payCuota(cuota.memberCuotaId, methodStr)
                                        step = 2
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("confirm_payment_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                            ) {
                                Text(
                                    text = if (paymentMethod == 2) "Generar Cupón de Cobro" else "Confirmar Pago Seguro",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    1 -> {
                        // BANK LOADER RUNNING
                        Column(
                            modifier = Modifier
                                .padding(40.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 5.dp,
                                color = colors.primary,
                                modifier = Modifier.size(60.dp)
                            )
                            Text(
                                text = "Comunicando con la red bancaria...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Procesando autorización de cobro seguro de \$${String.format("%,.2f", cuota.amount)}. Por favor no cierres la app.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = colors.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                    2 -> {
                        // PAYMENT SUCCESS FINISHED
                        Column(
                            modifier = Modifier
                                .padding(30.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Success",
                                    tint = colors.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Text(
                                text = "¡Pago Registrado con Éxito!",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                color = colors.primary
                            )

                            Text(
                                text = "Se ha enviado la confirmación de pago del club. Has saldado tu deuda de '${cuota.title}' por valor de \$${String.format("%,.2f", cuota.amount)}.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = colors.onBackground.copy(alpha = 0.7f)
                            )

                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("payment_success_done")
                            ) {
                                Text("Entendido")
                            }
                        }
                    }
                }
            }
        }
    }
}
