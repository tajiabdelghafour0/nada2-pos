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
                _toastMessage.emit("Checkout completed successfully! Total: $${String.format("%.2f", total)}")
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
