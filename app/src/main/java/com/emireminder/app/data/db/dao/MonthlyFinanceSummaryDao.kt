package com.emireminder.app.data.db.dao

import androidx.room.*
import com.emireminder.app.data.db.entity.MonthlyFinanceSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyFinanceSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: MonthlyFinanceSummary)

    @Update
    suspend fun update(summary: MonthlyFinanceSummary)

    @Query("SELECT * FROM monthly_finance_summaries WHERE year_month = :yearMonth LIMIT 1")
    suspend fun getByYearMonth(yearMonth: String): MonthlyFinanceSummary?

    @Query("SELECT * FROM monthly_finance_summaries ORDER BY year_month DESC")
    fun getAll(): Flow<List<MonthlyFinanceSummary>>

    @Query("DELETE FROM monthly_finance_summaries WHERE year_month = :yearMonth")
    suspend fun deleteByYearMonth(yearMonth: String)

    @Query("DELETE FROM monthly_finance_summaries")
    suspend fun deleteAll()
}
