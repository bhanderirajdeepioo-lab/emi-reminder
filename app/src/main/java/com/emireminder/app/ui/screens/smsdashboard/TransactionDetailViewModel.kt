package com.emireminder.app.ui.screens.smsdashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emireminder.app.data.db.dao.AutoDetectedEmiDao
import com.emireminder.app.data.db.dao.ParsedTransactionDao
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.domain.model.TransactionCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailUiState(
    val transaction: ParsedTransaction? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val editSheetOpen: Boolean = false,
    val editSaveInProgress: Boolean = false,
    val editSaveError: String? = null,
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionDao: ParsedTransactionDao,
    private val autoDetectedEmiDao: AutoDetectedEmiDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val txn = transactionDao.getById(transactionId)
                _uiState.value = TransactionDetailUiState(transaction = txn, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = TransactionDetailUiState(isLoading = false, error = e.message ?: "Failed to load transaction")
            }
        }
    }

    fun retry() {
        _uiState.value = TransactionDetailUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val txn = transactionDao.getById(transactionId)
                _uiState.value = TransactionDetailUiState(transaction = txn, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = TransactionDetailUiState(isLoading = false, error = e.message ?: "Failed to load transaction")
            }
        }
    }

    fun toggleVerified() {
        val txn = _uiState.value.transaction ?: return
        viewModelScope.launch {
            val updated = txn.copy(userVerified = !txn.userVerified)
            transactionDao.update(updated)
            _uiState.value = _uiState.value.copy(transaction = updated)
        }
    }

    fun openEditSheet() {
        _uiState.value = _uiState.value.copy(editSheetOpen = true, editSaveError = null)
    }

    fun dismissEditSheet() {
        _uiState.value = _uiState.value.copy(editSheetOpen = false, editSaveError = null)
    }

    fun saveNotes(notes: String) {
        val txn = _uiState.value.transaction ?: return
        viewModelScope.launch {
            try {
                val updated = txn.copy(notes = notes.takeIf { it.isNotBlank() })
                transactionDao.update(updated)
                _uiState.value = _uiState.value.copy(transaction = updated)
            } catch (_: Exception) {
                // notes save is best-effort; silently ignore
            }
        }
    }

    fun saveEdit(
        amount: Double,
        category: TransactionCategory,
        subCategory: String?,
        merchantName: String?,
        notes: String?,
        lenderName: String?,
    ) {
        val original = _uiState.value.transaction ?: return
        _uiState.value = _uiState.value.copy(editSaveInProgress = true, editSaveError = null)
        viewModelScope.launch {
            try {
                val updated = original.copy(
                    amount = amount,
                    category = category,
                    subCategory = subCategory?.takeIf { it.isNotBlank() },
                    merchantName = merchantName?.takeIf { it.isNotBlank() },
                    notes = notes?.takeIf { it.isNotBlank() },
                    userVerified = true,
                )
                transactionDao.update(updated)

                if (original.isEmi && !lenderName.isNullOrBlank()) {
                    val emi = autoDetectedEmiDao.getByTransactionId(original.id)
                    if (emi != null && emi.lenderName != lenderName) {
                        autoDetectedEmiDao.update(emi.copy(lenderName = lenderName))
                    }
                }

                _uiState.value = _uiState.value.copy(
                    transaction = updated,
                    editSheetOpen = false,
                    editSaveInProgress = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    editSaveInProgress = false,
                    editSaveError = e.message ?: "Save failed",
                )
            }
        }
    }
}
