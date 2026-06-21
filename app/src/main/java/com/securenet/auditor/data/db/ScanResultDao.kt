package com.securenet.auditor.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: ScanResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<ScanResultEntity>)

    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAllAsFlow(): Flow<List<ScanResultEntity>>

    @Query("DELETE FROM scan_results WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_results")
    suspend fun deleteAll()

    @Query("UPDATE scan_results SET tag = :tag WHERE id = :id")
    suspend fun updateTag(id: Long, tag: String)
}
