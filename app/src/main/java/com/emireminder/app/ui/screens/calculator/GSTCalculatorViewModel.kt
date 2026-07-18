package com.emireminder.app.ui.screens.calculator

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class GstMode { ADD, REMOVE }
enum class GstTransactionType { INTRA, INTER }

data class GSTUiState(
    val mode: GstMode = GstMode.ADD,
    val amountText: String = "10000",
    val selectedRate: Int = 18,
    val gstRates: List<Int> = listOf(0, 5, 12, 18, 28),
    val transactionType: GstTransactionType = GstTransactionType.INTRA,
    val total: Double = 0.0,
    val gstAmount: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val igst: Double = 0.0,
)

@HiltViewModel
class GSTCalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(GSTUiState().recalculated())
    val uiState: StateFlow<GSTUiState> = _uiState.asStateFlow()

    fun setMode(mode: GstMode) = update { copy(mode = mode) }
    fun setAmountText(text: String) = update { copy(amountText = text.filter { it.isDigit() || it == '.' }) }
    fun setRate(rate: Int) = update { copy(selectedRate = rate) }
    fun setTransactionType(type: GstTransactionType) = update { copy(transactionType = type) }

    private fun update(block: GSTUiState.() -> GSTUiState) {
        _uiState.value = _uiState.value.block().recalculated()
    }

    private fun GSTUiState.recalculated(): GSTUiState {
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val rate = selectedRate / 100.0
        val gst: Double
        val totalAmt: Double
        when (mode) {
            GstMode.ADD -> {
                gst = amount * rate
                totalAmt = amount + gst
            }
            GstMode.REMOVE -> {
                val original = amount / (1 + rate)
                gst = amount - original
                totalAmt = amount
            }
        }
        val half = gst / 2
        return copy(
            total = totalAmt,
            gstAmount = gst,
            cgst = if (transactionType == GstTransactionType.INTRA) half else 0.0,
            sgst = if (transactionType == GstTransactionType.INTRA) half else 0.0,
            igst = if (transactionType == GstTransactionType.INTER) gst else 0.0,
        )
    }
}
