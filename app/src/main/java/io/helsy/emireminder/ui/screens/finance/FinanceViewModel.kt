package io.helsy.emireminder.ui.screens.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.helsy.emireminder.data.db.entity.Loan
import io.helsy.emireminder.data.db.entity.Reminder
import io.helsy.emireminder.data.repository.LoanRepository
import io.helsy.emireminder.data.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class CalendarDayState(
    val day: Int,
    val reminders: List<Reminder> = emptyList(),
    val status: DayStatus = DayStatus.NONE,
)

enum class DayStatus { NONE, UPCOMING, OVERDUE }

data class MonthlyCalendarState(
    val year: Int,
    val month: Int,         // Calendar.MONTH (0-based)
    val days: List<CalendarDayState>,
    val firstDayOfWeek: Int, // day-of-week (1=Sun) of the 1st of month
    val totalDays: Int,
    val loans: List<Loan>,
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    val activeLoans = loanRepository.getActiveLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Loan>())

    val calendarState = combine(
        loanRepository.getActiveLoans(),
        reminderRepository.getActiveReminders(),
    ) { loans, reminders ->
        buildMonthState(Calendar.getInstance(), reminders, loans)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildEmptyMonth())

    private fun buildMonthState(cal: Calendar, reminders: List<Reminder>, loans: List<Loan>): MonthlyCalendarState {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val todayDay = cal.get(Calendar.DAY_OF_MONTH)

        val firstDay = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        val firstDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK) // 1=Sun
        val totalDays = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)

        val remindersByDay = reminders.groupBy { it.dueDayOfMonth }

        val days = (1..totalDays).map { day ->
            val dayReminders = remindersByDay[day] ?: emptyList()
            val status = when {
                dayReminders.isEmpty() -> DayStatus.NONE
                day < todayDay -> DayStatus.OVERDUE
                else -> DayStatus.UPCOMING
            }
            CalendarDayState(day = day, reminders = dayReminders, status = status)
        }

        return MonthlyCalendarState(
            year = year,
            month = month,
            days = days,
            firstDayOfWeek = firstDayOfWeek,
            totalDays = totalDays,
            loans = loans,
        )
    }

    private fun buildEmptyMonth(): MonthlyCalendarState {
        val cal = Calendar.getInstance()
        return buildMonthState(cal, emptyList(), emptyList())
    }
}
