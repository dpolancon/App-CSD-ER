package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ClubDao
import com.example.data.model.ClubNotification
import com.example.data.model.Cuota
import com.example.data.model.Member
import com.example.data.model.MemberCuota

@Database(
    entities = [Member::class, Cuota::class, MemberCuota::class, ClubNotification::class],
    version = 1,
    exportSchema = false
)
abstract class ClubDatabase : RoomDatabase() {
    abstract fun clubDao(): ClubDao

    companion object {
        @Volatile
        private var INSTANCE: ClubDatabase? = null

        fun getDatabase(context: Context): ClubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClubDatabase::class.java,
                    "club_social_futbol_db"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
