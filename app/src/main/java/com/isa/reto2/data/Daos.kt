package com.isa.reto2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<UserEntity?>

    @Query("UPDATE users SET photoPath = :path WHERE id = :id")
    suspend fun updatePhoto(id: Long, path: String?)

    @Query("UPDATE users SET balanceUsd = :balance WHERE id = :id")
    suspend fun updateBalance(id: Long, balance: Double)
}

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(tx: TransactionEntity): Long

    @Query("SELECT currentExchangeRate FROM transactions WHERE currencyPair = :pair ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun lastRateForPair(pair: String): Double?

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC, id DESC")
    fun observeByUser(userId: Long): Flow<List<TransactionEntity>>

    @Query(
        "SELECT t.*, u.email AS email FROM transactions t " +
            "INNER JOIN users u ON u.id = t.userId ORDER BY t.timestamp DESC, t.id DESC"
    )
    fun observeAll(): Flow<List<AuditRow>>
}
