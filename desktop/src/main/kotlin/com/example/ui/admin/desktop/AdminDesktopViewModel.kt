package com.example.ui.admin.desktop

import com.example.data.database.DesktopDatabaseConnector
import com.example.data.database.MemberWithBalance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminDesktopViewModel {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _members = MutableStateFlow<List<MemberWithBalance>>(emptyList())
    val members: StateFlow<List<MemberWithBalance>> = _members.asStateFlow()

    val searchQuery = MutableStateFlow("")

    // Filtered members list based on query
    val filteredMembers: StateFlow<List<MemberWithBalance>> = combine(_members, searchQuery) { membersList, query ->
        if (query.isBlank()) {
            membersList
        } else {
            membersList.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.memberNumber.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    // Form inputs state flows
    val formName = MutableStateFlow("")
    val formEmail = MutableStateFlow("")
    val formPhone = MutableStateFlow("")
    val formCuotaAmount = MutableStateFlow("4500.00")
    val formCuotaConcept = MutableStateFlow("Cuota Social Mayo 2026")

    // Validation state
    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        // Initialize SQLite tables if not present
        DesktopDatabaseConnector.initializeDatabase()
        // Load initial members list
        loadMembers()
    }

    fun loadMembers() {
        scope.launch {
            val list = DesktopDatabaseConnector.getAllMembersWithBalances()
            _members.value = list
        }
    }

    fun saveNewMember() {
        val name = formName.value.trim()
        val email = formEmail.value.trim().lowercase()
        val phone = formPhone.value.trim()
        val amountStr = formCuotaAmount.value.trim()
        val concept = formCuotaConcept.value.trim()

        _validationError.value = null
        _successMessage.value = null

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || amountStr.isEmpty() || concept.isEmpty()) {
            _validationError.value = "Error: Todos los campos del socio y cuota son obligatorios."
            return
        }

        // Email validation
        if (!email.contains("@") || !email.contains(".")) {
            _validationError.value = "Error: El correo electrónico ingresado no tiene un formato válido."
            return
        }

        // Check if email already registered
        if (DesktopDatabaseConnector.isEmailRegistered(email)) {
            _validationError.value = "Error: El correo '$email' ya se encuentra registrado en el club."
            return
        }

        // Amount validation
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _validationError.value = "Error: El monto de la cuota inicial debe ser un número positivo válido."
            return
        }

        scope.launch {
            // 1. Generate unique Member Number SOC-2026-XXXX
            val randDigits = (1000..9999).random()
            val memberNumber = "SOC-2026-$randDigits"

            // 2. Insert member in SQLite
            val memberId = DesktopDatabaseConnector.insertMember(
                name = name,
                email = email,
                phone = phone,
                memberNumber = memberNumber,
                passwordHash = "123", // Default provisional password
                role = "MEMBER"
            )

            if (memberId > 0) {
                // 3. Insert initial Cuota definition
                val cuotaId = DesktopDatabaseConnector.insertCuota(
                    title = concept,
                    description = "Cuota social inicial asignada automáticamente al alta del socio.",
                    amount = amount,
                    dueDate = System.currentTimeMillis() + 864000000, // 10 days in future
                    category = "MEMBERSHIP"
                )

                if (cuotaId > 0) {
                    // 4. Assign the cuota to the newly created member (State: PENDING)
                    DesktopDatabaseConnector.insertMemberCuota(
                        memberId = memberId,
                        cuotaId = cuotaId,
                        assignedDate = System.currentTimeMillis(),
                        status = "PENDING",
                        paidDate = null,
                        paymentReference = null
                    )
                }

                // Clean form fields
                formName.value = ""
                formEmail.value = ""
                formPhone.value = ""
                formCuotaAmount.value = "4500.00"
                formCuotaConcept.value = "Cuota Social Mayo 2026"

                _successMessage.value = "¡Socio creado con éxito!\nCarnet: $memberNumber\nClave provisoria: '123' (asignada por defecto)."
                
                // Refresh list
                loadMembers()
            } else {
                _validationError.value = "Error: No se pudo registrar el socio en la base de datos local."
            }
        }
    }

    fun clearForm() {
        formName.value = ""
        formEmail.value = ""
        formPhone.value = ""
        formCuotaAmount.value = "4500.00"
        formCuotaConcept.value = "Cuota Social Mayo 2026"
        _validationError.value = null
        _successMessage.value = null
    }
}
