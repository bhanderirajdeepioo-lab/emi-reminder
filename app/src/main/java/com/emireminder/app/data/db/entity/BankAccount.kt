package com.emireminder.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bank_accounts",
    indices = [Index(value = ["sender_id", "account_last4"], unique = true)]
)
data class BankAccount(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "sender_id")
    val senderId: String,

    @ColumnInfo(name = "bank_name")
    val bankName: String,

    @ColumnInfo(name = "account_last4")
    val accountLast4: String,

    @ColumnInfo(name = "account_label")
    val accountLabel: String? = null,

    @ColumnInfo(name = "label_confirmed", defaultValue = "0")
    val labelConfirmed: Boolean = false,

    @ColumnInfo(name = "first_seen_at")
    val firstSeenAt: Long,

    @ColumnInfo(name = "label_prompted_at")
    val labelPromptedAt: Long? = null,
)
