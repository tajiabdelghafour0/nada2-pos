package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products WHERE stockQuantity < :threshold ORDER BY stockQuantity ASC")
    fun getLowStockProducts(threshold: Double = 5.0): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE expiryDate IS NOT NULL AND expiryDate <= :timestampThreshold AND expiryDate >= :nowTimestamp ORDER BY expiryDate ASC")
    fun getExpiringSoonProducts(nowTimestamp: Long, timestampThreshold: Long): Flow<List<Product>>

    @Query("UPDATE products SET stockQuantity = CASE WHEN (stockQuantity - :amount) < 0 THEN 0.0 ELSE (stockQuantity - :amount) END WHERE id = :productId")
    suspend fun decreaseStock(productId: Long, amount: Double)
}

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItem(item: TransactionItem)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY id ASC")
    fun getTransactionItems(transactionId: Long): Flow<List<TransactionItem>>
}
