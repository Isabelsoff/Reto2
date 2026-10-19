package com.isa.reto2.data

import com.isa.reto2.network.ErApiService
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val firestoreRepo: FirestoreTransactionRepository = FirestoreTransactionRepository()
) {
    fun observeByUser(uid: String): Flow<List<TransactionModel>> =
        firestoreRepo.observeTransactionsByUser(uid)

    fun observeAll(): Flow<List<TransactionModel>> =
        firestoreRepo.observeAllTransactions()

    suspend fun execute(
        uid: String,
        currency: String,
        amountUsd: Double,
        rate: Double,
        type: String,
        photoPath: String? = null
    ): Result<TransactionModel> = firestoreRepo.executeTransaction(uid, currency, amountUsd, rate, type, photoPath)
}

class RatesRepository(private val api: ErApiService) {
    suspend fun getRate(currency: String): Double {
        val resp = api.latestUsd()
        val rate = resp.rates?.get(currency)
        if (resp.result != "success" || rate == null) throw IllegalStateException("Tasa no disponible para $currency")
        return rate
    }
}
