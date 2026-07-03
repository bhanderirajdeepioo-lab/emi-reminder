package com.emireminder.app.data.db.dao

import androidx.room.*
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.domain.model.TransactionCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ParsedTransactionDao {

    /** Returns rowId, or -1 if the sms_id already exists (IGNORE conflict strategy). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: ParsedTransaction): Long

    @Update
    suspend fun update(transaction: ParsedTransaction)

    @Delete
    suspend fun delete(transaction: ParsedTransaction)

    @Query("SELECT * FROM parsed_transactions WHERE sms_id = :smsId LIMIT 1")
    suspend fun getBySmsId(smsId: String): ParsedTransaction?

    @Query("SELECT * FROM parsed_transactions WHERE bank_account_id = :bankAccountId ORDER BY transaction_date DESC")
    fun getByBankAccountId(bankAccountId: String): Flow<List<ParsedTransaction>>

    @Query("SELECT * FROM parsed_transactions WHERE year_month = :yearMonth ORDER BY transaction_date DESC")
    fun getByMonth(yearMonth: String): Flow<List<ParsedTransaction>>

    @Query("SELECT * FROM parsed_transactions WHERE category = :category ORDER BY transaction_date DESC")
    fun getByCategory(category: TransactionCategory): Flow<List<ParsedTransaction>>

    @Query("SELECT * FROM parsed_transactions ORDER BY transaction_date DESC")
    fun getAll(): Flow<List<ParsedTransaction>>

    @Query("SELECT * FROM parsed_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ParsedTransaction?

    @Query("DELETE FROM parsed_transactions WHERE id = :id")
    suspend fun deleteById(id: String)
}
