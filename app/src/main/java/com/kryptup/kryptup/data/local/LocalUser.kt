package com.kryptup.kryptup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class LocalUser(
    @PrimaryKey val uid: String,
    val email: String,
    val displayName: String
)
