package com.example.data.database

import android.content.Context
import android.util.Log
import com.example.data.model.ClubNotification
import com.example.data.model.Cuota
import com.example.data.model.Member
import com.example.data.model.MemberCuota
import com.example.data.repository.ClubRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object DatabaseSeeder {

    private const val TAG = "DatabaseSeeder"

    suspend fun seedFromCsv(context: Context, repository: ClubRepository) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando carga masiva de socios desde CSV...")
            val inputStream = context.assets.open("padron_socios.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            // We use Kotlin's useLines which operates on a sequence to consume lines efficiently
            reader.useLines { lines ->
                var isHeader = true
                lines.forEach { line ->
                    if (isHeader) {
                        isHeader = false
                        return@forEach
                    }
                    if (line.trim().isEmpty()) return@forEach
                    
                    val parts = line.split(",").map { it.trim().removeSurrounding("\"") }
                    if (parts.size < 7) {
                        Log.w(TAG, "Línea ignorada por columnas insuficientes: $line")
                        return@forEach
                    }
                    
                    val memberNumber = parts[0]
                    val phone = parts[1] // RUT mapped to phone field
                    val name = parts[2]
                    val email = parts[3].lowercase()
                    val totalPaid = parts[4].toDoubleOrNull() ?: 0.0
                    val saldoDeudor = parts[5].toDoubleOrNull() ?: 0.0
                    
                    // Create default Member
                    val member = Member(
                        name = name,
                        email = email,
                        phone = phone,
                        memberNumber = memberNumber,
                        passwordHash = "123", // Default provisional password
                        role = "MEMBER"
                    )
                    
                    // Insert member
                    val memberId = repository.insertMember(member)
                    if (memberId > 0) {
                        Log.d(TAG, "Socio registrado con éxito: ${member.name} (ID: $memberId)")
                        
                        // Welcome Notification
                        repository.insertNotification(
                            ClubNotification(
                                memberId = memberId,
                                title = "¡Bienvenido al Ecosistema Estrella Roja! 🔴⚪",
                                body = "Hola ${member.name}, tu cuenta ha sido creada desde el padrón central. Tu número de socio es ${member.memberNumber}.",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        
                        // Check financial state and generate cuotas
                        if (saldoDeudor > 0.0) {
                            // Generate a pending cuota for this amount
                            val pendingCuota = Cuota(
                                title = "Saldo Deudor Padrón Sincronizado",
                                description = "Deuda consolidada registrada en el padrón administrativo de CSD Estrella Roja.",
                                amount = saldoDeudor,
                                dueDate = System.currentTimeMillis() + 864000000, // 10 days in future
                                category = "MEMBERSHIP"
                            )
                            val cuotaId = repository.insertCuota(pendingCuota)
                            if (cuotaId > 0) {
                                repository.insertMemberCuota(
                                    MemberCuota(
                                        memberId = memberId,
                                        cuotaId = cuotaId,
                                        assignedDate = System.currentTimeMillis(),
                                        status = "PENDING"
                                    )
                                )
                                Log.d(TAG, "Asignada cuota pendiente de \$${saldoDeudor} a socio ID: $memberId")
                            }
                        }
                        
                        if (saldoDeudor == 0.0 && totalPaid > 0.0) {
                            // Generate a paid cuota representing their contribution history
                            val paidCuota = Cuota(
                                title = "Cuota Padrón Cancelada",
                                description = "Historial de cuota social cancelada importada desde el padrón oficial del club.",
                                amount = totalPaid,
                                dueDate = System.currentTimeMillis() - 86400000, // Expired/past
                                category = "MEMBERSHIP"
                            )
                            val cuotaId = repository.insertCuota(paidCuota)
                            if (cuotaId > 0) {
                                repository.insertMemberCuota(
                                    MemberCuota(
                                        memberId = memberId,
                                        cuotaId = cuotaId,
                                        assignedDate = System.currentTimeMillis() - 86400000,
                                        status = "PAID",
                                        paidDate = System.currentTimeMillis() - 86400000,
                                        paymentReference = "REF-CSV-IMPORT"
                                    )
                                )
                                Log.d(TAG, "Registrada cuota pagada de \$${totalPaid} a socio ID: $memberId")
                            }
                        }
                    } else {
                        Log.e(TAG, "Error al insertar socio: ${member.name}")
                    }
                }
            }
            Log.d(TAG, "Carga masiva completada con éxito.")
        } catch (e: Exception) {
            Log.e(TAG, "Falla en DatabaseSeeder: ", e)
        }
    }
}
