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
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val loanId: Int? = null,
    val loanName: String,
    val bankName: String = "",
    val emiAmount: Double,
    val dueDayOfMonth: Int,      // 1–31
    val frequency: String = "MONTHLY",
    val isActive: Boolean = true,
    val notificationEnabled: Boolean = true,
    val notes: String = "",
    val lastTriggeredAt: Long? = null,
)
