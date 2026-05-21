package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

data class CartItem(
    val product: Product,
    var quantity: Double
)

class GroceryRepository(private val database: AppDatabase) {
    private val categoryDao = database.categoryDao()
    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()

    val categories: Flow<List<Category>> = categoryDao.getAllCategories()
    val products: Flow<List<Product>> = productDao.getAllProducts()

    fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.searchProducts(query)
    }

    suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    fun getLowStockProducts(threshold: Double = 5.0): Flow<List<Product>> {
        return productDao.getLowStockProducts(threshold)
    }

    fun getExpiringSoonProducts(daysAhead: Int = 7): Flow<List<Product>> {
        val now = System.currentTimeMillis()
        val msInDay = 24 * 60 * 60 * 1000L
        val threshold = now + (daysAhead * msInDay)
        return productDao.getExpiringSoonProducts(now, threshold)
    }

    val transactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allTransactionItems: Flow<List<TransactionItem>> = transactionDao.getAllTransactionItems()

    suspend fun restoreDatabase(
        cats: List<Category>,
        prods: List<Product>,
        txs: List<Transaction>,
        items: List<TransactionItem>
    ) {
        database.withTransaction {
            database.clearAllTables()
            for (cat in cats) {
                categoryDao.insertCategory(cat)
            }
            for (prod in prods) {
                productDao.insertProduct(prod)
            }
            for (tx in txs) {
                transactionDao.insertTransactionRaw(tx)
            }
            for (item in items) {
                transactionDao.insertTransactionItem(item)
            }
        }
    }

    suspend fun executeCheckout(totalAmount: Double, cartItems: List<CartItem>) {
        database.withTransaction {
            val transaction = Transaction(
                timestamp = System.currentTimeMillis(),
                totalAmount = totalAmount
            )
            val transactionId = transactionDao.insertTransaction(transaction)
            for (cartItem in cartItems) {
                val item = TransactionItem(
                    transactionId = transactionId,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    productPrice = cartItem.product.price,
                    quantity = cartItem.quantity,
                    subtotal = cartItem.product.price * cartItem.quantity
                )
                transactionDao.insertTransactionItem(item)
                productDao.decreaseStock(cartItem.product.id, cartItem.quantity)
            }
        }
    }
}
