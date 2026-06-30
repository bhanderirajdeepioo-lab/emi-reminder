package io.helsy.emireminder.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
