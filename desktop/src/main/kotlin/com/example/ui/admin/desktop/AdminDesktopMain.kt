package com.example.ui.admin.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

// CSD Estrella Roja administrative palette
val ClubRed = Color(0xFFD32F2F)
val DarkCharcoal = Color(0xFF212121)
val SoftGray = Color(0xFFF8F9FA)
val VerySoftRed = Color(0xFFFFEBEE)
val SoftGreen = Color(0xFFE8F5E9)
val ActionGreen = Color(0xFF2E7D32)

@Composable
fun AdminDesktopApp(viewModel: AdminDesktopViewModel) {
    val membersList by viewModel.filteredMembers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val name by viewModel.formName.collectAsState()
    val email by viewModel.formEmail.collectAsState()
    val phone by viewModel.formPhone.collectAsState()
    val amount by viewModel.formCuotaAmount.collectAsState()
    val concept by viewModel.formCuotaConcept.collectAsState()
    
    val validationError by viewModel.validationError.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = ClubRed,
            onPrimary = Color.White,
            surface = Color.White,
            background = SoftGray,
            onBackground = DarkCharcoal
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SoftGray
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Chrome
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ClubRed)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SportsSoccer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "CLUB SOCIAL Y DEPORTIVO ESTRELLA ROJA",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Consola de Administración Central - Secretaría de Finanzas",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF81C784)))
                            Text(
                                text = "PERSISTENCIA LOCAL ACTIVA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Split Layout Workspace
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // LEFT COLUMN: Padrón de Socios (Ancho: 380.dp)
                    Column(
                        modifier = Modifier
                            .width(380.dp)
                            .fillMaxHeight()
                            .background(Color.White)
                            .border(width = 1.dp, color = Color.LightGray.copy(alpha = 0.4f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SOCIOS REGISTRADOS",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkCharcoal
                                )
                                Text(
                                    text = "${membersList.size} socios cargados",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.loadMembers() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refrescar padrón",
                                    tint = ClubRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Search Input
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Buscar socio por nombre, carnet o email...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        if (membersList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Group,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Ningún socio coincide con los filtros.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(membersList, key = { it.id }) { member ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SoftGray.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
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
                                                // Initials avatar
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(ClubRed.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = member.name.take(2).uppercase(),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ClubRed
                                                    )
                                                }
                                                
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = member.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = DarkCharcoal,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = member.memberNumber,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }

                                            // Balance flag
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (member.totalPendingDebt > 0) VerySoftRed else SoftGreen)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (member.totalPendingDebt > 0) {
                                                        "Deuda: $${String.format("%,.0f", member.totalPendingDebt)}"
                                                    } else {
                                                        "Al Día"
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (member.totalPendingDebt > 0) ClubRed else ActionGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN: Formulario Alta / Detalle Socio (Flexible)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "REGISTRAR NUEVO SOCIO 👤",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkCharcoal
                        )

                        // Visual notifications
                        validationError?.let { err ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = VerySoftRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Error, contentDescription = null, tint = ClubRed)
                                    Text(
                                        text = err,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ClubRed
                                    )
                                }
                            }
                        }

                        successMessage?.let { msg ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SoftGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ActionGreen)
                                    Text(
                                        text = msg,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ActionGreen
                                    )
                                }
                            }
                        }

                        // Form card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Datos de Acceso y Afiliación",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ClubRed
                                )

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { viewModel.formName.value = it },
                                    label = { Text("Nombre Completo") },
                                    placeholder = { Text("Ej: Diego Armando Silva") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { viewModel.formEmail.value = it },
                                        label = { Text("Email de Acceso") },
                                        placeholder = { Text("socio@ejemplo.com") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { viewModel.formPhone.value = it },
                                        label = { Text("Teléfono / Celular") },
                                        placeholder = { Text("+54 9 11...") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.8f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Embedded initial fee sub-card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SoftGray),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.MonetizationOn,
                                                contentDescription = null,
                                                tint = ClubRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "ASIGNACIÓN DE CUOTA INICIAL OBLIGATORIA",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkCharcoal
                                            )
                                        }
                                        
                                        Text(
                                            text = "El socio recién creado quedará registrado automáticamente con una deuda pendiente por el valor y concepto definidos aquí, correspondiente a su alta social.",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = concept,
                                                onValueChange = { viewModel.formCuotaConcept.value = it },
                                                label = { Text("Concepto de Cuota") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1.3f),
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            OutlinedTextField(
                                                value = amount,
                                                onValueChange = { viewModel.formCuotaAmount.value = it },
                                                label = { Text("Monto ($)") },
                                                singleLine = true,
                                                modifier = Modifier.weight(0.7f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            
                            TextButton(
                                onClick = { viewModel.clearForm() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancelar", fontSize = 13.sp)
                            }
                            
                            Button(
                                onClick = { viewModel.saveNewMember() },
                                colors = ButtonDefaults.buttonColors(containerColor = ClubRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GUARDAR SOCIO", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun main() = application {
    val viewModel = remember { AdminDesktopViewModel() }
    Window(
        onCloseRequest = ::exitApplication,
        title = "CSD Estrella Roja - Administración Central",
        state = rememberWindowState(
            size = DpSize(1024.dp, 768.dp)
        )
    ) {
        AdminDesktopApp(viewModel)
    }
}
