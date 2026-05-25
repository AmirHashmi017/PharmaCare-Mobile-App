package com.example.mediquick

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String = "",
    val customerId: String = "", // UID of the User/Customer
    val date: String = "",
    val total: Double = 0.0,
    val pharmacistId: String? = null, // Track which pharmacist processed it
    val status: String = "PAYMENT_PENDING", // PAYMENT_PENDING, PAYMENT_DONE, IN_SHIPPING, COMPLETED, CANCELLED
    val firebaseId: String? = null
) {
    @Ignore
    var items: List<SaleItem> = emptyList()
}

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val medicineId: Long,
    val medicineName: String,
    val quantity: Int,
    val priceEach: Double
) {
    val subtotal: Double get() = quantity * priceEach
}
