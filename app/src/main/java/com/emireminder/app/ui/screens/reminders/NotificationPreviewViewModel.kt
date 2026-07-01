package com.emireminder.app.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.data.repository.LoanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class NotificationPreviewViewModel @Inject constructor(
    loanRepository: LoanRepository,
) : ViewModel() {

    val nextDueLoan = loanRepository.getActiveLoans()
        .map { loans -> loans.minByOrNull { daysUntilNextDue(it.emiDueDay) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

internal fun daysUntilNextDue(emiDueDay: Int): Int {
    val today = LocalDate.now()
    val dueThisMonth = today.withDayOfMonth(emiDueDay.coerceIn(1, today.lengthOfMonth()))
    return if (!dueThisMonth.isBefore(today)) {
        ChronoUnit.DAYS.between(today, dueThisMonth).toInt()
    } else {
        val nextMonth = today.plusMonths(1)
        ChronoUnit.DAYS.between(today, nextMonth.withDayOfMonth(emiDueDay.coerceIn(1, nextMonth.lengthOfMonth()))).toInt()
    }
}

internal fun nextDueDateLabel(emiDueDay: Int): String {
    val today = LocalDate.now()
    val dueThisMonth = today.withDayOfMonth(emiDueDay.coerceIn(1, today.lengthOfMonth()))
    val due = if (!dueThisMonth.isBefore(today)) dueThisMonth else {
        val next = today.plusMonths(1)
        next.withDayOfMonth(emiDueDay.coerceIn(1, next.lengthOfMonth()))
    }
    val suffix = when (due.dayOfMonth % 10) {
        1 -> if (due.dayOfMonth == 11) "th" else "st"
        2 -> if (due.dayOfMonth == 12) "th" else "nd"
        3 -> if (due.dayOfMonth == 13) "th" else "rd"
        else -> "th"
    }
    val month = due.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "${due.dayOfMonth}$suffix $month"
}
