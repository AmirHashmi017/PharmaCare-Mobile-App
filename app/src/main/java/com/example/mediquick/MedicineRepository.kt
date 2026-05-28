package com.example.mediquick

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class MedicineRepository(private val medicineDao: MedicineDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val medsCollection = firestore.collection("medicines")

    val allMedicines: Flow<List<Medicine>> = medicineDao.getAllMedicines()
    val lowStockMedicines: Flow<List<Medicine>> = medicineDao.getLowStockMedicines()

    suspend fun insert(medicine: Medicine) {
        val id = medicineDao.insertMedicine(medicine)
        val medWithId = medicine.copy(id = id)
        try {
            medsCollection.document(id.toString()).set(medWithId).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun update(medicine: Medicine) {
        medicineDao.updateMedicine(medicine)
        try {
            medsCollection.document(medicine.id.toString()).set(medicine).await()
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

    suspend fun syncFromRemote() {
        try {
            val snapshot = medsCollection.get().await()
            val remoteMeds = snapshot.toObjects(Medicine::class.java)
            remoteMeds.forEach { medicineDao.insertMedicine(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}