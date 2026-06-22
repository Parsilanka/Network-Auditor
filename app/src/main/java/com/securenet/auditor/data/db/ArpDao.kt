package com.securenet.auditor.data.db

import androidx.room.*

@Dao
interface ArpDao {
    @Query("SELECT * FROM arp_records")
    suspend fun getAll(): List<ArpEntity>

    @Query("SELECT * FROM arp_records WHERE ipAddress = :ip")
    suspend fun getByIp(ip: String): ArpEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(arp: ArpEntity)

    @Query("DELETE FROM arp_records")
    suspend fun deleteAll()
}
