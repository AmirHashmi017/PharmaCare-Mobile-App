package com.example.mediquick

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SaleRepository(private val saleDao: SaleDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val salesCollection = firestore.collection("sales")
    private val saleItemsCollection = firestore.collection("sale_items")

    suspend fun insertSaleWithItems(sale: Sale, items: List<CartItem>): Long {
        val saleId = saleDao.insertSale(sale)
        val saleWithId = sale.copy(id = saleId)

        val saleItems = items.map { item ->
            SaleItem(
                saleId       = saleId,
                medicineId   = item.medicine.id,
                medicineName = item.medicine.name,
                quantity     = item.quantity,
                priceEach    = item.medicine.price
            )
        }
        for (saleItem in saleItems) {
            saleDao.insertSaleItem(saleItem)
        }

        try {
            salesCollection.document(saleId.toString()).set(saleToMap(saleWithId)).await()
            for (saleItem in saleItems) {
                saleItemsCollection.add(saleItemToMap(saleItem)).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return saleId
    }

    suspend fun updateStatus(saleId: Long, status: String, pharmacistId: String?) {
        saleDao.updateStatus(saleId, status, pharmacistId)
        try {
            val updates = mutableMapOf<String, Any>("status" to status)
            if (pharmacistId != null) updates["pharmacistId"] = pharmacistId
            salesCollection.document(saleId.toString()).update(updates).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun cancelOrder(saleId: Long) {
        saleDao.cancelOrder(saleId)
        try {
            salesCollection.document(saleId.toString())
                .update(mapOf("status" to "CANCELLED"))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Pull all sales for the current user from Firestore into local Room.
     * Pass null for uid to sync ALL sales (admin use).
     */
    suspend fun syncFromFirestore(uid: String?, role: UserRole) {
        try {
            val query = when (role) {
                UserRole.ADMIN      -> salesCollection.get().await()
                UserRole.PHARMACIST -> salesCollection.whereEqualTo("pharmacistId", uid).get().await()
                UserRole.USER       -> salesCollection.whereEqualTo("customerId", uid).get().await()
            }

            for (doc in query.documents) {
                try {
                    val sale = Sale(
                        id           = doc.getLong("id") ?: 0L,
                        customerName = doc.getString("customerName") ?: "",
                        customerId   = doc.getString("customerId") ?: "",
                        date         = doc.getString("date") ?: "",
                        total        = doc.getDouble("total") ?: 0.0,
                        pharmacistId = doc.getString("pharmacistId"),
                        status       = doc.getString("status") ?: "PAYMENT_PENDING"
                    )
                    if (sale.id > 0) saleDao.insertSale(sale)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saleToMap(s: Sale): Map<String, Any?> = mapOf(
        "id"           to s.id,
        "customerName" to s.customerName,
        "customerId"   to s.customerId,
        "date"         to s.date,
        "total"        to s.total,
        "pharmacistId" to s.pharmacistId,
        "status"       to s.status
    )

    private fun saleItemToMap(i: SaleItem): Map<String, Any?> = mapOf(
        "saleId"       to i.saleId,
        "medicineId"   to i.medicineId,
        "medicineName" to i.medicineName,
        "quantity"     to i.quantity,
        "priceEach"    to i.priceEach
    )
}