package com.example.mediquick

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class MediQuickApi(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Gets pharmacist stats. Queries Firestore directly so admin sees
     * sales processed on the pharmacist's device (not just admin's local Room).
     */
    suspend fun getPharmacistStats(uid: String, startDate: String): PharmacistReport {
        return try {
            getPharmacistStatsFromFirestore(uid, startDate)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to local Room if offline
            getPharmacistStatsFromRoom(uid, startDate)
        }
    }

    private suspend fun getPharmacistStatsFromFirestore(uid: String, startDate: String): PharmacistReport {
        val snapshot = firestore.collection("sales")
            .whereEqualTo("pharmacistId", uid)
            .whereGreaterThanOrEqualTo("date", startDate)
            .get()
            .await()

        val allSales = snapshot.documents.mapNotNull { doc ->
            try {
                mapOf(
                    "status" to (doc.getString("status") ?: ""),
                    "total"  to (doc.getDouble("total") ?: 0.0)
                )
            } catch (e: Exception) { null }
        }

        val totalOrders     = allSales.size
        val completedSales  = allSales.filter { sale ->
            val s = sale["status"] as String
            s == "COMPLETED" || s == "PAYMENT_DONE" || s == "IN_SHIPPING"
        }
        val completedOrders = completedSales.size
        val revenue         = completedSales.sumOf { it["total"] as Double }
        val efficiency      = if (totalOrders > 0) (completedOrders.toFloat() / totalOrders) * 100f else 0f

        return PharmacistReport(totalOrders, completedOrders, revenue, efficiency)
    }

    private suspend fun getPharmacistStatsFromRoom(uid: String, startDate: String): PharmacistReport {
        val sales = db.saleDao().getSalesFromDateByPharmacist(uid, startDate).first()
        val completedSales = sales.filter {
            it.status == "COMPLETED" || it.status == "PAYMENT_DONE" || it.status == "IN_SHIPPING"
        }
        val totalOrders     = sales.size
        val completedOrders = completedSales.size
        val revenue         = completedSales.sumOf { it.total }
        val efficiency      = if (totalOrders > 0) (completedOrders.toFloat() / totalOrders) * 100f else 0f

        return PharmacistReport(totalOrders, completedOrders, revenue, efficiency)
    }
}

data class PharmacistReport(
    val totalOrders: Int,
    val completedOrders: Int,
    val revenue: Double,
    val efficiency: Float
)