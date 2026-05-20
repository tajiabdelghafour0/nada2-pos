package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories"
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val name: String,
    val categoryId: Long?,
    val price: Double,
    val stockQuantity: Double,
    val expiryDate: Long?,
    val imageUri: String?
)

@Entity(
    tableName = "transactions"
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val totalAmount: Double
)

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val productPrice: Double,
    val quantity: Double,
    val subtotal: Double
)

data class ProductWithCategory(
    val product: Product,
    val category: Category?
)
