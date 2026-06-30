package com.emireminder.app.data.db.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: String,         // HOME, CAR, PERSONAL, EDUCATION, OTHER
    val principalAmount: Double,
    val interestRate: Double,
    val tenureMonths: Int,
    val emiAmount: Double,
    val startDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val notes: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
)
