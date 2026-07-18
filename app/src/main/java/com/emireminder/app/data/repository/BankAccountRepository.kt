package com.emireminder.app.data.repository

import com.emireminder.app.data.db.dao.BankAccountDao
import com.emireminder.app.data.db.entity.BankAccount
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankAccountRepository @Inject constructor(
    private val bankAccountDao: BankAccountDao,
) {
    /**
     * Finds the BankAccount for (senderId, accountLast4) or creates one.
     * Returns the account ID.
     *
     * Per HEL-570 §Scope: no prompt is triggered here. The prompt is driven by
     * [getAccountsNeedingPrompt] in the SmsDashboard UI, which calls [markPromptShown]
     * when the card first appears.
     */
    suspend fun resolveAccount(
        senderId: String,
        bankName: String,
        accountLast4: String,
    ): String {
        val existing = bankAccountDao.getBySenderAndLast4(senderId, accountLast4)
        if (existing != null) return existing.id

        val id = UUID.randomUUID().toString()
        bankAccountDao.insert(
            BankAccount(
                id = id,
                senderId = senderId,
                bankName = bankName,
                accountLast4 = accountLast4,
                firstSeenAt = System.currentTimeMillis(),
            )
        )
        return id
    }

    /** Accounts where the user needs to be prompted to choose a label (shown once per account). */
    fun getAccountsNeedingPrompt(): Flow<List<BankAccount>> =
        bankAccountDao.getAccountsNeedingPrompt()

    fun getAllAccounts(): Flow<List<BankAccount>> =
        bankAccountDao.getAll()

    suspend fun getById(id: String): BankAccount? =
        bankAccountDao.getById(id)

    /** Call when the prompt card is first rendered — sets label_prompted_at to suppress re-prompt. */
    suspend fun markPromptShown(accountId: String) {
        val account = bankAccountDao.getById(accountId) ?: return
        if (account.labelPromptedAt == null) {
            bankAccountDao.update(account.copy(labelPromptedAt = System.currentTimeMillis()))
        }
    }

    /** Saves the user-chosen label and marks label_confirmed = true. */
    suspend fun applyLabel(accountId: String, label: String) {
        val account = bankAccountDao.getById(accountId) ?: return
        bankAccountDao.update(
            account.copy(
                accountLabel = label.ifBlank { null },
                labelConfirmed = label.isNotBlank(),
                labelPromptedAt = account.labelPromptedAt ?: System.currentTimeMillis(),
            )
        )
    }

    /** User chose Skip — marks prompt as seen but leaves label unconfirmed. */
    suspend fun skipLabel(accountId: String) {
        val account = bankAccountDao.getById(accountId) ?: return
        bankAccountDao.update(
            account.copy(
                labelConfirmed = false,
                labelPromptedAt = account.labelPromptedAt ?: System.currentTimeMillis(),
            )
        )
    }

    suspend fun deleteAll() = bankAccountDao.deleteAll()

    /** Renames an account at any time (accessible from Settings → Finance Accounts). */
    suspend fun renameAccount(accountId: String, label: String) {
        val account = bankAccountDao.getById(accountId) ?: return
        bankAccountDao.update(
            account.copy(
                accountLabel = label.ifBlank { null },
                labelConfirmed = label.isNotBlank(),
            )
        )
    }
}
