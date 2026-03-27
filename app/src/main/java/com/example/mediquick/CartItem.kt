package com.example.mediquick

data class CartItem(
    val medicine: Medicine,
    var quantity: Int
) {
    val subtotal: Double get() = medicine.price * quantity
}
