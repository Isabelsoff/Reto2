package com.isa.reto2.data

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
