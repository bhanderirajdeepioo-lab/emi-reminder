package com.emireminder.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emireminder.app.domain.model.TransactionCategory
import com.emireminder.app.domain.model.TransactionDirection

@Entity(
    tableName = "parsed_transactions",
    indices = [
        Index(value = ["sms_id"], unique = true),
        Index(value = ["bank_account_id"]),
        Index(value = ["year_month"]),
        Index(value = ["category"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = BankAccount::class,
            parentColumns = ["id"],
            childColumns = ["bank_account_id"],
            onDelete = ForeignKey.RESTRICT,
        )
    ]
)
data class ParsedTransaction(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "sms_id")
    val smsId: String,

    @ColumnInfo(name = "raw_sms_body")
    val rawSmsBody: String? = null,

    @ColumnInfo(name = "parsed_at")
    val parsedAt: Long,

    @ColumnInfo(name = "transaction_date")
    val transactionDate: Long,

    /** Derived "YYYY-MM" string stored for fast month-bucket queries. */
    @ColumnInfo(name = "year_month")
    val yearMonth: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "currency", defaultValue = "INR")
    val currency: String = "INR",

    @ColumnInfo(name = "direction")
    val direction: TransactionDirection,

    @ColumnInfo(name = "category")
    val category: TransactionCategory,

    @ColumnInfo(name = "sub_category")
    val subCategory: String? = null,

    @ColumnInfo(name = "sender_id")
    val senderId: String,

    @ColumnInfo(name = "bank_name")
    val bankName: String,

    @ColumnInfo(name = "account_last4")
    val accountLast4: String,

    @ColumnInfo(name = "bank_account_id")
    val bankAccountId: String,

    @ColumnInfo(name = "merchant_name")
    val merchantName: String? = null,

    @ColumnInfo(name = "vpa")
    val vpa: String? = null,

    @ColumnInfo(name = "utr_ref")
    val utrRef: String? = null,

    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Int,

    @ColumnInfo(name = "is_emi", defaultValue = "0")
    val isEmi: Boolean = false,

    @ColumnInfo(name = "loan_account_last4")
    val loanAccountLast4: String? = null,

    @ColumnInfo(name = "user_verified", defaultValue = "0")
    val userVerified: Boolean = false,

    @ColumnInfo(name = "notes")
    val notes: String? = null,
)
