package com.isa.reto2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.isa.reto2.data.FirestoreTransactionRepository
import com.isa.reto2.data.TransactionModel
import com.isa.reto2.data.TransactionRepository
import kotlinx.coroutines.flow.Flow

class AdminAuditViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TransactionRepository(FirestoreTransactionRepository())
    val rows: Flow<List<TransactionModel>> = repo.observeAll()
}
