package com.emireminder.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_finance_summaries")
data class MonthlyFinanceSummary(
    /** Format: "YYYY-MM", e.g. "2026-07". Primary key and natural partition key. */
    @PrimaryKey
    @ColumnInfo(name = "year_month")
    val yearMonth: String,

    @ColumnInfo(name = "total_income")
    val totalIncome: Double,

    @ColumnInfo(name = "total_emi")
    val totalEmi: Double,

    @ColumnInfo(name = "total_expenses")
    val totalExpenses: Double,

    /** Serialised JSON map: category name → amount. Converted by SmsFinanceConverters. */
    @ColumnInfo(name = "top_categories")
    val topCategories: Map<String, Double>,

    @ColumnInfo(name = "net_savings")
    val netSavings: Double,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
