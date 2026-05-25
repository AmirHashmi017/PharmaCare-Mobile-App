package com.example.mediquick

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY id DESC")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE category = :category ORDER BY name ASC")
    fun getMedicinesByCategory(category: String): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE (name LIKE '%' || :query || '%') AND (:category = 'All' OR category = :category) ORDER BY id DESC")
    fun searchMedicinesWithCategory(query: String, category: String): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE stock <= minStock")
    fun getLowStockMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Long): Medicine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("SELECT COUNT(*) FROM medicines")
    suspend fun getCount(): Int
}
