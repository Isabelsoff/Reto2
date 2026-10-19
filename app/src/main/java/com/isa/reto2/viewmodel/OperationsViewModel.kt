package com.isa.reto2.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isa.reto2.data.AppUser
import com.isa.reto2.data.FirestoreTransactionRepository
import com.isa.reto2.data.RatesRepository
import com.isa.reto2.data.SessionManager
import com.isa.reto2.data.TYPE_BUY
import com.isa.reto2.data.TransactionModel
import com.isa.reto2.data.TransactionRepository
import com.isa.reto2.hardware.VibrationHelper
import com.isa.reto2.network.ErApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


val CURRENCIES = listOf("COP", "EUR", "GBP", "JPY")

data class OpsUiState(
    val currency: String = "COP",
    val rate: Double? = null,
    val loadingRate: Boolean = false,
    val amount: String = "",
    val photoBitmap: Bitmap? = null,
    val error: String? = null,
    val message: String? = null,
    val auditAlert: Boolean = false
)

class OperationsViewModel(app: Application) : AndroidViewModel(app) {
    private val firestoreRepo = FirestoreTransactionRepository()
    private val txRepo = TransactionRepository(firestoreRepo)
    private val ratesRepo = RatesRepository(ErApiService.instance)
    private val vibrationHelper = VibrationHelper(app)

    private val _state = MutableStateFlow(OpsUiState())
    val state: StateFlow<OpsUiState> = _state.asStateFlow()

    private val userUid: String? get() = SessionManager.currentUser?.uid

    val history: Flow<List<TransactionModel>> =
        userUid?.let { txRepo.observeByUser(it) } ?: emptyFlow()

    val balance: Flow<Double> =
        userUid?.let { uid -> firestoreRepo.observeUser(uid).map { u: AppUser? -> u?.balanceUsd ?: 0.0 } }
            ?: emptyFlow()

    private var rateJob: Job? = null
    private var requestId = 0

    init { loadRate() }

    fun selectCurrency(currency: String) {
        _state.update { it.copy(currency = currency, error = null, message = null) }
        loadRate()
    }

    fun onAmountChange(value: String) {
        _state.update { it.copy(amount = value, error = null, message = null) }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        vibrationHelper.vibrateClick()
        _state.update { it.copy(photoBitmap = bitmap) }
    }

    fun removePhoto() {
        vibrationHelper.vibrateClick()
        _state.update { it.copy(photoBitmap = null) }
    }

    fun dismissAuditAlert() {
        _state.update { it.copy(auditAlert = false) }
    }

    /** Ignora respuestas tardías: solo aplica la de la última solicitud. */
    fun loadRate() {
        val id = ++requestId
        val currency = _state.value.currency
        rateJob?.cancel()
        _state.update { it.copy(loadingRate = true, rate = null, error = null) }
        rateJob = viewModelScope.launch {
            try {
                val r = ratesRepo.getRate(currency)
                if (id == requestId) _state.update { it.copy(rate = r, loadingRate = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (id == requestId) _state.update {
                    it.copy(loadingRate = false, error = "No se pudo obtener la tasa. Revisa tu conexión.")
                }
            }
        }
    }

    fun execute(type: String) {
        val uid = userUid
        if (uid == null) {
            _state.update { it.copy(error = "Sesión no válida") }
            return
        }
        val s = _state.value
        val amount = s.amount.replace(',', '.').toDoubleOrNull()
        val rate = s.rate
        when {
            rate == null -> {
                vibrationHelper.vibrateError()
                _state.update { it.copy(error = "Aún no hay una tasa disponible") }
                return
            }
            amount == null || amount <= 0.0 -> {
                vibrationHelper.vibrateError()
                _state.update { it.copy(error = "Ingresa una cantidad válida en USD") }
                return
            }
        }

        viewModelScope.launch {
            val photoPath = s.photoBitmap?.let { bmp ->
                withContext(Dispatchers.IO) {
                    val ctx = getApplication<Application>()
                    val file = File(ctx.filesDir, "tx_${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    file.absolutePath
                }
            }

            txRepo.execute(uid, s.currency, amount, rate, type, photoPath)
                .onSuccess { tx ->
                    if (tx.requiresAudit) {
                        // Patrón largo de vibración: más fuerte que el de éxito normal
                        vibrationHelper.vibrateWaveform(
                            longArrayOf(0, 300, 100, 500, 100, 300),
                            intArrayOf(0, 255, 0, 255, 0, 255)
                        )
                    } else {
                        vibrationHelper.vibrateSuccess()
                    }

                    _state.update {
                        it.copy(
                            amount = "",
                            photoBitmap = null,
                            error = null,
                            message = if (tx.requiresAudit) null else "$type registrada",
                            auditAlert = tx.requiresAudit
                        )
                    }
                }
                .onFailure { e ->
                    vibrationHelper.vibrateError()
                    _state.update { it.copy(error = e.message ?: "Error al registrar", message = null) }
                }
        }
    }
}
