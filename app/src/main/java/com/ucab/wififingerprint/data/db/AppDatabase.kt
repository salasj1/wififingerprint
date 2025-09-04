package com.ucab.wififingerprint.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocationEntity::class, WifiReadingEntity::class], version = 1,exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fingerprintDao(): FingerprintDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fingerprint_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
