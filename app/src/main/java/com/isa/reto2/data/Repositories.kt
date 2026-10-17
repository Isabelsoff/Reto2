package com.isa.reto2.data

import androidx.room.withTransaction
import com.isa.reto2.network.ErApiService
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs
import kotlin.math.round

class TransactionRepository(private val db: AppDatabase) {
    private val users = db.userDao()
    private val txs = db.transactionDao()

    fun observeByUser(userId: Long): Flow<List<TransactionEntity>> = txs.observeByUser(userId)
    fun observeAll(): Flow<List<AuditRow>> = txs.observeAll()

    /**
     * Registra la operación y actualiza el balance en UNA sola transacción de Room.
     * Lanza IllegalStateException si el saldo no alcanza (ventas).
     */
    suspend fun execute(
        userId: Long,
        currency: String,
        amountUsd: Double,
        rate: Double,
        type: String,
        photoPath: String? = null
    ): TransactionEntity = db.withTransaction {
        val user = users.getById(userId) ?: throw IllegalStateException("Usuario no encontrado")
        val newBalance = if (type == TYPE_SELL) {
            if (user.balanceUsd < amountUsd) throw IllegalStateException("Balance insuficiente")
            user.balanceUsd - amountUsd
        } else {
            user.balanceUsd + amountUsd
        }
        val pair = "USD/$currency"
        val previous = txs.lastRateForPair(pair)
        val deviation = if (previous != null && previous > 0) abs(rate - previous) / previous * 100.0 else 0.0
        val tx = TransactionEntity(
            userId = userId,
            currencyPair = pair,
            amount = amountUsd,
            currentExchangeRate = rate,
            transactionType = type,
            timestamp = System.currentTimeMillis(),
            requiresAudit = deviation > AUDIT_THRESHOLD_PERCENT,
            deviationPercent = round(deviation * 100) / 100.0,
            photoPath = photoPath
        )
        val id = txs.insert(tx)
        users.updateBalance(userId, round(newBalance * 100) / 100.0)
        tx.copy(id = id)
    }
}

class RatesRepository(private val api: ErApiService) {
    suspend fun getRate(currency: String): Double {
        val resp = api.latestUsd()
        val rate = resp.rates?.get(currency)
        if (resp.result != "success" || rate == null) throw IllegalStateException("Tasa no disponible para $currency")
        return rate
    }
}
