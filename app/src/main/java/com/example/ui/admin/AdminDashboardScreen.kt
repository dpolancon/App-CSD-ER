package com.example.ui.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Cuota
import com.example.data.model.CuotaAssignmentWithMemberDetails
import com.example.data.model.Member
import com.example.ui.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: ClubViewModel,
    onLogout: () -> Unit
) {
    val members by viewModel.allMembers.collectAsStateWithLifecycle()
    val cuotasByClub by viewModel.allCuotas.collectAsStateWithLifecycle()
    val allAssignments by viewModel.allDuesAssignments.collectAsStateWithLifecycle()

    val colors = MaterialTheme.colorScheme
    var currentTab by remember { mutableStateOf(0) } // 0: Dashboard/Members, 1: Create Cuota, 2: Control & Pay Registers

    // Dialog sheets states
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var selectedAssignmentForUpdate by remember { mutableStateOf<CuotaAssignmentWithMemberDetails?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Panel Administración Club",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Secretaría de Finanzas",
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
                            .background(colors.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = "Admin icon",
                            tint = colors.tertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("admin_logout_button")
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
                    icon = { Icon(Icons.Filled.People, contentDescription = "Socios") },
                    label = { Text("Socios") },
                    modifier = Modifier.testTag("admin_tab_socios")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Filled.PostAdd, contentDescription = "Nueva Cuota") },
                    label = { Text("Asignar Cuota") },
                    modifier = Modifier.testTag("admin_tab_nueva_cuota")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Filled.Checklist, contentDescription = "Deudas") },
                    label = { Text("Cobros") },
                    modifier = Modifier.testTag("admin_tab_control_pagos")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Filled.Laptop, contentDescription = "App PC Sync") },
                    label = { Text("App PC Sync") },
                    modifier = Modifier.testTag("admin_tab_desktop_app")
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
            label = "AdminTabContent"
        ) { targetTab ->
            when (targetTab) {
                0 -> AdminMembersTab(
                    members = members,
                    allAssignments = allAssignments,
                    onAddNewMember = { showAddMemberDialog = true }
                )
                1 -> AdminCreateCuotaTab(
                    viewModel = viewModel,
                    existingCuotas = cuotasByClub
                )
                2 -> AdminPaymentControlTab(
                    assignments = allAssignments,
                    viewModel = viewModel,
                    onSelectAssignment = { selectedAssignmentForUpdate = it }
                )
                3 -> AdminDesktopAppSimulatorTab(
                    viewModel = viewModel,
                    members = members
                )
            }
        }

        // Dialog - Register new member
        if (showAddMemberDialog) {
            AddMemberDialog(
                viewModel = viewModel,
                onDismiss = { showAddMemberDialog = false }
            )
        }

        // Dialog - Mark manual payment of dues
        selectedAssignmentForUpdate?.let { assignment ->
            AdminMarkPaidDialog(
                assignment = assignment,
                viewModel = viewModel,
                onDismiss = { selectedAssignmentForUpdate = null }
            )
        }
    }
}

