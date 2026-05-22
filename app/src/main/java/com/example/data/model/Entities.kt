package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String,
    val memberNumber: String,
    val passwordHash: String,
    val role: String = "MEMBER" // "MEMBER" or "ADMIN"
)

@Entity(tableName = "cuotas")
data class Cuota(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val amount: Double,
    val dueDate: Long, // timestamp
    val category: String // "MEMBERSHIP", "TOURNAMENT", "EQUIPMENT", "OTHER"
)

@Entity(tableName = "member_cuotas")
data class MemberCuota(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val cuotaId: Long,
    val assignedDate: Long,
    val status: String, // "PENDING", "PAID"
    val paidDate: Long? = null,
    val paymentReference: String? = null
)

@Entity(tableName = "notifications")
data class ClubNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long?, // null means general broadcast
    val title: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false
)

// Data class to easily represent a socio's assigned fee with its full definition
data class MemberCuotaWithDetails(
    val memberCuotaId: Long,
    val memberId: Long,
    val cuotaId: Long,
    val title: String,
    val description: String,
    val amount: Double,
    val dueDate: Long,
    val category: String,
    val assignedDate: Long,
    val status: String,
    val paidDate: Long?,
    val paymentReference: String?
)

// Data class to represent assigned fee reports for administrative lists
data class CuotaAssignmentWithMemberDetails(
    val memberCuotaId: Long,
    val memberId: Long,
    val cuotaId: Long,
    val title: String,
    val description: String,
    val amount: Double,
    val dueDate: Long,
    val category: String,
    val assignedDate: Long,
    val status: String,
    val paidDate: Long?,
    val paymentReference: String?,
    val memberName: String,
    val memberNumber: String
)
