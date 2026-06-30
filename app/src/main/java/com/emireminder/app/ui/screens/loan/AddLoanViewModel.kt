package com.emireminder.app.ui.screens.loan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.data.repository.LoanRepository
import com.emireminder.app.domain.model.LoanType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class AddLoanViewModel @Inject constructor(
    private val repository: LoanRepository,
) : ViewModel() {

    var loanName      by mutableStateOf("")
    var selectedType  by mutableStateOf(LoanType.HOME)
    var bankName      by mutableStateOf("")
    var principal     by mutableStateOf("")
    var interestRate  by mutableStateOf("")
    var tenureMonths  by mutableStateOf("")
    var emiOverride   by mutableStateOf("")   // non-empty when user manually edits EMI
    var accountNumber by mutableStateOf("")
    var notes         by mutableStateOf("")
    var dueDayOfMonth by mutableStateOf("")

    val autoEmi: Double
        get() {
            val p = principal.toDoubleOrNull() ?: return 0.0
            val n = tenureMonths.toIntOrNull()?.takeIf { it > 0 } ?: return 0.0
            val annualRate = interestRate.toDoubleOrNull() ?: 0.0
            if (annualRate == 0.0) return p / n
            val r = annualRate / 12.0 / 100.0
            return p * r * (1 + r).pow(n) / ((1 + r).pow(n) - 1)
        }

    val displayEmi: String
        get() = emiOverride.ifBlank {
            val e = autoEmi
            if (e > 0) "%.2f".format(e) else ""
        }

    fun onPrincipalChange(v: String)     { principal = v; emiOverride = "" }
    fun onInterestRateChange(v: String)  { interestRate = v; emiOverride = "" }
    fun onTenureChange(v: String)        { tenureMonths = v; emiOverride = "" }
    fun onEmiOverrideChange(v: String)   { emiOverride = v }

    val isValid: Boolean
        get() = loanName.isNotBlank() &&
                principal.toDoubleOrNull() != null &&
                displayEmi.toDoubleOrNull() != null

    fun save(onSuccess: () -> Unit) {
        if (!isValid) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLoan(
                Loan(
                    name           = loanName.trim(),
                    type           = selectedType.name,
                    bankName       = bankName.trim(),
                    principalAmount = principal.toDouble(),
                    interestRate   = interestRate.toDoubleOrNull() ?: 0.0,
                    tenureMonths   = tenureMonths.toIntOrNull() ?: 0,
                    emiAmount      = displayEmi.toDouble(),
                    accountNumber  = accountNumber.trim(),
                    notes          = notes.trim(),
                )
            )
            launch(Dispatchers.Main) { onSuccess() }
        }
    }
}
