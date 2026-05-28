package com.example.mediquick

import android.content.Context
import kotlinx.coroutines.flow.first

class MediQuickApi(context: Context) {
    private val db = AppDatabase.getDatabase(context)

    suspend fun getPharmacistStats(uid: String, startDate: String): PharmacistReport {
        val sales = db.saleDao().getSalesFromDateByPharmacist(uid, startDate).first()
        val completedSales = sales.filter { 
            it.status == "COMPLETED" || it.status == "PAYMENT_DONE" || it.status == "IN_SHIPPING" 
        }
        val totalOrders = sales.size
        val completedOrders = completedSales.size
        val revenue = completedSales.sumOf { it.total }
        val efficiency = if (totalOrders > 0) (completedOrders.toFloat() / totalOrders) * 100 else 0f
        
        return PharmacistReport(totalOrders, completedOrders, revenue, efficiency)
    }
}

data class PharmacistReport(
    val totalOrders: Int,
    val completedOrders: Int,
    val revenue: Double,
    val efficiency: Float
)