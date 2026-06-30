package com.emireminder.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_imports")
data class SMSImport(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val senderAddress: String,
    val rawBody: String,
    val detectedLoanName: String = "",
    val detectedEmiAmount: Double = 0.0,
    val detectedDueDate: String = "",
    val receivedAt: Long = System.currentTimeMillis(),
    val isConfirmed: Boolean = false,
    val linkedLoanId: Int? = null,
)
