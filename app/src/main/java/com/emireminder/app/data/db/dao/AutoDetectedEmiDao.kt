package com.emireminder.app.data.db.dao

import androidx.room.*
import com.emireminder.app.data.db.entity.AutoDetectedEmi
import com.emireminder.app.domain.model.EmiStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoDetectedEmiDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(emi: AutoDetectedEmi): Long

    @Update
    suspend fun update(emi: AutoDetectedEmi)

    @Delete
    suspend fun delete(emi: AutoDetectedEmi)

    @Query("SELECT * FROM auto_detected_emis ORDER BY detected_date DESC")
    fun getAll(): Flow<List<AutoDetectedEmi>>

    @Query("SELECT * FROM auto_detected_emis WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AutoDetectedEmi?

    @Query("SELECT * FROM auto_detected_emis WHERE transaction_id = :transactionId LIMIT 1")
    suspend fun getByTransactionId(transactionId: String): AutoDetectedEmi?

    @Query("SELECT * FROM auto_detected_emis WHERE status = :status ORDER BY detected_date DESC")
    fun getByStatus(status: EmiStatus): Flow<List<AutoDetectedEmi>>

    @Query("DELETE FROM auto_detected_emis WHERE id = :id")
    suspend fun deleteById(id: String)
}
