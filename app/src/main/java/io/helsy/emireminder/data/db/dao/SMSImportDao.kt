package io.helsy.emireminder.data.db.dao

import androidx.room.*
import io.helsy.emireminder.data.db.entity.SMSImport
import kotlinx.coroutines.flow.Flow

@Dao
interface SMSImportDao {
    @Query("SELECT * FROM sms_imports ORDER BY receivedAt DESC")
    fun getAllSmsImports(): Flow<List<SMSImport>>

    @Query("SELECT * FROM sms_imports WHERE isConfirmed = 0 ORDER BY receivedAt DESC")
    fun getPendingSmsImports(): Flow<List<SMSImport>>

    @Query("SELECT * FROM sms_imports WHERE id = :id")
    suspend fun getSmsImportById(id: Int): SMSImport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsImport(smsImport: SMSImport): Long

    @Update
    suspend fun updateSmsImport(smsImport: SMSImport)

    @Delete
    suspend fun deleteSmsImport(smsImport: SMSImport)
}
