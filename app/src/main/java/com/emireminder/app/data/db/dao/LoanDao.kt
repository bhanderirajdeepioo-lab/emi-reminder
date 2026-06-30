package com.emireminder.app.data.db.dao

import androidx.room.*
import com.emireminder.app.data.db.entity.Loan
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE isActive = 1 ORDER BY startDate DESC")
    fun getActiveLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Int): Loan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: Int)
}
