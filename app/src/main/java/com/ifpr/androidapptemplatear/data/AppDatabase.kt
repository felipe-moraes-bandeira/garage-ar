package com.ifpr.androidapptemplatear.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Vehicle::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao

    companion object {
        /** v2: localização onde o carro foi cadastrado. Mantém os carros já salvos. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vehicles ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN longitude REAL")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "garage_ar.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
