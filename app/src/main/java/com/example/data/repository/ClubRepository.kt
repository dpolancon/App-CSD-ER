package com.example.data.repository

import com.example.data.dao.ClubDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class ClubRepository(private val clubDao: ClubDao) {

    // --- MEMBER ACTIONS ---
    suspend fun getMemberByEmail(email: String): Member? = clubDao.getMemberByEmail(email)

    fun getMemberByIdFlow(id: Long): Flow<Member?> = clubDao.getMemberByIdFlow(id)

    suspend fun getMemberById(id: Long): Member? = clubDao.getMemberById(id)

    fun getAllMembersFlow(): Flow<List<Member>> = clubDao.getAllMembersFlow()

    suspend fun updateMemberPassword(id: Long, passwordHash: String) = clubDao.updateMemberPassword(id, passwordHash)

    suspend fun updateMemberName(id: Long, name: String) = clubDao.updateMemberName(id, name)

    suspend fun insertMember(member: Member): Long = clubDao.insertMember(member)

    suspend fun deleteMember(member: Member) = clubDao.deleteMember(member)


    // --- CUOTA (FEE DEFINITIONS) ACTIONS ---
    fun getAllCuotasFlow(): Flow<List<Cuota>> = clubDao.getAllCuotasFlow()

    suspend fun insertCuota(cuota: Cuota): Long = clubDao.insertCuota(cuota)

    suspend fun getCuotaById(id: Long): Cuota? = clubDao.getCuotaById(id)


    // --- MEMBER CUOTAS (ASSIGNED FEES / DEBTS) ACTIONS ---
    fun getMemberCuotasFlow(memberId: Long): Flow<List<MemberCuota>> = clubDao.getMemberCuotasFlow(memberId)

    fun getMemberDuesWithDetails(memberId: Long): Flow<List<MemberCuotaWithDetails>> = clubDao.getMemberDuesWithDetails(memberId)

    fun getAllDuesAssignmentsWithMemberDetails(): Flow<List<CuotaAssignmentWithMemberDetails>> = clubDao.getAllDuesAssignmentsWithMemberDetails()

    suspend fun insertMemberCuota(memberCuota: MemberCuota): Long = clubDao.insertMemberCuota(memberCuota)

    suspend fun updateMemberCuotaStatus(id: Long, status: String, paidDate: Long?, paymentReference: String?) =
        clubDao.updateMemberCuotaStatus(id, status, paidDate, paymentReference)


    // --- NOTIFICATIONS ACTIONS ---
    fun getNotificationsForMemberFlow(memberId: Long): Flow<List<ClubNotification>> = clubDao.getNotificationsForMemberFlow(memberId)

    suspend fun insertNotification(notification: ClubNotification): Long = clubDao.insertNotification(notification)

    suspend fun markNotificationAsRead(id: Long) = clubDao.markNotificationAsRead(id)
}
