package com.isa.reto2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserEntity::class, TransactionEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "reto2.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(seedCallback)
                    .build()
                    .also { instance = it }
            }

        /** Usuarios y transacciones por defecto (se ejecuta solo al crear la BD). */
        private val seedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT INTO users (email, password, role, balanceUsd, photoPath) VALUES (?, ?, ?, ?, NULL)",
                    arrayOf<Any>("admin@reto.com", Security.sha256("admin123"), ROLE_ADMIN, 10000.0)
                )
                db.execSQL(
                    "INSERT INTO users (email, password, role, balanceUsd, photoPath) VALUES (?, ?, ?, ?, NULL)",
                    arrayOf<Any>("user@reto.com", Security.sha256("user123"), ROLE_USER, 10120.0)
                )
                val now = System.currentTimeMillis()
                val sql = "INSERT INTO transactions (userId, currencyPair, amount, currentExchangeRate, " +
                    "transactionType, timestamp, requiresAudit, deviationPercent, photoPath) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL)"
                db.execSQL(sql, arrayOf<Any>(2L, "USD/COP", 100.0, 4000.0, TYPE_BUY, now - 3_000_000L, 0, 0.0))
                db.execSQL(sql, arrayOf<Any>(2L, "USD/COP", 50.0, 4150.0, TYPE_BUY, now - 2_000_000L, 1, 3.75))
                db.execSQL(sql, arrayOf<Any>(2L, "USD/COP", 30.0, 4160.0, TYPE_SELL, now - 1_000_000L, 0, 0.24))
            }
        }
    }
}
