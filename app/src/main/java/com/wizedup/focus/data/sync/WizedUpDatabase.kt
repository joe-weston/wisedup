package com.wizedup.focus.data.sync

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PendingSyncEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WizedUpDatabase : RoomDatabase() {
    abstract fun pendingSyncDao(): PendingSyncDao
}
