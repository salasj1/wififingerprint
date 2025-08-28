package com.ucab.wififingerprint.data.db

// data/db/AppDatabase.kt
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ucab.wififingerprint.data.model.WifiFingerprint
import com.ucab.wififingerprint.data.model.WifiReading

@Database(entities = [WifiFingerprint::class, WifiReading::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fingerprintDao(): FingerprintDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
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
