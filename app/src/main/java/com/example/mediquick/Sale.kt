package com.example.mediquick

data class Sale(
    val id: Long = 0,
    val customerName: String,
    val date: String,
    val total: Double,
    val items: List<SaleItem> = emptyList()
)

data class SaleItem(
    val id: Long = 0,
    val saleId: Long,
    val medicineId: Long,
    val medicineName: String,
    val quantity: Int,
    val priceEach: Double
) {
    val subtotal: Double get() = quantity * priceEach
}