@Composable
fun AdminMembersTab(
    members: List<Member>,
    allAssignments: List<CuotaAssignmentWithMemberDetails>,
    onAddNewMember: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }

    val filteredMembers = members.filter {
        it.role != "ADMIN" && (
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.memberNumber.contains(searchQuery, ignoreCase = true)
        )
    }

    // Mathematical aggregation calculation
    val totalCollected = allAssignments.filter { it.status == "PAID" }.sumOf { it.amount }
    val totalPending = allAssignments.filter { it.status == "PENDING" }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Core accounting indicators
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📊 Balance de Caja General",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = colors.primary)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Collected
                        Card(
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Total Cobrado", style = MaterialTheme.typography.labelSmall, color = colors.onSurface.copy(alpha = 0.6f))
                                Text(
                                    text = "$${String.format("%,.0f", totalCollected)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = colors.primary)
                                )
                            }
                        }

                        // Total Pending
                        Card(
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Total Pendiente", style = MaterialTheme.typography.labelSmall, color = colors.onSurface.copy(alpha = 0.6f))
                                Text(
                                    text = "$${String.format("%,.0f", totalPending)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = colors.error)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section header and Action buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Socios Registrados", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("${filteredMembers.size} socios activos", style = MaterialTheme.typography.labelSmall, color = colors.onBackground.copy(alpha = 0.5f))
                }

                Button(
                    onClick = onAddNewMember,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("admin_add_member_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo", fontSize = 12.sp)
                }
            }
        }

        // Search Input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar socio por nombre o carnet...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_search_members_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (filteredMembers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ningún socio club coincide con el criterio.",
                        color = colors.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredMembers, key = { it.id }) { user ->
                val socioAssignments = allAssignments.filter { it.memberId == user.id }
                val userPendingAmount = socioAssignments.filter { it.status == "PENDING" }.sumOf { it.amount }

                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary
                                    )
                                )
                            }

                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = user.memberNumber,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = colors.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Socio personal debt state
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text(
                                text = "Deuda",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onBackground.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "$${String.format("%,.0f", userPendingAmount)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (userPendingAmount > 0) colors.error else colors.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AdminCreateCuotaTab(
    viewModel: ClubViewModel,
    existingCuotas: List<Cuota>
) {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("MEMBERSHIP") }
    var monthsOffset by remember { mutableStateOf(0) } // 0: Mayo/Este mes, 1: Próximo mes, 2: Dos meses

    val categories = listOf(
        "MEMBERSHIP" to "Membresía Social",
        "EQUIPMENT" to "Equipamiento",
        "TOURNAMENT" to "Torneo Fútbol",
        "OTHER" to "Otros Gastos"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text("Emisión Masiva de Cuota Obligatoria", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Define una nueva tarifa y se asignará automáticamente como deuda a todos los miembros con recordatorio automático.", style = MaterialTheme.typography.bodySmall, color = colors.onBackground.copy(alpha = 0.6f))
        }

        // Form Card
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
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la Cuota") },
                    placeholder = { Text("Ej: Cuota Social Junio 2026") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_cuota_title"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción / Detalle") },
                    placeholder = { Text("Mantenimiento general de vestuarios y canchas sintéticas...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Importe ($)") },
                        placeholder = { Text("5000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_cuota_amount"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Month offset selector
                    Box(modifier = Modifier.weight(1.2f)) {
                        Column {
                            Text("Vencimiento", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Inmediato", "30 Días", "60 Días").forEachIndexed { idx, label ->
                                    Button(
                                        onClick = { monthsOffset = idx },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (monthsOffset == idx) colors.primary else colors.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Category Selector Row
                Text("Categoría de Tarifa:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (key, label) ->
                        Button(
                            onClick = { category = key },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (category == key) colors.primary else colors.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(label.split(" ").last(), fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amt > 0.0) {
                            viewModel.createCuotaAndAssign(
                                title = title,
                                description = description,
                                amount = amt,
                                monthsFromNow = monthsOffset,
                                category = category
                            )
                            // Clear form
                            title = ""
                            description = ""
                            amountStr = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_submit_cuota_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Icon(Icons.Filled.Publish, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emitir & Cobrar a Todos", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of issued cuotas
        Text("Historial de Emisiones Masivas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

        if (existingCuotas.isEmpty()) {
            Text(
                text = "No has emitido cuotas extraordinarias todavía.",
                color = colors.onBackground.copy(alpha = 0.4f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                existingCuotas.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Valor: $${String.format("%,.2f", item.amount)} · Vence: ${viewModel.getFormattedDate(item.dueDate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onBackground.copy(alpha = 0.6f)
                                )
                            }

                            // Tiny indicator label matching category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.category.take(4),
                                    style = MaterialTheme.typography.labelSmall.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AdminPaymentControlTab(
    assignments: List<CuotaAssignmentWithMemberDetails>,
    viewModel: ClubViewModel,
    onSelectAssignment: (CuotaAssignmentWithMemberDetails) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var filterPendingOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredAssignments = assignments.filter {
        val matchesSearch = it.memberName.contains(searchQuery, ignoreCase = true) ||
                it.title.contains(searchQuery, ignoreCase = true)
        val matchesStatus = !filterPendingOnly || it.status == "PENDING"
        matchesSearch && matchesStatus
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text("Registro de Devolución & Cobros", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Monitoreo en tiempo real de deudas. Haz clic en un pago pendiente para regularizar de forma manual (efectivo en oficina o depósito).", style = MaterialTheme.typography.bodySmall, color = colors.onBackground.copy(alpha = 0.6f))
        }

        // Filters card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Socio o cuota...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = filterPendingOnly,
                onClick = { filterPendingOnly = !filterPendingOnly },
                label = { Text("Pendientes", fontSize = 11.sp) },
                modifier = Modifier.height(34.dp)
            )
        }

        if (filteredAssignments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ningún balance cargado coincide.",
                    color = colors.onBackground.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("admin_assignments_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredAssignments, key = { it.memberCuotaId }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.status == "PENDING") colors.surface else colors.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (item.status == "PENDING") null else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = if (item.status == "PENDING") 1.dp else 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = item.status == "PENDING") {
                                onSelectAssignment(item)
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Member Name
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.memberName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Status
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (item.status == "PENDING") colors.errorContainer else colors.primaryContainer.copy(alpha = 0.5f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (item.status == "PENDING") "PENDIENTE" else "PAGADO",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = if (item.status == "PENDING") colors.error else colors.primary
                                        )
                                    )
                                }
                            }

                            // Cuota Title & Value
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Vencimiento: ${viewModel.getFormattedDate(item.dueDate)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onBackground.copy(alpha = 0.5f)
                                    )
                                }

                                Text(
                                    text = "$${String.format("%,.0f", item.amount)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (item.status == "PENDING") colors.error else colors.primary
                                )
                            }

                            // Payment reference if paid
                            if (item.status == "PAID") {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = colors.onBackground.copy(alpha = 0.05f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Ref: ${item.paymentReference}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = colors.onBackground.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "Abonado: ${item.paidDate?.let { viewModel.getFormattedDate(it) } ?: ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.primaryContainer.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.EditNote, contentDescription = null, tint = colors.primary, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Presiona esta fila para registrar pago manual",
                                            style = MaterialTheme.typography.labelSmall.copy(color = colors.primary, fontSize = 9.sp)
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

// Dialog that lets the admin register a manual transaction (Cash, Office deposit)
@Composable
fun AdminMarkPaidDialog(
    assignment: CuotaAssignmentWithMemberDetails,
    viewModel: ClubViewModel,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var reference by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Registrar Cobro Manual ✍️",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                // Summary info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Socio: ${assignment.memberName}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Text(text = "Carnet: ${assignment.memberNumber}", style = MaterialTheme.typography.labelSmall)
                    Text(text = "Concepto: ${assignment.title}", style = MaterialTheme.typography.bodySmall)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Monto a liquidar:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "$${String.format("%,.2f", assignment.amount)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = colors.primary)
                        )
                    }
                }

                Text(
                    text = "Usa esta herramienta cuando el socio abone en efectivo en la secretaría del club o presente un comprobante de transferencia bancario externo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onBackground.copy(alpha = 0.6f)
                )

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Referencia del Cobro") },
                    placeholder = { Text("Efectivo - Recibo Nro #102") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_mark_paid_reference"),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = colors.error)
                    }

                    Button(
                        onClick = {
                            viewModel.adminMarkAsPaid(
                                memberCuotaId = assignment.memberCuotaId,
                                reference = reference
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("admin_confirm_manual_payment"),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Registrar Pago")
                    }
                }
            }
        }
    }
}


// Dialog adding a custom partner through administration desk
@Composable
fun AddMemberDialog(
    viewModel: ClubViewModel,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val errorMsg by viewModel.registerError.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("123") } // auto default simple

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Registrar Nuevo Socio 👤",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                )

                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = colors.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre Completo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_add_member_name"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_add_member_email"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_add_member_phone"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña Provisoria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar", color = colors.error)
                    }

                    Button(
                        onClick = {
                            viewModel.register(
                                name = name,
                                email = email,
                                phone = phone,
                                passwordHash = password,
                                isAdmin = false
                            ) { success ->
                                if (success) {
                                    // Reset user stream focus back to admin session and close dialog safely
                                    viewModel.login("admin@club.com", "123") {}
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("admin_submit_add_member"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Crear Socio")
                    }
                }
            }
        }
    }
}

// ==========================================
// DESKTOP WORKSTATION APP PORTAL SIMULATOR
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDesktopAppSimulatorTab(
    viewModel: ClubViewModel,
    members: List<Member>
) {
    val colors = MaterialTheme.colorScheme
    var desktopSubTab by remember { mutableStateOf(0) } // 0: User Creation & Passwords, 1: Planilla Sync & Sheets
    val spreadsheetRows by viewModel.spreadsheetRows.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Frame Simulation (OS Window chrome representing desktop workstation)
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // OS App Bar (Mock title bar representing Desktop App)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.primary)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Window control buttons
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFE57373)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFB74D)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF81C784)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SecretaríaClub Pro v3.7 - Desktop PC Mode",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.onPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.onPrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OFFLINE SOURCE",
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.onPrimary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        )
                    }
                }

                // Desktop App Internal Tab Switcher (Navigation Workspace)
                TabRow(
                    selectedTabIndex = desktopSubTab,
                    containerColor = colors.surfaceVariant.copy(alpha = 0.3f),
                    contentColor = colors.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[desktopSubTab]),
                            color = colors.primary
                        )
                    }
                ) {
                    Tab(
                        selected = desktopSubTab == 0,
                        onClick = { desktopSubTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Generador Socios", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                            }
                        }
                    )
                    Tab(
                        selected = desktopSubTab == 1,
                        onClick = { desktopSubTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.BackupTable, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Planilla Sync (Sheets)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                            }
                        }
                    )
                }

                // Desktop Tab Content
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (desktopSubTab == 0) {
                        // USER GENERATION & CLAVE INICIAL
                        Text(
                            text = "Generación Centralizada de Cuentas de Socio",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.primary
                        )
                        Text(
                            text = "Por norma del club, los socios NOT se registran solos. Únicamente de forma centralizada a través de esta app de escritorio. Ingrese los datos de afiliación y genere una contraseña inicial provisoria.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurface.copy(alpha = 0.6f)
                        )

                        var mName by remember { mutableStateOf("") }
                        var mEmail by remember { mutableStateOf("") }
                        var mPhone by remember { mutableStateOf("") }
                        var mPass by remember { mutableStateOf("") }
                        var genMessage by remember { mutableStateOf("") }
                        var isGenSuccess by remember { mutableStateOf(false) }

                        // Form Fields
                        OutlinedTextField(
                            value = mName,
                            onValueChange = { mName = it },
                            label = { Text("Nombre y Apellido") },
                            placeholder = { Text("Ej: Ronaldinho Gaucho") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = mEmail,
                            onValueChange = { mEmail = it },
                            label = { Text("Correo Electrónico Oficial") },
                            placeholder = { Text("socio@ejemplo.com") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = mPhone,
                                onValueChange = { mPhone = it },
                                label = { Text("Teléfono de Contacto") },
                                placeholder = { Text("+549...") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Column(modifier = Modifier.weight(1.2f)) {
                                OutlinedTextField(
                                    value = mPass,
                                    onValueChange = { mPass = it },
                                    label = { Text("Clave Provisoria") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val generated = (1000..9999).random().toString()
                                            mPass = "SOCIO-$generated"
                                        }) {
                                            Icon(Icons.Filled.Autorenew, contentDescription = "Generar contraseña")
                                        }
                                    }
                                )
                            }
                        }

                        if (genMessage.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isGenSuccess) colors.primaryContainer.copy(alpha = 0.4f) else colors.errorContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isGenSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                        contentDescription = null,
                                        tint = if (isGenSuccess) colors.primary else colors.error
                                    )
                                    Text(
                                        text = genMessage,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isGenSuccess) colors.onPrimaryContainer else colors.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val cleanEmail = mEmail.trim()
                                val cleanName = mName.trim()
                                val cleanPhone = mPhone.trim()
                                val cleanPass = mPass.trim()

                                if (cleanName.isBlank() || cleanEmail.isBlank() || cleanPhone.isBlank() || cleanPass.isBlank()) {
                                    isGenSuccess = false
                                    genMessage = "Error: Complete todos los campos antes de generar la afiliación."
                                    return@Button
                                }

                                viewModel.register(
                                    name = cleanName,
                                    email = cleanEmail,
                                    phone = cleanPhone,
                                    passwordHash = cleanPass,
                                    isAdmin = false
                                ) { success ->
                                    if (success) {
                                        isGenSuccess = true
                                        genMessage = "¡SOCIO CREADO CON ÉXITO!\nInicial entregada: '$cleanPass'. Avise al socio para que ingrese y la cambie."
                                        mName = ""
                                        mEmail = ""
                                        mPhone = ""
                                        mPass = ""
                                        // Reset to Admin session automatically
                                        viewModel.login("admin@club.com", "123") {}
                                    } else {
                                        isGenSuccess = false
                                        genMessage = viewModel.registerError.value ?: "Error al registrar en base de datos local."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Icon(Icons.Filled.AssignmentInd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generar Socio y Clave Inicial", fontWeight = FontWeight.Bold)
                        }

                    } else {
                        // PLANILLA SYNC & SHEETS CONSOLE
                        Text(
                            text = "Planilla del Club como Fuente de Verdad",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.primary
                        )
                        Text(
                            text = "Aquí dispone de la planilla electrónica contable del club (Sincronización Simulada). Al presionar Sincronizar, el sistema lee cada fila, valida el correo del socio, define la cuota en el catálogo e impacta la deuda pendiente asignada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurface.copy(alpha = 0.6f)
                        )

                        // Spreadsheet Grid Simulator
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceVariant.copy(alpha = 0.2f))
                                .border(1.dp, colors.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        ) {
                            // Table Headers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.primaryContainer.copy(alpha = 0.4f))
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                Text("Socio Correo", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.3f))
                                Text("Concepto de Cuota", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.5f))
                                Text("Importe", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(0.7f))
                                Text("Estado", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(0.8f))
                            }

                            HorizontalDivider(color = colors.outline.copy(alpha = 0.15f))

                            spreadsheetRows.forEachIndexed { idx, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (idx % 2 == 0) colors.surface else colors.surfaceVariant.copy(alpha = 0.05f))
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(row.email, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.3f), overflow = TextOverflow.Ellipsis, maxLines = 1)
                                    Text(row.conceptTitle, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.5f), overflow = TextOverflow.Ellipsis, maxLines = 1)
                                    Text("$${String.format("%,.0f", row.amount)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(0.7f))
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(0.8f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (row.isPending) colors.secondaryContainer else colors.primaryContainer.copy(alpha = 0.4f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (row.isPending) "Pendiente" else "Sincrónico",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (row.isPending) colors.onSecondaryContainer else colors.primary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                                HorizontalDivider(color = colors.outline.copy(alpha = 0.08f))
                            }
                        }

                        // Add custom row simulator to spreadsheet
                        var newSheetEmail by remember { mutableStateOf("") }
                        var newSheetConcept by remember { mutableStateOf("") }
                        var newSheetAmount by remember { mutableStateOf("") }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Text("Añadir Fila Ficticia a Planilla", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.primary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newSheetEmail,
                                    onValueChange = { newSheetEmail = it },
                                    label = { Text("Correo", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = newSheetConcept,
                                    onValueChange = { newSheetConcept = it },
                                    label = { Text("Concepto", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = newSheetAmount,
                                    onValueChange = { newSheetAmount = it },
                                    label = { Text("Monto", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            Button(
                                onClick = {
                                    val amt = newSheetAmount.toDoubleOrNull() ?: 0.0
                                    if (newSheetEmail.isNotBlank() && newSheetConcept.isNotBlank() && amt > 0.0) {
                                        viewModel.addSpreadsheetRow(newSheetEmail, newSheetConcept, amt)
                                        newSheetEmail = ""
                                        newSheetConcept = ""
                                        newSheetAmount = ""
                                    }
                                },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.secondary)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Añadir Fila", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // SPREADSHEET TRIGGER SINC
                        Button(
                            onClick = { viewModel.syncWithClubSpreadsheet() },
                            enabled = !isSyncing && spreadsheetRows.any { it.isPending },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = colors.onPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sincronizando de Planilla...")
                            } else {
                                Icon(Icons.Filled.Sync, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sincronizar Planilla con Base de Datos", fontWeight = FontWeight.Bold)
                            }
                        }

                        // terminal sync monitor logs
                        Text(
                            text = "Terminal Sync Monitor Logs:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.onSurface.copy(alpha = 0.8f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E1E))
                                .padding(12.dp)
                        ) {
                            LazyColumn(reverseLayout = true) {
                                items(syncLogs.reversed()) { logLine ->
                                    Text(
                                        text = logLine,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = if (logLine.contains("[ERROR]")) Color(0xFFEF9A9A) 
                                                    else if (logLine.contains("[ÉXITO]")) Color(0xFFA5D6A7) 
                                                    else if (logLine.contains("[CREADO]")) Color(0xFFFFCC80) 
                                                    else Color(0xFFE0E0E0)
                                        ),
                                        modifier = Modifier.padding(bottom = 4.dp)
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
