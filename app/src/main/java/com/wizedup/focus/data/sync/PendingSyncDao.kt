package com.wizedup.focus.data.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync ORDER BY id ASC LIMIT 100")
    suspend fun oldestBatch(): List<PendingSyncEntity>

    @Insert
    suspend fun insert(row: PendingSyncEntity): Long

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM pending_sync")
    suspend fun count(): Long
}
