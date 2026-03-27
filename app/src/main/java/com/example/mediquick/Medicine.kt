package com.example.mediquick

data class Medicine(
    val id: Long = 0,
    val name: String,
    val category: String,
    val price: Double,
    val stock: Int,
    val unit: String,
    val expiryDate: String,
    val manufacturer: String,
    val minStock: Int = 10
)
