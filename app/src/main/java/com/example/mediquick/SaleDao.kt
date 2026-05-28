package com.example.mediquick

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItems(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sales WHERE customerId = :userId ORDER BY date DESC")
    fun getSalesByCustomer(userId: String): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE pharmacistId = :userId ORDER BY date DESC")
    fun getSalesByPharmacist(userId: String): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItem(item: SaleItem)

    @Query("UPDATE sales SET status = :status, pharmacistId = :pharmacistId WHERE id = :saleId")
    suspend fun updateStatus(saleId: Long, status: String, pharmacistId: String?)

    @Query("UPDATE sales SET status = 'CANCELLED' WHERE id = :saleId AND status = 'PAYMENT_PENDING'")
    suspend fun cancelOrder(saleId: Long)

    @Query("SELECT * FROM sales WHERE date >= :startDate")
    fun getSalesFromDate(startDate: String): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE pharmacistId = :userId AND date >= :startDate")
    fun getSalesFromDateByPharmacist(userId: String, startDate: String): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE customerId = :userId AND date >= :startDate")
    fun getSalesFromDateByCustomer(userId: String, startDate: String): Flow<List<Sale>>

    @Query("SELECT COUNT(*) FROM sales WHERE pharmacistId = :userId AND status = 'COMPLETED'")
    suspend fun getCompletedCountByPharmacist(userId: String): Int
}