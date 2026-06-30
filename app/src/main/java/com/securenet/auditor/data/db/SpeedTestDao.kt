package com.securenet.auditor.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedTestDao {
    @Query("SELECT * FROM speed_test_results ORDER BY timestamp DESC LIMIT 20")
    fun getLastResults(): Flow<List<SpeedTestEntity>>

    @Insert
    suspend fun insert(result: SpeedTestEntity)

    @Query("DELETE FROM speed_test_results WHERE id NOT IN (SELECT id FROM speed_test_results ORDER BY timestamp DESC LIMIT 20)")
    suspend fun trimHistory()

    @Query("DELETE FROM speed_test_results")
    suspend fun deleteAll()

    @Query("DELETE FROM speed_test_results WHERE id = :id")
    suspend fun deleteById(id: Long)
}
