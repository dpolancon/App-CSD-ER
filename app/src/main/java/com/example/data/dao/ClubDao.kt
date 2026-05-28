package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClubDao {

    // --- MEMBER ACTIONS ---
    @Query("SELECT * FROM members WHERE email = :email LIMIT 1")
    suspend fun getMemberByEmail(email: String): Member?

    @Query("SELECT * FROM members WHERE id = :id")
    fun getMemberByIdFlow(id: Long): Flow<Member?>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Long): Member?

    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembersFlow(): Flow<List<Member>>

    @Query("SELECT COUNT(*) FROM members")
    suspend fun getMembersCount(): Int

    @Query("UPDATE members SET passwordHash = :passwordHash WHERE id = :id")
    suspend fun updateMemberPassword(id: Long, passwordHash: String)

    @Query("UPDATE members SET name = :name WHERE id = :id")
    suspend fun updateMemberName(id: Long, name: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMember(member: Member): Long

    @Delete
    suspend fun deleteMember(member: Member)


    // --- CUOTA (FEE DEFINITIONS) ACTIONS ---
    @Query("SELECT * FROM cuotas ORDER BY dueDate DESC")
    fun getAllCuotasFlow(): Flow<List<Cuota>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCuota(cuota: Cuota): Long

    @Query("SELECT * FROM cuotas WHERE id = :id")
    suspend fun getCuotaById(id: Long): Cuota?


    // --- MEMBER CUOTAS (ASSIGNED FEES / DEBTS) ACTIONS ---
    @Query("SELECT * FROM member_cuotas WHERE memberId = :memberId")
    fun getMemberCuotasFlow(memberId: Long): Flow<List<MemberCuota>>

    // Fetch deep information of a single user's debts
    @Query("""
        SELECT mc.id as memberCuotaId, mc.memberId, mc.cuotaId, c.title, c.description, c.amount, c.dueDate, c.category, mc.assignedDate, mc.status, mc.paidDate, mc.paymentReference 
        FROM member_cuotas mc 
        INNER JOIN cuotas c ON mc.cuotaId = c.id 
        WHERE mc.memberId = :memberId 
        ORDER BY mc.status DESC, c.dueDate ASC
    """)
    fun getMemberDuesWithDetails(memberId: Long): Flow<List<MemberCuotaWithDetails>>

    // Fetch deep information of ALL debts (useful for Admin)
    @Query("""
        SELECT mc.id as memberCuotaId, mc.memberId, mc.cuotaId, c.title, c.description, c.amount, c.dueDate, c.category, mc.assignedDate, mc.status, mc.paidDate, mc.paymentReference, m.name as memberName, m.memberNumber as memberNumber 
        FROM member_cuotas mc 
        INNER JOIN cuotas c ON mc.cuotaId = c.id 
        INNER JOIN members m ON mc.memberId = m.id 
        ORDER BY mc.status DESC, mc.assignedDate DESC
    """)
    fun getAllDuesAssignmentsWithMemberDetails(): Flow<List<CuotaAssignmentWithMemberDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemberCuota(memberCuota: MemberCuota): Long

    // Special query to update billing status
    @Query("UPDATE member_cuotas SET status = :status, paidDate = :paidDate, paymentReference = :paymentReference WHERE id = :id")
    suspend fun updateMemberCuotaStatus(id: Long, status: String, paidDate: Long?, paymentReference: String?)


    // --- NOTIFICATIONS ACTIONS ---
    @Query("SELECT * FROM notifications WHERE memberId = :memberId OR memberId IS NULL ORDER BY timestamp DESC")
    fun getNotificationsForMemberFlow(memberId: Long): Flow<List<ClubNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: ClubNotification): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Long)
}
