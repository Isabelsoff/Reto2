package com.isa.reto2.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isa.reto2.RetoApp
import com.isa.reto2.data.RatesRepository
import com.isa.reto2.data.SessionManager
import com.isa.reto2.data.TYPE_BUY
import com.isa.reto2.data.TransactionEntity
import com.isa.reto2.data.TransactionRepository
import com.isa.reto2.data.UserEntity
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
    val message: String? = null
)

class OperationsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as RetoApp).database
    private val txRepo = TransactionRepository(db)
    private val ratesRepo = RatesRepository(ErApiService.instance)
    private val vibrationHelper = VibrationHelper(app)

    private val _state = MutableStateFlow(OpsUiState())
    val state: StateFlow<OpsUiState> = _state.asStateFlow()

    private val userId: Long? = SessionManager.currentUser?.id

    val history: Flow<List<TransactionEntity>> =
        userId?.let { txRepo.observeByUser(it) } ?: emptyFlow()

    val balance: Flow<Double> =
        userId?.let { id -> db.userDao().observeById(id).map { u: UserEntity? -> u?.balanceUsd ?: 0.0 } }
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
        val uid = userId ?: return
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
            try {
                // Si hay una foto cargada, se guarda en disco y se obtiene su ruta
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

                val tx = txRepo.execute(uid, s.currency, amount, rate, type, photoPath)
                val extra = if (tx.requiresAudit) " (marcada para auditoría)" else ""

                // REQUERIMIENTO HÁPTICO:
                // Si es una COMPRA y el monto es mayor a 200 USD, se activa una vibración intensa/prolongada
                if (type == TYPE_BUY && amount > 200.0) {
                    val timings = longArrayOf(0, 300, 100, 500, 100, 300)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    vibrationHelper.vibrateWaveform(timings, amplitudes)
                } else {
                    vibrationHelper.vibrateSuccess()
                }

                _state.update {
                    it.copy(
                        amount = "",
                        photoBitmap = null,
                        error = null,
                        message = "$type registrada$extra"
                    )
                }
            } catch (e: IllegalStateException) {
                vibrationHelper.vibrateError()
                _state.update { it.copy(error = e.message ?: "Error al registrar", message = null) }
            }
        }
    }
}
