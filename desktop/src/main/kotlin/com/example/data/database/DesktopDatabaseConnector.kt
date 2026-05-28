package com.example.data.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.sql.Types

data class MemberWithBalance(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val memberNumber: String,
    val role: String,
    val totalPendingDebt: Double
)

object DesktopDatabaseConnector {

    private const val DB_PATH = "club_social_futbol_db"

    init {
        // Load SQLite JDBC Driver
        Class.forName("org.sqlite.JDBC")
    }

    fun getConnection(): Connection {
        val url = "jdbc:sqlite:$DB_PATH"
        return DriverManager.getConnection(url)
    }

    fun initializeDatabase() {
        val sqlMembers = """
            CREATE TABLE IF NOT EXISTS members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                phone TEXT NOT NULL,
                memberNumber TEXT NOT NULL,
                passwordHash TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'MEMBER'
            )
        """.trimIndent()

        val sqlCuotas = """
            CREATE TABLE IF NOT EXISTS cuotas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                dueDate INTEGER NOT NULL,
                category TEXT NOT NULL
            )
        """.trimIndent()

        val sqlMemberCuotas = """
            CREATE TABLE IF NOT EXISTS member_cuotas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                memberId INTEGER NOT NULL,
                cuotaId INTEGER NOT NULL,
                assignedDate INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'PENDING',
                paidDate INTEGER,
                paymentReference TEXT,
                FOREIGN KEY(memberId) REFERENCES members(id),
                FOREIGN KEY(cuotaId) REFERENCES cuotas(id)
            )
        """.trimIndent()

        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute(sqlMembers)
                    stmt.execute(sqlCuotas)
                    stmt.execute(sqlMemberCuotas)
                }
            }
        } catch (e: Exception) {
            println("Database initialization failed: ${e.message}")
        }
    }

    fun insertMember(name: String, email: String, phone: String, memberNumber: String, passwordHash: String, role: String): Long {
        val sql = "INSERT INTO members (name, email, phone, memberNumber, passwordHash, role) VALUES (?, ?, ?, ?, ?, ?)"
        try {
            getConnection().use { conn ->
                conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                    stmt.setString(1, name.trim())
                    stmt.setString(2, email.trim().lowercase())
                    stmt.setString(3, phone.trim())
                    stmt.setString(4, memberNumber)
                    stmt.setString(5, passwordHash)
                    stmt.setString(6, role)
                    stmt.executeUpdate()
                    stmt.generatedKeys.use { rs ->
                        if (rs.next()) return rs.getLong(1)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error inserting member: ${e.message}")
        }
        return -1L
    }

    fun insertCuota(title: String, description: String, amount: Double, dueDate: Long, category: String): Long {
        val sql = "INSERT INTO cuotas (title, description, amount, dueDate, category) VALUES (?, ?, ?, ?, ?)"
        try {
            getConnection().use { conn ->
                conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                    stmt.setString(1, title.trim())
                    stmt.setString(2, description.trim())
                    stmt.setDouble(3, amount)
                    stmt.setLong(4, dueDate)
                    stmt.setString(5, category)
                    stmt.executeUpdate()
                    stmt.generatedKeys.use { rs ->
                        if (rs.next()) return rs.getLong(1)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error inserting cuota: ${e.message}")
        }
        return -1L
    }

    fun insertMemberCuota(memberId: Long, cuotaId: Long, assignedDate: Long, status: String, paidDate: Long?, paymentReference: String?): Long {
        val sql = "INSERT INTO member_cuotas (memberId, cuotaId, assignedDate, status, paidDate, paymentReference) VALUES (?, ?, ?, ?, ?, ?)"
        try {
            getConnection().use { conn ->
                conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                    stmt.setLong(1, memberId)
                    stmt.setLong(2, cuotaId)
                    stmt.setLong(3, assignedDate)
                    stmt.setString(4, status)
                    if (paidDate != null) stmt.setLong(5, paidDate) else stmt.setNull(5, Types.INTEGER)
                    if (paymentReference != null) stmt.setString(6, paymentReference) else stmt.setNull(6, Types.VARCHAR)
                    stmt.executeUpdate()
                    stmt.generatedKeys.use { rs ->
                        if (rs.next()) return rs.getLong(1)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error inserting member_cuota: ${e.message}")
        }
        return -1L
    }

    fun isEmailRegistered(email: String): Boolean {
        val sql = "SELECT 1 FROM members WHERE email = ? LIMIT 1"
        try {
            getConnection().use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, email.trim().lowercase())
                    stmt.executeQuery().use { rs ->
                        return rs.next()
                    }
                }
            }
        } catch (e: Exception) {
            println("Error checking email: ${e.message}")
        }
        return false
    }

    fun getAllMembersWithBalances(): List<MemberWithBalance> {
        val sql = """
            SELECT m.id, m.name, m.email, m.phone, m.memberNumber, m.role,
                   COALESCE(SUM(CASE WHEN mc.status = 'PENDING' THEN c.amount ELSE 0.0 END), 0.0) as totalPending
            FROM members m
            LEFT JOIN member_cuotas mc ON m.id = mc.memberId
            LEFT JOIN cuotas c ON mc.cuotaId = c.id
            WHERE m.role = 'MEMBER'
            GROUP BY m.id
            ORDER BY m.name ASC
        """.trimIndent()
        
        val list = mutableListOf<MemberWithBalance>()
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery(sql).use { rs ->
                        while (rs.next()) {
                            list.add(
                                MemberWithBalance(
                                    id = rs.getLong("id"),
                                    name = rs.getString("name"),
                                    email = rs.getString("email"),
                                    phone = rs.getString("phone"),
                                    memberNumber = rs.getString("memberNumber"),
                                    role = rs.getString("role"),
                                    totalPendingDebt = rs.getDouble("totalPending")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error listing members: ${e.message}")
        }
        return list
    }
}
