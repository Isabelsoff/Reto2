package com.isa.reto2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.isa.reto2.RetoApp
import com.isa.reto2.data.AuditRow
import com.isa.reto2.data.TransactionRepository
import kotlinx.coroutines.flow.Flow

class AdminAuditViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TransactionRepository((app as RetoApp).database)
    val rows: Flow<List<AuditRow>> = repo.observeAll()
}
