package com.isa.reto2.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import kotlin.math.round

class FirestoreTransactionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Observa en tiempo real los datos del usuario en Firestore.
     */
    fun observeUser(uid: String): Flow<AppUser?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(AppUser::class.java)?.copy(uid = uid)
                if (user != null) {
                    SessionManager.currentUser = user
                }
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Observa en tiempo real el historial de transacciones de un usuario.
     */
    fun observeTransactionsByUser(uid: String): Flow<List<TransactionModel>> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TransactionModel::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Observa en tiempo real todas las transacciones para la pantalla de auditoría (Admin).
     */
    fun observeAllTransactions(): Flow<List<TransactionModel>> = callbackFlow {
        val listener = firestore.collectionGroup("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TransactionModel::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Registra la operación y actualiza el saldo de forma atómica usando runTransaction.
     */
    suspend fun executeTransaction(
        uid: String,
        currency: String,
        amountUsd: Double,
        rate: Double,
        type: String,
        photoPath: String? = null
    ): Result<TransactionModel> = runCatching {
        val userRef = firestore.collection("users").document(uid)
        val pair = "USD/$currency"

        // 1. Consultar la última tasa para este par en las transacciones del usuario
        val lastTxQuery = userRef.collection("transactions")
            .whereEqualTo("currencyPair", pair)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val previousRate = lastTxQuery.documents.firstOrNull()?.getDouble("exchangeRate")

        // Regla 2.5%: si no hay transacciones previas para el par, requiresAudit es false
        val (deviation, requiresAudit) = if (previousRate != null && previousRate > 0) {
            val dev = abs(rate - previousRate) / previousRate * 100.0
            val roundedDev = round(dev * 100) / 100.0
            Pair(roundedDev, roundedDev > AUDIT_THRESHOLD_PERCENT)
        } else {
            Pair(0.0, false)
        }

        // 2. Transacción atómica en Firestore: lee el saldo, valida y escribe
        val newTx = firestore.runTransaction { transaction ->
            val userDoc = transaction.get(userRef)
            if (!userDoc.exists()) {
                throw IllegalStateException("Usuario no encontrado")
            }

            val currentBalance = userDoc.getDouble("balanceUsd") ?: 0.0
            val email = userDoc.getString("email") ?: ""

            val newBalance = if (type == TYPE_SELL) {
                if (currentBalance < amountUsd) {
                    throw IllegalStateException("Balance insuficiente")
                }
                currentBalance - amountUsd
            } else {
                currentBalance + amountUsd
            }

            val roundedBalance = round(newBalance * 100) / 100.0
            val txRef = userRef.collection("transactions").document()

            val tx = TransactionModel(
                id = txRef.id,
                userId = uid,
                currencyPair = pair,
                amount = amountUsd,
                exchangeRate = rate,
                transactionType = type,
                timestamp = System.currentTimeMillis(),
                requiresAudit = requiresAudit,
                deviationPercent = deviation,
                photoPath = photoPath,
                email = email
            )

            transaction.update(userRef, "balanceUsd", roundedBalance)
            transaction.set(txRef, tx)

            tx
        }.await()

        // Actualizar sesión en memoria
        val updatedUserBalance = if (type == TYPE_SELL) {
            round(((SessionManager.currentUser?.balanceUsd ?: 0.0) - amountUsd) * 100) / 100.0
        } else {
            round(((SessionManager.currentUser?.balanceUsd ?: 0.0) + amountUsd) * 100) / 100.0
        }
        SessionManager.currentUser = SessionManager.currentUser?.copy(balanceUsd = updatedUserBalance)

        newTx
    }

    /**
     * Actualiza la foto de perfil del usuario en Firestore.
     */
    suspend fun updateProfilePhoto(uid: String, photoPath: String) {
        firestore.collection("users").document(uid)
            .update("photoPath", photoPath)
            .await()
        SessionManager.currentUser = SessionManager.currentUser?.copy(photoPath = photoPath)
    }
}
