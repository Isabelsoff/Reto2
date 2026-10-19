package com.isa.reto2.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

const val ROLE_ADMIN = "Admin"
const val ROLE_USER = "User"
const val TYPE_BUY = "Compra"
const val TYPE_SELL = "Venta"
const val AUDIT_THRESHOLD_PERCENT = 2.5

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val role: String = ROLE_USER,
    val balanceUsd: Double = 10000.0,
    val photoPath: String? = null
)

data class TransactionModel(
    val id: String = "",
    val userId: String = "",
    val currencyPair: String = "",
    val amount: Double = 0.0,
    val exchangeRate: Double = 0.0,
    val transactionType: String = "",
    val timestamp: Long = 0L,
    val requiresAudit: Boolean = false,
    val deviationPercent: Double = 0.0,
    val photoPath: String? = null,
    val email: String = ""
)

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val password: String,
    val role: String,
    @ColumnInfo(defaultValue = "10000.0") val balanceUsd: Double = 10000.0,
    val photoPath: String? = null
)

@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId"), Index("currencyPair")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val currencyPair: String,
    val amount: Double,
    val currentExchangeRate: Double,
    val transactionType: String,
    val timestamp: Long,
    val requiresAudit: Boolean,
    val deviationPercent: Double,
    val photoPath: String? = null
)

data class AuditRow(
    @Embedded val tx: TransactionEntity,
    val email: String
)
