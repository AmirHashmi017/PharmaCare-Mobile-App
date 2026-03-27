package com.example.mediquick

data class Medicine(
    val id: Long = 0,
    val name: String,
    val category: String,
    val price: Double,
    val stock: Int,
    val unit: String,          // tablets, ml, capsules, etc.
    val expiryDate: String,    // yyyy-MM-dd
    val manufacturer: String,
    val minStock: Int = 10     // threshold for low-stock alert
)
