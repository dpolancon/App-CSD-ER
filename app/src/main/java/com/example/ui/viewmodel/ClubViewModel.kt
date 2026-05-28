package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.ClubDatabase
import com.example.data.model.*
import com.example.data.repository.ClubRepository
import com.example.data.service.GeminiService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ClubViewModel(
    application: Application,
    private val repository: ClubRepository
) : AndroidViewModel(application) {

    // --- CURRENT SESSION STATE ---
    private val _currentUser = MutableStateFlow<Member?>(null)
    val currentUser: StateFlow<Member?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _registerError = MutableStateFlow<String?>(null)
    val registerError: StateFlow<String?> = _registerError.asStateFlow()

    // --- RECIENT NOTIFICATIONS FOR CURRENT USER ---
    val currentNotifications: Flow<List<ClubNotification>> = _currentUser.flatMapLatest { member ->
        if (member != null) {
            repository.getNotificationsForMemberFlow(member.id)
        } else {
            flowOf(emptyList())
        }
    }

    // --- PERSONAL DEBTS FOR CURRENT USER ---
    val currentDues: Flow<List<MemberCuotaWithDetails>> = _currentUser.flatMapLatest { member ->
        if (member != null) {
            repository.getMemberDuesWithDetails(member.id)
        } else {
            flowOf(emptyList())
        }
    }

    // --- ADMIN CENTRALIZED STATES ---
    val allMembers: StateFlow<List<Member>> = repository.getAllMembersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCuotas: StateFlow<List<Cuota>> = repository.getAllCuotasFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDuesAssignments: StateFlow<List<CuotaAssignmentWithMemberDetails>> = repository.getAllDuesAssignmentsWithMemberDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Asynchronously check and seed database from CSV if empty
        viewModelScope.launch {
            val count = repository.getMembersCount()
            if (count == 0) {
                com.example.data.database.DatabaseSeeder.seedFromCsv(application, repository)
                // Always seed the default admin user so that the administration panel remains accessible
                val demoAdmin = Member(
                    name = "Presidente del Club",
                    email = "admin@club.com",
                    phone = "+54 9 11 4444-8888",
                    memberNumber = "ADM-2026-0001",
                    passwordHash = "123",
                    role = "ADMIN"
                )
                repository.insertMember(demoAdmin)
            }
        }
    }

    // --- AUTHENTICATION ACTIONS ---
    fun login(email: String, passwordHash: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loginError.value = null
            val emailLower = email.trim().lowercase()
            val member = repository.getMemberByEmail(emailLower)
            if (member == null) {
                _loginError.value = "El correo no está registrado en el club."
                onResult(false)
            } else if (member.passwordHash != passwordHash) {
                _loginError.value = "Contraseña incorrecta."
                onResult(false)
            } else {
                _currentUser.value = member
                onResult(true)
            }
        }
    }

    fun register(name: String, email: String, phone: String, passwordHash: String, isAdmin: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _registerError.value = null
            val emailLower = email.trim().lowercase()
            if (name.isBlank() || emailLower.isBlank() || phone.isBlank() || passwordHash.isBlank()) {
                _registerError.value = "Todos los campos son obligatorios."
                onResult(false)
                return@launch
            }

            val existing = repository.getMemberByEmail(emailLower)
            if (existing != null) {
                _registerError.value = "El correo ya está registrado en el club."
                onResult(false)
                return@launch
            }

            val randomDigits = (1000..9999).random()
            val year = 2026
            val memberNumber = "SOC-$year-$randomDigits"

            val roleStr = if (isAdmin) "ADMIN" else "MEMBER"

            val newMember = Member(
                name = name.trim(),
                email = emailLower,
                phone = phone.trim(),
                memberNumber = memberNumber,
                passwordHash = passwordHash,
                role = roleStr
            )

            val newId = repository.insertMember(newMember)
            if (newId > 0) {
                val registeredMember = newMember.copy(id = newId)
                _currentUser.value = registeredMember

                // Send welcome notification
                repository.insertNotification(
                    ClubNotification(
                        memberId = newId,
                        title = "¡Bienvenido/a al Socio Club!",
                        body = "Hola ${name.trim()}, tu cuenta ha sido creada con éxito. Tu número de socio es $memberNumber.",
                        timestamp = System.currentTimeMillis()
                    )
                )

                // Assign any existing active cuota to this new member so they don't start with 0 debts if we have default cuotas!
                allCuotas.value.forEach { cuota ->
                    repository.insertMemberCuota(
                        MemberCuota(
                            memberId = newId,
                            cuotaId = cuota.id,
                            assignedDate = System.currentTimeMillis(),
                            status = "PENDING"
                        )
                    )
                }

                onResult(true)
            } else {
                _registerError.value = "Error al crear la cuenta. Intente nuevamente."
                onResult(false)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _loginError.value = null
        _registerError.value = null
    }

    // --- PASSWORD CHANGE FLOW FOR SOCIOS ---
    fun changePassword(memberId: Long, oldPass: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val member = repository.getMemberById(memberId)
            if (member == null) {
                onResult(false, "No se encontró el socio especificado.")
                return@launch
            }
            if (member.passwordHash != oldPass) {
                onResult(false, "La contraseña actual es incorrecta.")
                return@launch
            }
            if (newPass.length < 3) {
                onResult(false, "La nueva contraseña debe tener al menos 3 caracteres.")
                return@launch
            }
            repository.updateMemberPassword(memberId, newPass)
            if (_currentUser.value?.id == memberId) {
                _currentUser.value = member.copy(passwordHash = newPass)
            }
            onResult(true, "Contraseña actualizada exitosamente.")
        }
    }

    // --- DISPLAY NAME CHANGE FLOW FOR SOCIOS ---
    fun changeDisplayName(memberId: Long, newName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (newName.isBlank()) {
                onResult(false, "El nombre no puede estar vacío.")
                return@launch
            }
            val member = repository.getMemberById(memberId)
            if (member == null) {
                onResult(false, "Socio no encontrado.")
                return@launch
            }
            repository.updateMemberName(memberId, newName.trim())
            if (_currentUser.value?.id == memberId) {
                _currentUser.value = member.copy(name = newName.trim())
            }
            onResult(true, "Nombre actualizado exitosamente.")
        }
    }

    // --- SIMULATED CLUB SPREADSHEET (PLANILLA FUENTE DE VERDAD) ---
    data class SpreadsheetRow(
        val email: String,
        val name: String,
        val conceptTitle: String,
        val amount: Double,
        val category: String,
        val isPending: Boolean
    )

    private val _spreadsheetRows = MutableStateFlow<List<SpreadsheetRow>>(
        listOf(
            SpreadsheetRow("socio@club.com", "Diego Silva", "Cuota Gimnasia de Junio", 5000.0, "MEMBERSHIP", true),
            SpreadsheetRow("leo@club.com", "Lionel Paz", "Cuota Gimnasia de Junio", 5000.0, "MEMBERSHIP", true),
            SpreadsheetRow("invitado@example.com", "Juan Pérez (No Reg)", "Pase Diario Canchas", 3500.0, "OTHER", true),
            SpreadsheetRow("socio@club.com", "Diego Silva", "Seguimiento Médico Deportivo", 7500.0, "OTHER", true),
            SpreadsheetRow("leo@club.com", "Lionel Paz", "Inscripción Torneo Relámpago", 6000.0, "TOURNAMENT", true)
        )
    )
    val spreadsheetRows: StateFlow<List<SpreadsheetRow>> = _spreadsheetRows.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(
        listOf("Sistema listo. Planilla cargada: 5 filas pendientes de sincronizar.")
    )
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun updateSpreadsheetRow(index: Int, updatedRow: SpreadsheetRow) {
        val current = _spreadsheetRows.value.toMutableList()
        if (index in current.indices) {
            current[index] = updatedRow
            _spreadsheetRows.value = current
        }
    }

    fun addSpreadsheetRow(email: String, concept: String, amount: Double) {
        val current = _spreadsheetRows.value.toMutableList()
        current.add(SpreadsheetRow(email.trim(), "Fila Simulada", concept.trim(), amount, "OTHER", true))
        _spreadsheetRows.value = current
        _syncLogs.value = _syncLogs.value + "[PLANILLA LOG] Nueva fila añadida para '${email.trim()}': '${concept.trim()}' por $${amount}."
    }

    fun syncWithClubSpreadsheet() {
        viewModelScope.launch {
            _isSyncing.value = true
            val logs = mutableListOf<String>()
            logs.add("[INFO - %s] Iniciando sincronización con Planilla Oficial (Google Sheets Simulada)...".format(
                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            ))
            
            // Wait 1.5 seconds to make the desktop sync experience look incredibly authentic and interactive!
            kotlinx.coroutines.delay(1500)

            val rows = _spreadsheetRows.value
            var successCount = 0
            var failCount = 0

            rows.forEachIndexed { idx, row ->
                if (row.isPending) {
                    val member = repository.getMemberByEmail(row.email)
                    if (member != null) {
                        // 1. Create the Cuota definition if it doesn't exist
                        val allExistingCuotas = repository.getAllCuotasFlow().first()
                        var matchedCuota = allExistingCuotas.find { 
                            it.title.lowercase() == row.conceptTitle.lowercase() 
                        }
                        
                        val cuotaId = if (matchedCuota == null) {
                            val newC = Cuota(
                                title = row.conceptTitle,
                                description = "Generada automáticamente desde Planilla General de Administración.",
                                amount = row.amount,
                                dueDate = System.currentTimeMillis() + 864000000, // 10 days from now
                                category = row.category
                            )
                            val newId = repository.insertCuota(newC)
                            logs.add("[CREADO] Concepto nuevo '%s' (\$%,.2f) definido en sistema.".format(row.conceptTitle, row.amount))
                            newId
                        } else {
                            matchedCuota.id
                        }

                        // 2. Clear any existing pending assigned cuotas of this type for this member to avoid duplicates
                        val existingAssignments = repository.getMemberCuotasFlow(member.id).first()
                        val alreadyAssigned = existingAssignments.any { it.cuotaId == cuotaId }

                        if (!alreadyAssigned) {
                            repository.insertMemberCuota(
                                MemberCuota(
                                    memberId = member.id,
                                    cuotaId = cuotaId,
                                    assignedDate = System.currentTimeMillis(),
                                    status = "PENDING"
                                )
                            )
                            repository.insertNotification(
                                ClubNotification(
                                    memberId = member.id,
                                    title = "Nueva Cuenta por Pagar 📊",
                                    body = "Se sincronizó de la planilla la cuota '%s' de \$%,.2f.".format(row.conceptTitle, row.amount),
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            logs.add("[ÉXITO] Cuota '%s' asignada a socio %s (%s).".format(row.conceptTitle, member.name, row.email))
                        } else {
                            logs.add("[INFO] Cuota '%s' ya estaba asignada previamente a socio %s.".format(row.conceptTitle, member.name))
                        }

                        // Mark spreadsheet row as processed in local state
                        updateSpreadsheetRow(idx, row.copy(isPending = false))
                        successCount++
                    } else {
                        logs.add("[ERROR] Fila %d: El correo '%s' de '%s' no pertenece a ningún socio registrado.".format(idx + 1, row.email, row.name))
                        failCount++
                    }
                }
            }

            logs.add("[RESULTADO] Sincronización completa. Éxitos: %d | Errores por correo no registrado: %d.".format(successCount, failCount))
            _isSyncing.value = false
            _syncLogs.value = _syncLogs.value + logs
        }
    }

    // --- MEMER SIDE PAYMENT SIMULATOR ---
    fun payCuota(memberCuotaId: Long, paymentMethod: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val reference = "REF-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
            repository.updateMemberCuotaStatus(
                id = memberCuotaId,
                status = "PAID",
                paidDate = System.currentTimeMillis(),
                paymentReference = reference
            )

            // Dynamic find cuota details to display custom notification
            _currentUser.value?.let { member ->
                repository.insertNotification(
                    ClubNotification(
                        memberId = member.id,
                        title = "Pago Aprobado ⚽",
                        body = "Se ha registrado con éxito el pago de tu cuota mediante $paymentMethod. Ref: $reference.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // --- GEMINI PROOF-OF-PAYMENT REAL TIME ANALYSIS ---
    private val _receiptAnalysisResult = MutableStateFlow<GeminiService.ReceiptAnalysis?>(null)
    val receiptAnalysisResult: StateFlow<GeminiService.ReceiptAnalysis?> = _receiptAnalysisResult.asStateFlow()

    private val _isAnalyzingReceipt = MutableStateFlow(false)
    val isAnalyzingReceipt: StateFlow<Boolean> = _isAnalyzingReceipt.asStateFlow()

    fun resetReceiptAnalysis() {
        _receiptAnalysisResult.value = null
        _isAnalyzingReceipt.value = false
    }

    fun analyzeReceiptImage(bitmap: Bitmap, onResult: (GeminiService.ReceiptAnalysis) -> Unit = {}) {
        viewModelScope.launch {
            _isAnalyzingReceipt.value = true
            _receiptAnalysisResult.value = null
            val result = GeminiService.analyzeReceipt(bitmap)
            _receiptAnalysisResult.value = result
            _isAnalyzingReceipt.value = false
            onResult(result)
        }
    }

    // --- GENERAL NOTIFICATION READ STATE ---
    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }


    // --- ADMINISTRATIVE ACTIONS ---
    fun createCuotaAndAssign(title: String, description: String, amount: Double, monthsFromNow: Int, category: String) {
        viewModelScope.launch {
            if (title.isBlank() || amount <= 0.0) return@launch

            // Set due date as a timestamp in future
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.MONTH, monthsFromNow)
            val dueDate = calendar.timeInMillis

            val newCuota = Cuota(
                title = title.trim(),
                description = description.trim(),
                amount = amount,
                dueDate = dueDate,
                category = category
            )

            val cuotaId = repository.insertCuota(newCuota)

            if (cuotaId > 0) {
                // Get all members currently registered (excluding admins to be accurate)
                allMembers.value.forEach { member ->
                    if (member.role != "ADMIN") {
                        repository.insertMemberCuota(
                            MemberCuota(
                                memberId = member.id,
                                cuotaId = cuotaId,
                                assignedDate = System.currentTimeMillis(),
                                status = "PENDING"
                            )
                        )
                    }
                }

                // Insert broad club notification (simulates the automatic notifications!)
                repository.insertNotification(
                    ClubNotification(
                        memberId = null, // broadcast to everyone
                        title = "Nueva Cuota Registrada: $title 📢",
                        body = "Se ha asignado una nueva obligación de pago: $title de \$${String.format("%,.2f", amount)}. Fecha límite de pago: ${getFormattedDate(dueDate)}.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun adminMarkAsPaid(memberCuotaId: Long, reference: String) {
        viewModelScope.launch {
            repository.updateMemberCuotaStatus(
                id = memberCuotaId,
                status = "PAID",
                paidDate = System.currentTimeMillis(),
                paymentReference = reference.ifBlank { "REG-MANUAL-ADMIN" }
            )

            // Inject alert for the member
            val assignments = allDuesAssignments.value
            val match = assignments.find { it.memberCuotaId == memberCuotaId }
            if (match != null) {
                repository.insertNotification(
                    ClubNotification(
                        memberId = match.memberId,
                        title = "Pago Registrado por Administración 📝",
                        body = "La administración regularizó manualmente tu deuda para '${match.title}' por valor de \$${String.format("%,.2f", match.amount)}.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun adminSendNotification(title: String, body: String, memberId: Long?) {
        viewModelScope.launch {
            if (title.isBlank() || body.isBlank()) return@launch

            repository.insertNotification(
                ClubNotification(
                    memberId = memberId, // null for broad cast, or specific member
                    title = title.trim(),
                    body = body.trim(),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }


    // --- UTILS ---
    fun getFormattedDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    private suspend fun checkAndPopulateDefaults() {
        val emailCheck = "socio@club.com"
        val existingSocio = repository.getMemberByEmail(emailCheck)

        if (existingSocio == null) {
            // 1. Create a typical socio account
            val demoSocio = Member(
                name = "Diego Armando Silva",
                email = "socio@club.com",
                phone = "+54 9 11 5555-4321",
                memberNumber = "SOC-2026-0010",
                passwordHash = "123",
                role = "MEMBER"
            )
            val socioId = repository.insertMember(demoSocio)

            // 2. Create another member
            val demoSocio2 = Member(
                name = "Lionel Andrés Paz",
                email = "leo@club.com",
                phone = "+54 9 341 555-1010",
                memberNumber = "SOC-2026-0030",
                passwordHash = "123",
                role = "MEMBER"
            )
            val socio2Id = repository.insertMember(demoSocio2)

            // 3. Create an administrator account
            val demoAdmin = Member(
                name = "Presidente del Club",
                email = "admin@club.com",
                phone = "+54 9 11 4444-8888",
                memberNumber = "ADM-2026-0001",
                passwordHash = "123",
                role = "ADMIN"
            )
            repository.insertMember(demoAdmin)

            // 4. Create some standard club cuotas (due definitions)
            val cal = java.util.Calendar.getInstance()

            // Cuota 1: Already expired or very soon
            cal.add(java.util.Calendar.DAY_OF_YEAR, 3)
            val t1 = cal.timeInMillis
            val cuota1 = Cuota(
                title = "Cuota Social Mayo 2026",
                description = "Cuota social obligatoria mensual para el mantenimiento general de las canchas e instalaciones.",
                amount = 4500.0,
                dueDate = t1,
                category = "MEMBERSHIP"
            )
            val c1Id = repository.insertCuota(cuota1)

            // Cuota 2: Equipment fee due next month
            val cal2 = java.util.Calendar.getInstance()
            cal2.add(java.util.Calendar.DAY_OF_YEAR, 25)
            val t2 = cal2.timeInMillis
            val cuota2 = Cuota(
                title = "Equipamiento Nuevas Camisetas",
                description = "Cuota extraordinaria única destinada al rediseño e importación del conjunto oficial de juego (camiseta, shorts y medias).",
                amount = 12000.0,
                dueDate = t2,
                category = "EQUIPMENT"
            )
            val c2Id = repository.insertCuota(cuota2)

            // Cuota 3: Football tournament registration
            val cal3 = java.util.Calendar.getInstance()
            cal3.add(java.util.Calendar.DAY_OF_YEAR, 45)
            val t3 = cal3.timeInMillis
            val cuota3 = Cuota(
                title = "Inscripción Torneo Relámpago",
                description = "Cuota para cubrir gastos de arbitraje, hidratación y premios para el Torneo Apertura 2026.",
                amount = 6000.0,
                dueDate = t3,
                category = "TOURNAMENT"
            )
            val c3Id = repository.insertCuota(cuota3)


            // 5. Assign cuotas to our members (Debts)
            // Diego has: Mayo paid, and Camisetas pending, Inscripción pending.
            repository.insertMemberCuota(
                MemberCuota(memberId = socioId, cuotaId = c1Id, assignedDate = System.currentTimeMillis() - 864000000, status = "PAID", paidDate = System.currentTimeMillis() - 400000000, paymentReference = "REF-M4Y02026")
            )
            repository.insertMemberCuota(
                MemberCuota(memberId = socioId, cuotaId = c2Id, assignedDate = System.currentTimeMillis(), status = "PENDING")
            )
            repository.insertMemberCuota(
                MemberCuota(memberId = socioId, cuotaId = c3Id, assignedDate = System.currentTimeMillis(), status = "PENDING")
            )

            // Leo has: Mayo pending (overdue soon), Camisetas pending.
            repository.insertMemberCuota(
                MemberCuota(memberId = socio2Id, cuotaId = c1Id, assignedDate = System.currentTimeMillis() - 864000000, status = "PENDING")
            )
            repository.insertMemberCuota(
                MemberCuota(memberId = socio2Id, cuotaId = c2Id, assignedDate = System.currentTimeMillis(), status = "PENDING")
            )


            // 6. Insert some sample notifications
            repository.insertNotification(
                ClubNotification(
                    memberId = null,
                    title = "¡Mantenimiento de Canchas! 🏟️",
                    body = "Le informamos a todos los socios que las canchas de césped sintético estarán inactivas el lunes 25 por resembrado y mantenimiento técnico.",
                    timestamp = System.currentTimeMillis() - 172800000
                )
            )
            repository.insertNotification(
                ClubNotification(
                    memberId = socioId,
                    title = "Recordatorio de Pago ⏰",
                    body = "Tu cuota mensual de Mayo de \$4,500.00 vencerá pronto. Recuerda realizar el abono electrónico para mantenerte al día.",
                    timestamp = System.currentTimeMillis() - 86400000
                )
            )
        }
    }
}

// --- VIEWMODEL FACTORY PROTOCOL ---
class ClubViewModelFactory(
    private val application: Application,
    private val repository: ClubRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClubViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClubViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
