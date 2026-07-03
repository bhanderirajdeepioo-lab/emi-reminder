package com.emireminder.app.ui.screens.calculator

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class CibilBand(val label: String, val colorHex: Long, val range: String) {
    POOR("Poor", 0xFFDC2626, "300–549"),
    FAIR("Fair", 0xFFEA580C, "550–649"),
    AVERAGE("Average", 0xFFD97706, "650–699"),
    GOOD("Good", 0xFF64748B, "700–749"),
    VERY_GOOD("Very Good", 0xFF22C55E, "750–799"),
    EXCELLENT("Excellent", 0xFF16A34A, "800–900"),
}

data class CibilUiState(
    val scoreText: String = "",
    val noHistory: Boolean = false,
    val showResults: Boolean = false,
    val band: CibilBand? = null,
    val score: Int? = null,
    val scoreError: String? = null,
    val description: String = "",
    val tips: List<String> = emptyList(),
    val triggerRewardedAd: Boolean = false,
    val rewardedAdWatched: Boolean = false,
)

@HiltViewModel
class CIBILScoreViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CibilUiState())
    val uiState: StateFlow<CibilUiState> = _uiState.asStateFlow()

    fun setScore(text: String) {
        val filtered = text.filter { it.isDigit() }.take(3)
        _uiState.value = filtered.toIntOrNull().let { score ->
            when {
                filtered.isEmpty() -> _uiState.value.copy(
                    scoreText = filtered, score = null, band = null,
                    scoreError = null, description = "", tips = emptyList(),
                    noHistory = false, showResults = false,
                )
                score == null || score < 300 || score > 900 -> _uiState.value.copy(
                    scoreText = filtered, score = null, band = null,
                    scoreError = "CIBIL scores range from 300 to 900. Please enter a valid score.",
                    description = "", tips = emptyList(), noHistory = false, showResults = false,
                )
                else -> _uiState.value.copy(
                    scoreText = filtered, score = score, band = bandFor(score),
                    scoreError = null, description = descriptionFor(score),
                    tips = tipsFor(score), noHistory = false, showResults = false,
                )
            }
        }
    }

    fun calculate() {
        val s = _uiState.value
        if ((s.band != null && s.score != null) || s.noHistory) {
            _uiState.value = s.copy(showResults = true)
        }
    }

    fun reset() {
        _uiState.value = CibilUiState()
    }

    fun onRewardedAdTriggered() {
        _uiState.value = _uiState.value.copy(triggerRewardedAd = true)
    }

    fun onRewardedAdConsumed() {
        _uiState.value = _uiState.value.copy(triggerRewardedAd = false, rewardedAdWatched = true)
    }

    fun setNoHistory(value: Boolean) {
        _uiState.value = if (value) {
            CibilUiState(
                noHistory = true,
                showResults = true,
                description = "You have no credit history yet. Start building credit with a secured credit card or a small loan.",
                tips = listOf(
                    "Apply for a secured credit card against a fixed deposit.",
                    "Pay all bills on time — even utility and phone bills.",
                    "Avoid applying for too many credit products at once.",
                ),
            )
        } else {
            CibilUiState()
        }
    }

    private fun bandFor(score: Int): CibilBand = when (score) {
        in 300..549 -> CibilBand.POOR
        in 550..649 -> CibilBand.FAIR
        in 650..699 -> CibilBand.AVERAGE
        in 700..749 -> CibilBand.GOOD
        in 750..799 -> CibilBand.VERY_GOOD
        else -> CibilBand.EXCELLENT
    }

    private fun descriptionFor(score: Int): String = when {
        score <= 549 -> "High risk profile. Most banks and NBFCs are likely to reject loan applications. Focus on clearing dues and rebuilding credit."
        score <= 649 -> "Difficult to get loans or credit cards. Lenders may approve with high interest rates and strict conditions."
        score <= 699 -> "Some lenders may approve loans with conditions. Improving your score by 50–100 points will open significantly better options."
        score <= 749 -> "Most lenders will approve. You qualify for standard interest rates. Maintain good habits to reach the excellent range."
        score <= 799 -> "Preferred by lenders. You can negotiate better interest rates. Small improvements push you to the excellent tier."
        else -> "Best credit profile. You qualify for the lowest interest rates and fastest loan approvals. Maintain your habits."
    }

    private fun tipsFor(score: Int): List<String> = when {
        score <= 549 -> listOf(
            "Pay all overdue amounts immediately — even partial payments help.",
            "Avoid new credit applications for at least 6 months.",
            "Get a secured credit card against a fixed deposit to rebuild history.",
        )
        score <= 649 -> listOf(
            "Set up auto-pay for all EMIs and credit card bills.",
            "Reduce credit card outstanding to below 30% of your limit.",
            "Check your CIBIL report for errors — dispute inaccuracies promptly.",
        )
        score <= 699 -> listOf(
            "Maintain zero missed payments for at least 12 months.",
            "Don't close your oldest credit card — it anchors your credit age.",
            "Limit new loan applications to avoid hard enquiries.",
        )
        score <= 749 -> listOf(
            "Keep credit card utilization under 30% of total limit.",
            "Diversify your credit mix if you only have one type of loan.",
            "Monitor your credit report for unauthorized enquiries.",
        )
        else -> listOf(
            "Maintain your current repayment habits consistently.",
            "Negotiate better interest rates with your existing lenders.",
            "Review your credit report annually at CIBIL.com.",
        )
    }
}
