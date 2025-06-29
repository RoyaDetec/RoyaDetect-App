// data/database/AppDatabase.kt
package com.example.royadetect.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.royadetect.data.dao.ReportDao
import com.example.royadetect.data.entity.Report

@Database(
    entities = [Report::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "royadetect_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}