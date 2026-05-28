package com.example.mediquick

import android.util.Log
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
            Log.d("MediQuick", "✅ Medicine saved to Firestore: ${medWithId.name} id=$id")
        } catch (e: Exception) {
            Log.e("MediQuick", "❌ Firestore save failed: ${e.message}")
        }
        return id
    }

    suspend fun update(medicine: Medicine) {
        medicineDao.updateMedicine(medicine)
        try {
            medsCollection.document(medicine.id.toString()).set(toMap(medicine)).await()
            Log.d("MediQuick", "✅ Medicine updated in Firestore: ${medicine.name}")
        } catch (e: Exception) {
            Log.e("MediQuick", "❌ Firestore update failed: ${e.message}")
        }
    }

    suspend fun delete(medicine: Medicine) {
        medicineDao.deleteMedicine(medicine)
        try {
            medsCollection.document(medicine.id.toString()).delete().await()
            Log.d("MediQuick", "✅ Medicine deleted from Firestore: ${medicine.name}")
        } catch (e: Exception) {
            Log.e("MediQuick", "❌ Firestore delete failed: ${e.message}")
        }
    }

    suspend fun getMedicineById(id: Long): Medicine? = medicineDao.getMedicineById(id)

    suspend fun syncFromFirestore() {
        try {
            Log.d("MediQuick", "🔄 Starting medicine sync from Firestore...")
            val snapshot = medsCollection.get().await()
            Log.d("MediQuick", "📦 Firestore returned ${snapshot.documents.size} medicine documents")

            if (snapshot.documents.isEmpty()) {
                Log.w("MediQuick", "⚠️ No medicines found in Firestore collection 'medicines'")
                return
            }

            for (doc in snapshot.documents) {
                Log.d("MediQuick", "📄 Raw doc id=${doc.id} data=${doc.data}")
                try {
                    val rawId = doc.getLong("id")
                    val name  = doc.getString("name")

                    if (name.isNullOrBlank()) {
                        Log.w("MediQuick", "⚠️ Skipping doc ${doc.id} — name is null/blank")
                        continue
                    }

                    val med = Medicine(
                        id            = rawId ?: 0L,
                        name          = name,
                        category      = doc.getString("category") ?: "Others",
                        price         = doc.getDouble("price") ?: 0.0,
                        stock         = (doc.getLong("stock") ?: 0L).toInt(),
                        unit          = doc.getString("unit") ?: "tablets",
                        expiryDate    = doc.getString("expiryDate") ?: "",
                        manufacturer  = doc.getString("manufacturer") ?: "",
                        minStock      = (doc.getLong("minStock") ?: 10L).toInt(),
                        imageUri      = doc.getString("imageUri"),
                        addedBy       = doc.getString("addedBy"),
                        lastUpdatedBy = doc.getString("lastUpdatedBy")
                    )
                    medicineDao.insertMedicine(med)
                    Log.d("MediQuick", "✅ Inserted medicine into Room: ${med.name} id=${med.id}")
                } catch (e: Exception) {
                    Log.e("MediQuick", "❌ Error parsing doc ${doc.id}: ${e.message}")
                }
            }

            val roomCount = medicineDao.getCount()
            Log.d("MediQuick", "✅ Sync complete. Room now has $roomCount medicines")

        } catch (e: Exception) {
            Log.e("MediQuick", "❌ syncFromFirestore FAILED: ${e.message}", e)
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