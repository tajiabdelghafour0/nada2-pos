package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroceryViewModel(private val repository: GroceryRepository) : ViewModel() {

    // Toast/Alert message channels
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Pre-filled barcode for new products scanned but not found
    private val _scannedBarcodeForNewProduct = MutableStateFlow<String?>(null)
    val scannedBarcodeForNewProduct: StateFlow<String?> = _scannedBarcodeForNewProduct.asStateFlow()

    fun clearScannedBarcodeForNewProduct() {
        _scannedBarcodeForNewProduct.value = null
    }

    // Categories State
    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products State
    val products: StateFlow<List<Product>> = repository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Product search query & results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Product>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.products
            } else {
                repository.searchProducts(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Low Stock Alert
    val lowStockProducts: StateFlow<List<Product>> = repository.getLowStockProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expiring Soon Alert
    val expiringSoonProducts: StateFlow<List<Product>> = repository.getExpiringSoonProducts(daysAhead = 7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent checkout transactions
    val transactions: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Cart
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val cartTotal: StateFlow<Double> = _cartItems
        .map { items -> items.sumOf { it.product.price * it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayRevenue: StateFlow<Double> = repository.transactions
        .map { txList ->
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            txList.filter { it.timestamp >= startOfDay }.sumOf { it.totalAmount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // COOLDOWN map to prevent double-scans of same item within 1.5 seconds
    private val scanCooldowns = mutableMapOf<String, Long>()

    fun onBarcodeScanned(barcode: String, onNavigateToNewProduct: () -> Unit) {
        val now = System.currentTimeMillis()
        val lastScan = scanCooldowns[barcode] ?: 0L
        if (now - lastScan < 1500L) {
            // Rate-limit/debounce scanning of the exact same item
            return
        }
        scanCooldowns[barcode] = now

        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            if (product != null) {
                // Product exists, add to cart!
                addProductToCart(product)
                _toastMessage.emit("Added to Cart: ${product.name}")
            } else {
                // Product not registered
                _scannedBarcodeForNewProduct.value = barcode
                _toastMessage.emit("Unrecognized barcode: $barcode. Click 'Add' in Inventory to register!")
                onNavigateToNewProduct()
            }
        }
    }

    private fun addProductToCart(product: Product) {
        val current = _cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.barcode == product.barcode }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(quantity = existing.quantity + 1.0)
        } else {
            current.add(CartItem(product = product, quantity = 1.0))
        }
        _cartItems.value = current
    }

    fun decreaseQuantityInCart(cartItem: CartItem) {
        val current = _cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == cartItem.product.id }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            if (existing.quantity <= 1.0) {
                current.removeAt(existingIndex)
            } else {
                current[existingIndex] = existing.copy(quantity = existing.quantity - 1.0)
            }
            _cartItems.value = current
        }
    }

    fun updateCartItemQuantity(cartItem: CartItem, newQty: Double) {
        val current = _cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == cartItem.product.id }
        if (existingIndex >= 0) {
            if (newQty <= 0) {
                current.removeAt(existingIndex)
            } else {
                current[existingIndex] = cartItem.copy(quantity = newQty)
            }
            _cartItems.value = current
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun checkoutCart() {
        val items = _cartItems.value
        val total = cartTotal.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            try {
                repository.executeCheckout(total, items)
                _cartItems.value = emptyList()
                _toastMessage.emit("Checkout completed successfully! Total: ${String.format("%.2f", total)} DH")
            } catch (e: Exception) {
                _toastMessage.emit("Checkout failed: ${e.localizedMessage}")
            }
        }
    }

    // Categories CRUD
    fun saveCategory(category: Category) {
        viewModelScope.launch {
            if (category.id == 0L) {
                repository.insertCategory(category)
                _toastMessage.emit("Category '${category.name}' created")
            } else {
                repository.updateCategory(category)
                _toastMessage.emit("Category updated")
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _toastMessage.emit("Category '${category.name}' deleted")
        }
    }

    // Products CRUD
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            if (product.id == 0L) {
                repository.insertProduct(product)
                _toastMessage.emit("Product '${product.name}' created")
            } else {
                repository.updateProduct(product)
                _toastMessage.emit("Product updated")
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _toastMessage.emit("Product '${product.name}' deleted")
        }
    }

    // Duplicate Product utility to clone variants instantly
    fun duplicateProduct(product: Product) {
        viewModelScope.launch {
            val randomSuffix = (1000..9999).random().toString()
            val duplicated = Product(
                id = 0L,
                barcode = "${product.barcode}_copy_$randomSuffix",
                name = "${product.name} (Copy)",
                categoryId = product.categoryId,
                price = product.price,
                stockQuantity = product.stockQuantity,
                expiryDate = product.expiryDate,
                imageUri = product.imageUri
            )
            repository.insertProduct(duplicated)
            _toastMessage.emit("Cloned product '${product.name}' into draft duplicate!")
        }
    }

    // Export completely offline data into standard text-sharing JSON backup
    fun exportBackupAsJson(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject()

                // Categories
                val cats = repository.categories.first()
                val catsArray = org.json.JSONArray()
                cats.forEach { cat ->
                    val obj = org.json.JSONObject().apply {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("colorHex", cat.colorHex)
                    }
                    catsArray.put(obj)
                }
                root.put("categories", catsArray)

                // Products
                val prods = repository.products.first()
                val prodsArray = org.json.JSONArray()
                prods.forEach { prod ->
                    val obj = org.json.JSONObject().apply {
                        put("id", prod.id)
                        put("barcode", prod.barcode)
                        put("name", prod.name)
                        put("categoryId", prod.categoryId ?: org.json.JSONObject.NULL)
                        put("price", prod.price)
                        put("stockQuantity", prod.stockQuantity)
                        put("expiryDate", prod.expiryDate ?: org.json.JSONObject.NULL)
                        put("imageUri", prod.imageUri ?: org.json.JSONObject.NULL)
                    }
                    prodsArray.put(obj)
                }
                root.put("products", prodsArray)

                // Transactions
                val txs = repository.transactions.first()
                val txsArray = org.json.JSONArray()
                txs.forEach { tx ->
                    val obj = org.json.JSONObject().apply {
                        put("id", tx.id)
                        put("timestamp", tx.timestamp)
                        put("totalAmount", tx.totalAmount)
                    }
                    txsArray.put(obj)
                }
                root.put("transactions", txsArray)

                // Transaction Items
                val items = repository.allTransactionItems.first()
                val itemsArray = org.json.JSONArray()
                items.forEach { item ->
                    val obj = org.json.JSONObject().apply {
                        put("id", item.id)
                        put("transactionId", item.transactionId)
                        put("productId", item.productId)
                        put("productName", item.productName)
                        put("productPrice", item.productPrice)
                        put("quantity", item.quantity)
                        put("subtotal", item.subtotal)
                    }
                    itemsArray.put(obj)
                }
                root.put("transactionItems", itemsArray)

                val jsonString = root.toString(4)

                // Copy to Clipboard as a convenient backup fallback
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("NADA2_Backup", jsonString)
                clipboard.setPrimaryClip(clip)

                // Open system text share sheet to let users send backup string to whatsapp, email, drive, etc.
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "NADA 2 Backup JSON")
                    putExtra(android.content.Intent.EXTRA_TEXT, jsonString)
                }

                val chooser = android.content.Intent.createChooser(intent, "Share NADA 2 Backup JSON File")
                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                _toastMessage.emit("Database backup copied to clipboard & share sheet launched!")
            } catch (e: Exception) {
                _toastMessage.emit("Backup export failed: ${e.localizedMessage}")
            }
        }
    }

    // Import from JSON text backup
    fun importBackupFromJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(jsonString)

                val catsList = mutableListOf<Category>()
                val catsArray = root.optJSONArray("categories") ?: org.json.JSONArray()
                for (i in 0 until catsArray.length()) {
                    val obj = catsArray.getJSONObject(i)
                    catsList.add(Category(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        colorHex = obj.getString("colorHex")
                    ))
                }

                val prodsList = mutableListOf<Product>()
                val prodsArray = root.optJSONArray("products") ?: org.json.JSONArray()
                for (i in 0 until prodsArray.length()) {
                    val obj = prodsArray.getJSONObject(i)
                    prodsList.add(Product(
                        id = obj.getLong("id"),
                        barcode = obj.getString("barcode"),
                        name = obj.getString("name"),
                        categoryId = if (obj.isNull("categoryId")) null else obj.getLong("categoryId"),
                        price = obj.getDouble("price"),
                        stockQuantity = obj.getDouble("stockQuantity"),
                        expiryDate = if (obj.isNull("expiryDate")) null else obj.getLong("expiryDate"),
                        imageUri = if (obj.isNull("imageUri")) null else obj.getString("imageUri")
                    ))
                }

                val txsList = mutableListOf<Transaction>()
                val txsArray = root.optJSONArray("transactions") ?: org.json.JSONArray()
                for (i in 0 until txsArray.length()) {
                    val obj = txsArray.getJSONObject(i)
                    txsList.add(Transaction(
                        id = obj.getLong("id"),
                        timestamp = obj.getLong("timestamp"),
                        totalAmount = obj.getDouble("totalAmount")
                    ))
                }

                val itemsList = mutableListOf<TransactionItem>()
                val itemsArray = root.optJSONArray("transactionItems") ?: org.json.JSONArray()
                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.getJSONObject(i)
                    itemsList.add(TransactionItem(
                        id = obj.getLong("id"),
                        transactionId = obj.getLong("transactionId"),
                        productId = obj.getLong("productId"),
                        productName = obj.getString("productName"),
                        productPrice = obj.getDouble("productPrice"),
                        quantity = obj.getDouble("quantity"),
                        subtotal = obj.getDouble("subtotal")
                    ))
                }

                repository.restoreDatabase(catsList, prodsList, txsList, itemsList)
                _toastMessage.emit("Backup imported! Restored ${prodsList.size} products, ${catsList.size} categories and ${txsList.size} sales records.")
            } catch (e: Exception) {
                _toastMessage.emit("Import failed: JSON data is malformed or invalid. Details: ${e.localizedMessage}")
            }
        }
    }
}

class GroceryViewModelFactory(private val repository: GroceryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroceryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroceryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
