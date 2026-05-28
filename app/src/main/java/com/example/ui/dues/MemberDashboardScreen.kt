package com.example.ui.dues

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.service.GeminiService
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
    val dues by viewModel.currentDues.collectAsStateWithLifecycle(initialValue = emptyList())

    val colors = MaterialTheme.colorScheme
    var currentTab by remember { mutableStateOf(0) } // 0: Resumen, 1: Saldos

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
                            imageVector = Icons.AutoMirrored.Filled.Logout,
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
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Resumen") },
                    label = { Text("Resumen") },
                    modifier = Modifier.testTag("tab_resumen")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Filled.Assessment, contentDescription = "Saldos") },
                    label = { Text("Saldos") },
                    modifier = Modifier.testTag("tab_saldos")
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
                0 -> MemberResumenTab(
                    viewModel = viewModel,
                    dues = dues,
                    onNavigateToSaldos = { currentTab = 1 }
                )
                1 -> MemberSaldosTab(
                    viewModel = viewModel,
                    dues = dues,
                    onPayCuota = { selectedCuotaForPayment = it }
                )
            }
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

@Composable
fun MemberResumenTab(
    viewModel: ClubViewModel,
    dues: List<MemberCuotaWithDetails>,
    onNavigateToSaldos: () -> Unit
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
        // Welcoming header banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Bienvenido al Club Estrella Roja ⚽",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.primary
                    )
                    Text(
                        text = user?.name ?: "Socio",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = colors.onBackground
                    )
                    Text(
                        text = "Nro. Afiliado: ${user?.memberNumber ?: "SOC-2026"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        color = colors.onBackground.copy(alpha = 0.7f)
                    )
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
                        text = "Estado Financiero Corto",
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
                                text = "Saldo Deudor Pendiente",
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
                                onClick = onNavigateToSaldos,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Ver Saldos")
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
                        headlineContent = { Text("Correo / Usuario (Fijo)") },
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
    val context = LocalContext.current

    val isAnalyzing by viewModel.isAnalyzingReceipt.collectAsStateWithLifecycle()
    val analysisResult by viewModel.receiptAnalysisResult.collectAsStateWithLifecycle()

    var paymentMethod by remember { mutableStateOf(0) } // 0: Credit Card, 1: Bank Transfer, 2: Office Receipt
    var cardNum by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var txHash by remember { mutableStateOf("") }

    var step by remember { mutableStateOf(0) } // 0: Form, 1: Processing, 2: Success

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                viewModel.analyzeReceiptImage(bitmap)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    LaunchedEffect(analysisResult) {
        analysisResult?.let { res ->
            if (res.success && res.transactionId.isNotEmpty() && res.isReceiptValid) {
                txHash = res.transactionId
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetReceiptAnalysis()
        }
    }

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
                                        
                                        Spacer(modifier = Modifier.height(2.dp))

                                        // --- GEMINI REAL-TIME AI RECEIPT ANALYSIS AREA ---
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = 0.08f)),
                                            border = if (isAnalyzing) BorderStroke(1.dp, colors.primary) else null,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.AutoAwesome,
                                                        contentDescription = "AI Scanner",
                                                        tint = colors.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "Comprobante AI (Gemini 3.5)",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = colors.primary
                                                    )
                                                }

                                                Text(
                                                    text = "Sube la captura de tu transferencia. Gemini autocompletará y validará los datos de tu pago al instante.",
                                                    fontSize = 10.sp,
                                                    color = colors.onSurface.copy(alpha = 0.8f)
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Button(
                                                        onClick = { imageLauncher.launch("image/*") },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 6.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                                                    ) {
                                                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Subir Foto", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            val ref = "MP-" + (100000..999999).random().toString()
                                                            val demoBitmap = GeminiService.generateSimulatedReceipt("Mercado Pago", cuota.amount, ref)
                                                            viewModel.analyzeReceiptImage(demoBitmap)
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                                    ) {
                                                        Text("Demo MP", fontSize = 9.sp)
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            val ref = "BG-" + (100000..999999).random().toString()
                                                            val mismatchAmount = cuota.amount - 1000.0
                                                            val demoBitmap = GeminiService.generateSimulatedReceipt("Banco Galicia", mismatchAmount, ref)
                                                            viewModel.analyzeReceiptImage(demoBitmap)
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                                    ) {
                                                        Text("Demo Galicia", fontSize = 9.sp)
                                                    }
                                                }

                                                if (isAnalyzing) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 2.dp),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        CircularProgressIndicator(
                                                            strokeWidth = 2.dp,
                                                            modifier = Modifier.size(14.dp),
                                                            color = colors.primary
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Gemini leyendo captura...",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = colors.primary
                                                        )
                                                     }
                                                 }

                                                 analysisResult?.let { res ->
                                                     Spacer(modifier = Modifier.height(2.dp))
                                                     HorizontalDivider(color = colors.primary.copy(alpha = 0.15f))
                                                     
                                                     if (res.success) {
                                                         val amountsMatch = Math.abs(res.amount - cuota.amount) < 0.1
                                                         val isValidPayment = res.isReceiptValid && amountsMatch

                                                         Row(
                                                             modifier = Modifier
                                                                 .fillMaxWidth()
                                                                 .background(
                                                                     if (isValidPayment) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                                     RoundedCornerShape(6.dp)
                                                                 )
                                                                 .padding(6.dp),
                                                             horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                         ) {
                                                             Icon(
                                                                 imageVector = if (isValidPayment) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                                                 contentDescription = "Validación",
                                                                 tint = if (isValidPayment) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                                 modifier = Modifier.size(18.dp)
                                                             )

                                                             Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                 Text(
                                                                     text = if (isValidPayment) "¡Comprobante Válido!" else "Incoherencia Detectada",
                                                                     style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                     color = if (isValidPayment) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                                 )
                                                                 Text(
                                                                     text = "• Billetera/Banco: ${res.company}\n" +
                                                                            "• Ref ID: ${res.transactionId}\n" +
                                                                            "• Monto en Foto: \$${String.format("%,.0f", res.amount)}\n" +
                                                                            "• Deuda: \$${String.format("%,.0f", cuota.amount)}",
                                                                     fontSize = 10.sp,
                                                                     lineHeight = 11.sp,
                                                                     color = colors.onSurface
                                                                 )
                                                                 
                                                                 if (!amountsMatch) {
                                                                     Text(
                                                                         text = "⚠️ El monto no coincide con la cuota (\$${String.format("%,.0f", cuota.amount)}).",
                                                                         fontSize = 9.sp,
                                                                         fontWeight = FontWeight.Bold,
                                                                         color = Color(0xFFC62828),
                                                                         modifier = Modifier.padding(top = 1.dp)
                                                                     )
                                                                 }
                                                                 if (res.extraNotes.isNotEmpty()) {
                                                                     Text(
                                                                         text = "Obs: ${res.extraNotes}",
                                                                         fontSize = 9.sp,
                                                                         color = colors.onSurface.copy(alpha = 0.6f)
                                                                     )
                                                                 }
                                                             }
                                                         }
                                                     } else {
                                                         Row(
                                                             modifier = Modifier
                                                                 .fillMaxWidth()
                                                                 .background(colors.errorContainer, RoundedCornerShape(6.dp))
                                                                 .padding(6.dp),
                                                             horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                         ) {
                                                             Icon(
                                                                 imageVector = Icons.Filled.ErrorOutline,
                                                                 contentDescription = "Error",
                                                                 tint = colors.error,
                                                                 modifier = Modifier.size(18.dp)
                                                             )
                                                             Text(
                                                                 text = res.extraNotes,
                                                                 fontSize = 9.sp,
                                                                 color = colors.onErrorContainer
                                                             )
                                                         }
                                                     }
                                                 }
                                             }
                                         }

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
