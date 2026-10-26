package com.kryptup.kryptup.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocalUser::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // Add DAOs here
}
