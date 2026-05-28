package com.example.ui.dues

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberCuotaWithDetails
import com.example.ui.viewmodel.ClubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberSaldosTab(
    viewModel: ClubViewModel,
    dues: List<MemberCuotaWithDetails>,
    onPayCuota: (MemberCuotaWithDetails) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    // Filter state: 0 = Todas, 1 = Pagadas, 2 = Impagas
    var selectedFilter by remember { mutableStateOf(0) }

    // Calculated metrics
    val paidDues = dues.filter { it.status == "PAID" }
    val pendingDues = dues.filter { it.status == "PENDING" }

    val totalPaidAmount = paidDues.sumOf { it.amount }
    val totalPendingAmount = pendingDues.sumOf { it.amount }

    val totalCount = dues.size
    val paidCount = paidDues.size
    val paymentProgress = if (totalCount > 0) paidCount.toFloat() / totalCount else 1.0f

    // Filter displayed list
    val displayedDues = when (selectedFilter) {
        1 -> paidDues
        2 -> pendingDues
        else -> dues
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("saldos_tab_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Core Resumen Financiero Personal Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saldos_summary_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Estado de Cuenta 📊",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Progress Indicator inside a box with overlay text
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { paymentProgress },
                                strokeWidth = 8.dp,
                                color = if (paymentProgress >= 1.0f) colors.primary else colors.secondary,
                                trackColor = colors.surfaceVariant,
                                modifier = Modifier.fillMaxSize()
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${(paymentProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (paymentProgress >= 1.0f) colors.primary else colors.onSurface
                                )
                                Text(
                                    text = "Al Día",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = colors.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Detailed balance labels
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colors.primary))
                                    Text(
                                        text = "Abonado con Éxito",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Text(
                                    text = "$${String.format("%,.2f", totalPaidAmount)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = colors.primary
                                    )
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colors.error))
                                    Text(
                                        text = "Saldo Deudor",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Text(
                                    text = "$${String.format("%,.2f", totalPendingAmount)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (totalPendingAmount > 0.0) colors.error else colors.onSurface.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }

                    // Explanatory info bar inside summary card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.4f))
                            .padding(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Llevas $paidCount de $totalCount cuotas pagadas en este ciclo académico/deportivo.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Horizontal scrolling Filter Selector (Material 3 Filter Chips)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtrar por:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.onBackground.copy(alpha = 0.6f)
                )

                FilterChip(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    label = { Text("Todas") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("saldos_filter_all")
                )

                FilterChip(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    label = { Text("Pagadas") },
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        if (selectedFilter == 1) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    },
                    modifier = Modifier.testTag("saldos_filter_paid")
                )

                FilterChip(
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    label = { Text("Impagas") },
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        if (selectedFilter == 2) {
                            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    },
                    modifier = Modifier.testTag("saldos_filter_pending")
                )
            }
        }

        // List Header label
        item {
            Text(
                text = when (selectedFilter) {
                    1 -> "Detalle de Cuotas Pagadas"
                    2 -> "Detalle de Cuotas Impagas"
                    else -> "Detalle General de Saldos"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Empty state visual representation
        if (displayedDues.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inbox,
                            contentDescription = "Empty",
                            tint = colors.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No se encontraron registros de cuotas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        } else {
            // Dues list items
            items(displayedDues, key = { it.memberCuotaId }) { item ->
                val isPaid = item.status == "PAID"

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPaid) colors.surfaceVariant.copy(alpha = 0.2f) else colors.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = if (!isPaid) androidx.compose.foundation.BorderStroke(1.dp, colors.error.copy(alpha = 0.15f)) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isPaid) 0.dp else 1.5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saldo_item_${item.memberCuotaId}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category Label + Status Badge Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.primaryContainer.copy(alpha = 0.6f))
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

                            // Distinctive Payment Status Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isPaid) colors.primaryContainer.copy(alpha = 0.3f)
                                        else colors.errorContainer.copy(alpha = 0.4f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaid) Icons.Filled.CheckCircle else Icons.Filled.Pending,
                                    contentDescription = null,
                                    tint = if (isPaid) colors.primary else colors.error,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isPaid) "PAGADA" else "IMPAGA",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isPaid) colors.primary else colors.error,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        // Title & Details block
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.onBackground
                        )

                        if (item.description.isNotBlank()) {
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onBackground.copy(alpha = 0.6f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = colors.onBackground.copy(alpha = 0.05f)
                        )

                        // Amount & Timeline Dates
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
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (isPaid) colors.primary else colors.error
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (isPaid) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Verified,
                                            contentDescription = "Verified",
                                            tint = colors.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Abonado: ${item.paidDate?.let { viewModel.getFormattedDate(it) } ?: ""}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colors.primary
                                        )
                                    }
                                    Text(
                                        text = "Ref: ${item.paymentReference ?: ""}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        ),
                                        color = colors.onBackground.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Vence: ${viewModel.getFormattedDate(item.dueDate)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.error,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )

                                    // Quick payment trigger button
                                    Button(
                                        onClick = { onPayCuota(item) },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("saldos_pay_now_${item.memberCuotaId}")
                                    ) {
                                        Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pagar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
