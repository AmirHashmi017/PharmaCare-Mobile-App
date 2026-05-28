package com.example.mediquick

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class MedicineRepository(private val medicineDao: MedicineDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val medsCollection = firestore.collection("medicines")

    val allMedicines: Flow<List<Medicine>> = medicineDao.getAllMedicines()
    val lowStockMedicines: Flow<List<Medicine>> = medicineDao.getLowStockMedicines()

    suspend fun insert(medicine: Medicine): Long {
        val id = medicineDao.insertMedicine(medicine)
        val medWithId = medicine.copy(id = id)
        try {
            medsCollection.document(id.toString()).set(toMap(medWithId)).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    suspend fun update(medicine: Medicine) {
        medicineDao.updateMedicine(medicine)
        try {
            medsCollection.document(medicine.id.toString()).set(toMap(medicine)).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun delete(medicine: Medicine) {
        medicineDao.deleteMedicine(medicine)
        try {
            medsCollection.document(medicine.id.toString()).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getMedicineById(id: Long): Medicine? = medicineDao.getMedicineById(id)

    /**
     * Pull all medicines from Firestore into local Room.
     * Called on app startup so the local DB is always populated from Firestore.
     */
    suspend fun syncFromFirestore() {
        try {
            val snapshot = medsCollection.get().await()
            for (doc in snapshot.documents) {
                try {
                    val med = Medicine(
                        id           = (doc.getLong("id") ?: 0L),
                        name         = doc.getString("name") ?: continue,
                        category     = doc.getString("category") ?: "",
                        price        = doc.getDouble("price") ?: 0.0,
                        stock        = (doc.getLong("stock") ?: 0L).toInt(),
                        unit         = doc.getString("unit") ?: "tablets",
                        expiryDate   = doc.getString("expiryDate") ?: "",
                        manufacturer = doc.getString("manufacturer") ?: "",
                        minStock     = (doc.getLong("minStock") ?: 10L).toInt(),
                        imageUri     = doc.getString("imageUri"),
                        addedBy      = doc.getString("addedBy"),
                        lastUpdatedBy = doc.getString("lastUpdatedBy")
                    )
                    medicineDao.insertMedicine(med)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toMap(m: Medicine): Map<String, Any?> = mapOf(
        "id"            to m.id,
        "name"          to m.name,
        "category"      to m.category,
        "price"         to m.price,
        "stock"         to m.stock,
        "unit"          to m.unit,
        "expiryDate"    to m.expiryDate,
        "manufacturer"  to m.manufacturer,
        "minStock"      to m.minStock,
        "imageUri"      to m.imageUri,
        "addedBy"       to m.addedBy,
        "lastUpdatedBy" to m.lastUpdatedBy
    )
}