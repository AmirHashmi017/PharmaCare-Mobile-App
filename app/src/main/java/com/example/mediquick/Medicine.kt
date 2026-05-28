package com.example.mediquick

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val price: Double,
    val stock: Int,
    val unit: String,
    val expiryDate: String,
    val manufacturer: String,
    val minStock: Int = 10,
    val imageUri: String? = null,
    val addedBy: String? = null, // Track pharmacist/accountant
    val lastUpdatedBy: String? = null
)

object MedicineConstants {
    val CATEGORIES = listOf("Analgesic", "Antibiotic", "Rehydration", "Antacid", "Antihistamine", "Anti-inflammatory", "Antidiabetic", "Vitamins", "Others")
    val UNITS = listOf("tablets", "capsules", "sachets", "bottles", "strips", "pcs") // Removed mg and ml as requested
}