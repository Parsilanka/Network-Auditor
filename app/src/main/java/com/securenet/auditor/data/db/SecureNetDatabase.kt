package com.securenet.auditor.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScanResultEntity::class, SpeedTestEntity::class, ArpEntity::class], version = 6, exportSchema = false)
abstract class SecureNetDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao
    abstract fun speedTestDao(): SpeedTestDao
    abstract fun arpDao(): ArpDao

    companion object {
        @Volatile private var INSTANCE: SecureNetDatabase? = null
        fun getInstance(context: Context): SecureNetDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, SecureNetDatabase::class.java, "securenet_db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
