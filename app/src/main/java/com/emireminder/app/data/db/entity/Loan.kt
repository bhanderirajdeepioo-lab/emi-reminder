package com.emireminder.app.data.db.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.emireminder.app.domain.model.LoanType
import com.emireminder.app.domain.model.toLoanType

@Immutable
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: String,         // stored as enum name, e.g. "HOME"; safe to read via loanType
    val principalAmount: Double,
    val interestRate: Double,
    val tenureMonths: Int,
    val emiAmount: Double,
    val startDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val notes: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
) {
    @get:Ignore
    val loanType: LoanType get() = type.toLoanType()
}
