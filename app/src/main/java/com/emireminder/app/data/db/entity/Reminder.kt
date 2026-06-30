package com.emireminder.app.data.db.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Loan::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val loanId: Int,
    val loanName: String,
    val emiAmount: Double,
    val dueDayOfMonth: Int,      // 1–31
    val frequency: String = "MONTHLY",
    val isActive: Boolean = true,
    val notes: String = "",
    val lastTriggeredAt: Long? = null,
)
