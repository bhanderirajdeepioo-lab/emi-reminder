package com.emireminder.app.data.db.dao

import androidx.room.*
import com.emireminder.app.data.db.entity.BankAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(account: BankAccount): Long

    @Update
    suspend fun update(account: BankAccount)

    @Delete
    suspend fun delete(account: BankAccount)

    @Query("SELECT * FROM bank_accounts ORDER BY bank_name ASC")
    fun getAll(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getById(id: String): BankAccount?

    @Query("SELECT * FROM bank_accounts WHERE sender_id = :senderId AND account_last4 = :last4 LIMIT 1")
    suspend fun getBySenderAndLast4(senderId: String, last4: String): BankAccount?

    @Query("SELECT * FROM bank_accounts WHERE sender_id = :senderId ORDER BY first_seen_at ASC")
    fun getBySenderId(senderId: String): Flow<List<BankAccount>>

    @Query("SELECT COUNT(*) FROM bank_accounts WHERE sender_id = :senderId")
    suspend fun countBySenderId(senderId: String): Int

    @Query("DELETE FROM bank_accounts")
    suspend fun deleteAll()

    /**
     * Returns accounts that need a labelling prompt:
     * - labelPromptedAt is null (prompt not yet shown)
     * - AND another account from the same sender exists (second account at same bank)
     */
    @Query("""
        SELECT * FROM bank_accounts
        WHERE label_prompted_at IS NULL
        AND (SELECT COUNT(*) FROM bank_accounts b2 WHERE b2.sender_id = bank_accounts.sender_id) > 1
    """)
    fun getAccountsNeedingPrompt(): Flow<List<BankAccount>>
}
