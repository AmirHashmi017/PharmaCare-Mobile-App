package com.example.mediquick

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    PHARMACIST,
    USER
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.USER,
    val pharmacyName: String? = null,
    val phone: String? = null,
    val addedByAdmin: Boolean = false
)
