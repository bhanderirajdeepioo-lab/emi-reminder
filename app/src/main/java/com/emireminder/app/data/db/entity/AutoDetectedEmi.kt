package com.emireminder.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emireminder.app.domain.model.EmiStatus

@Entity(
    tableName = "auto_detected_emis",
    indices = [
        Index(value = ["transaction_id"]),
        Index(value = ["linked_reminder_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ParsedTransaction::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Reminder::class,
            parentColumns = ["id"],
            childColumns = ["linked_reminder_id"],
            onDelete = ForeignKey.SET_NULL,
        )
    ]
)
data class AutoDetectedEmi(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "transaction_id")
    val transactionId: String,

    @ColumnInfo(name = "lender_name")
    val lenderName: String,

    @ColumnInfo(name = "loan_account_last4")
    val loanAccountLast4: String,

    @ColumnInfo(name = "emi_amount")
    val emiAmount: Double,

    @ColumnInfo(name = "detected_date")
    val detectedDate: Long,

    @ColumnInfo(name = "recurring_day")
    val recurringDay: Int,

    @ColumnInfo(name = "linked_reminder_id")
    val linkedReminderId: Int? = null,

    @ColumnInfo(name = "status")
    val status: EmiStatus = EmiStatus.PENDING_CONFIRM,

    @ColumnInfo(name = "confirmation_at")
    val confirmationAt: Long? = null,
)
