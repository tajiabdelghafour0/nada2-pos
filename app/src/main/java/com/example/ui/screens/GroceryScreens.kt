package com.example.ui.screens

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.data.CartItem
import com.example.data.Category
import com.example.data.Product
import com.example.ui.GroceryViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

enum class GroceryTab(val label: String, val iconSelected: ImageVector, val iconUnselected: ImageVector) {
    CHECKOUT("Checkout", Icons.Default.QrCodeScanner, Icons.Outlined.QrCodeScanner),
    INVENTORY("Inventory", Icons.Default.Inventory, Icons.Outlined.Inventory2),
    CATEGORIES("Categories", Icons.Default.Category, Icons.Outlined.Category),
    ALERTS("Alerts", Icons.Default.NotificationsActive, Icons.Outlined.Notifications)
}

// Preset Category Colors for circular chip selector
val presetColors = listOf(
    "#2E7D32", // Forest Green
    "#C62828", // Deep Red
    "#1565C0", // Sky Blue
    "#EF6C00", // Bright Orange
    "#6A1B9A", // Royal Purple
    "#37474F", // Dark Slate Blue
    "#00838F", // Teal Sky
    "#AD1457"  // Crimson Pink
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryApp(viewModel: GroceryViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(GroceryTab.CHECKOUT) }
    var showBackupSettingsDialog by remember { mutableStateOf(false) }
    
    val lowStockCount by viewModel.lowStockProducts.collectAsState()
    val expiringCount by viewModel.expiringSoonProducts.collectAsState()
    val badgeCount = lowStockCount.size + expiringCount.size

    // Simple toast message observer
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    if (showBackupSettingsDialog) {
        BackupSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showBackupSettingsDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sleek Green Avatar logo matching 'N2' in HTML
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "N2",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        
                        Column {
                            Text(
                                text = "NADA 2",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Retail Active Tracker",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Sleek Violet Notification Bell Button with Badge (Navigates to Alerts)
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { currentTab = GroceryTab.ALERTS },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        if (badgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-6).dp, y = 6.dp)
                            )
                        }
                    }

                    // Sleek Violet User Profile Button (Opens Backup Settings)
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { showBackupSettingsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Profile",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                GroceryTab.values().forEach { tab ->
                    val selected = currentTab == tab
                    val labelText = tab.label
                    
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        label = { Text(labelText) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (tab == GroceryTab.ALERTS && badgeCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ) {
                                            Text(badgeCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selected) tab.iconSelected else tab.iconUnselected,
                                    contentDescription = labelText
                                )
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                GroceryTab.CHECKOUT -> CheckoutScreen(viewModel = viewModel, onNavigateToInventory = { currentTab = GroceryTab.INVENTORY })
                GroceryTab.INVENTORY -> InventoryScreen(viewModel = viewModel)
                GroceryTab.CATEGORIES -> CategoriesScreen(viewModel = viewModel)
                GroceryTab.ALERTS -> AlertsDashboard(viewModel = viewModel)
            }
        }
    }
}

// ==========================================
// 1. CHECKOUT & SCANNER (TAB 1)
// ==========================================
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CheckoutScreen(
    viewModel: GroceryViewModel,
    onNavigateToInventory: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    // Simulate typable barcode/scans to let users test barcode scanning off-device easily
    var simulatedBarcode by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP HALF: SCANNER OR PREVIEW FEED (320dp Prominent Height)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1C1B1F) // Deep black backdrop
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                var isTorchOn by remember { mutableStateOf(false) }
                val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current

                if (cameraPermissionState.status.isGranted) {
                    CameraScannerView(
                        modifier = Modifier.fillMaxSize(),
                        isTorchOn = isTorchOn,
                        onBarcodeScanned = { barcode ->
                            // Audio Feedback (Quick Tone Beep)
                            try {
                                val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                                toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 120)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            
                            // Haptic Feedback
                            try {
                                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            viewModel.onBarcodeScanned(barcode, onNavigateToInventory)
                        }
                    )

                    // Flashlight / Torch Toggle Button (High Contrast Top Left)
                    IconButton(
                        onClick = { isTorchOn = !isTorchOn },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .size(46.dp)
                            .background(
                                color = if (isTorchOn) Color(0xFFFFEB3B) else Color.Black.copy(alpha = 0.65f),
                                shape = CircleShape
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Toggle Flashlight",
                            tint = if (isTorchOn) Color.Black else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    // Sleek Neon Green Rounded Sight Box
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(220.dp, 110.dp)
                            .border(
                                width = 2.dp,
                                color = Color(0xFF00FF00), // Neon Green
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulsing Red Laser Sight Line
                        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                            val midY = size.height / 2
                            // Draw glow
                            drawLine(
                                color = Color(0x7FFF0000),
                                start = androidx.compose.ui.geometry.Offset(8f, midY),
                                end = androidx.compose.ui.geometry.Offset(size.width - 8f, midY),
                                strokeWidth = 8f
                            )
                            // Draw core laser
                            drawLine(
                                color = Color(0xFFFF0000), // Sharp Red
                                start = androidx.compose.ui.geometry.Offset(12f, midY),
                                end = androidx.compose.ui.geometry.Offset(size.width - 12f, midY),
                                strokeWidth = 3f
                            )
                        }
                    }

                    // Bottom "Active Scanner" Pill matching HTML
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.62f), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ACTIVE SCANNER",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // No permission state UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "No Camera",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera Access Required",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Allow camera access or use the manual simulated simulation scanner at the top right to register items.",
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Grant Permission", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Barcode simulated typing toolbox (Sleek Theme Rounded)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.75f),
                            shape = CircleShape
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.15f),
                            CircleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Simulate",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.width(76.dp)) {
                        BasicTextFieldNoLabel(
                            value = simulatedBarcode,
                            onValueChange = { simulatedBarcode = it },
                            placeholderText = "Barcode...",
                            keyboardType = KeyboardType.Text
                        )
                    }
                    IconButton(
                        onClick = {
                            if (simulatedBarcode.isNotBlank()) {
                                viewModel.onBarcodeScanned(simulatedBarcode, onNavigateToInventory)
                                simulatedBarcode = ""
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Submit",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // BOTTOM HALF: CART LISTING
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Sleek Header with Active Cart Title and Clear All link matching HTML
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE CART (${cartItems.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (cartItems.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearCart() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Clear All",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (cartItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBasket,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Shopping Cart is Empty",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scan products above or type barcodes to calculate totals.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(cartItems) { item ->
                            CartListItemRow(item = item, viewModel = viewModel)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )

                    // Totals Panel matching the HTML details block precisely
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "ITEMS COUNT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val totalQty = cartItems.sumOf { it.quantity }
                            val scannedCountStr = if (totalQty % 1.0 == 0.0) "${totalQty.toInt()} Scanned" else "${totalQty} Scanned"
                            Text(
                                text = scannedCountStr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL AMOUNT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.2f", cartTotal)} DH",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF006D3A) // Professional Emerald Green
                            )
                        }
                    }

                    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current

                    // Large Sleek Primary Full-Width Checkout Button
                    Button(
                        onClick = {
                            try {
                                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            viewModel.checkoutCart()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Complete Checkout",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Complete",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartListItemRow(item: CartItem, viewModel: GroceryViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(
                imageUri = item.product.imageUri,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${String.format("%.2f", item.product.price)} DH each",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Subtotal: ${String.format("%.2f", item.product.price * item.quantity)} DH",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Beautiful, customized Outlined Adjuster buttons matching HTML precisely!
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Outlined '-' Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clip(CircleShape)
                        .clickable { viewModel.decreaseQuantityInCart(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Dec quantity",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.widthIn(min = 20.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Outlined '+' Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clip(CircleShape)
                        .clickable { viewModel.updateCartItemQuantity(item, item.quantity + 1.0) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Inc quantity",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Custom simple basic text field with placeholder
@Composable
fun BasicTextFieldNoLabel(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    keyboardType: KeyboardType
) {
    Box(contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) {
            Text(placeholderText, color = Color.Gray, fontSize = 12.sp)
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==========================================
// 2. INVENTORY & PRODUCT MANAGER (TAB 2)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: GroceryViewModel) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val products by viewModel.searchResults.collectAsState()
    val categories by viewModel.categories.collectAsState()
    
    // Category chips selection state (null = All)
    var selectedCategoryFilter by remember { mutableStateOf<Category?>(null) }
    
    // Add & Edit Product forms variables
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Product?>(null) }

    // Read barcode passed from scanned but unrecognized items (Legendary UX!)
    val scannedBarcodeForNewProduct by viewModel.scannedBarcodeForNewProduct.collectAsState()
    
    LaunchedEffect(scannedBarcodeForNewProduct) {
        if (scannedBarcodeForNewProduct != null) {
            showAddDialog = true
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(horizontal = 8.dp)
        ) {
            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                placeholder = { Text("Search by product name or barcode...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // CATEGORIES FILTER BADGES
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .horizontalScrollCompose(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("All Products") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryFilter?.id == category.id,
                        onClick = { selectedCategoryFilter = category },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(category.colorHex)))
                            )
                        }
                    )
                }
            }

            // PRODUCT LIST
            val filteredProducts = remember(products, selectedCategoryFilter) {
                if (selectedCategoryFilter == null) products
                else products.filter { it.categoryId == selectedCategoryFilter?.id }
            }

            if (filteredProducts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NoFood,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Products in Catalog",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Click the '+' button below to add your first grocery item!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredProducts) { product ->
                        val productCategory = categories.firstOrNull { it.id == product.categoryId }
                        ProductInventoryRow(
                            product = product,
                            category = productCategory,
                            onDuplicate = { viewModel.duplicateProduct(product) },
                            onEdit = { showEditDialog = product },
                            onDelete = { viewModel.deleteProduct(product) }
                        )
                    }
                }
            }
        }

        // ADD DIALOG
        if (showAddDialog) {
            AddEditProductDialog(
                editingProduct = null,
                categories = categories,
                initialBarcode = scannedBarcodeForNewProduct,
                onDismiss = {
                    showAddDialog = false
                    viewModel.clearScannedBarcodeForNewProduct()
                },
                onSave = { product ->
                    viewModel.saveProduct(product)
                    showAddDialog = false
                    viewModel.clearScannedBarcodeForNewProduct()
                }
            )
        }

        // EDIT DIALOG
        if (showEditDialog != null) {
            AddEditProductDialog(
                editingProduct = showEditDialog,
                categories = categories,
                initialBarcode = null,
                onDismiss = { showEditDialog = null },
                onSave = { product ->
                    viewModel.saveProduct(product)
                    showEditDialog = null
                }
            )
        }
    }
}

// Helper scrollable modifier function
@Composable
fun Modifier.horizontalScrollCompose(): Modifier = this.then(
    Modifier.clickable(enabled = false) {}
        .horizontalScrollComposeActual()
)

@Composable
fun Modifier.horizontalScrollComposeActual(): Modifier {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    return this.then(Modifier.horizontalScroll(scrollState))
}

@Composable
fun ProductInventoryRow(
    product: Product,
    category: Category?,
    onDuplicate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val lowStockThreshold = 5.0
    val isLowStock = product.stockQuantity < lowStockThreshold
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(
                imageUri = product.imageUri,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (category != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(android.graphics.Color.parseColor(category.colorHex)).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = category.name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(android.graphics.Color.parseColor(category.colorHex))
                            )
                        }
                    }
                }

                Text(
                    text = "Barcode: ${product.barcode}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${String.format("%.2f", product.price)} DH",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isLowStock) Color(0xFFE65100).copy(alpha = 0.15f) else Color(0xFF1B5E20).copy(alpha = 0.12f),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Stock: ${product.stockQuantity}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLowStock) Color(0xFFD84315) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (product.expiryDate != null) {
                    val daysRemaining = remember(product.expiryDate) {
                        val diff = product.expiryDate - System.currentTimeMillis()
                        (diff / (24 * 60 * 60 * 1000L)).toInt()
                    }
                    val formattedDate = remember(product.expiryDate) {
                        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(product.expiryDate))
                    }
                    Text(
                        text = if (daysRemaining < 0) "Expired ($formattedDate)" else "Expires: $formattedDate (in $daysRemaining days)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (daysRemaining <= 7) Color.Red else Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Actions dropdown
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate (Variant)", tint = Color(0xFF2E7D32))
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// Helper thumbnail loader that falls back gracefully
@Composable
fun ProductThumbnail(imageUri: String?, modifier: Modifier = Modifier) {
    if (imageUri != null && File(imageUri).exists()) {
        Image(
            painter = rememberAsyncImagePainter(model = File(imageUri)),
            contentDescription = "Product picture",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fastfood, // Fallback retail symbol
                contentDescription = "Placeholder",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
    }
}

// ADD/EDIT FORM
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    editingProduct: Product?,
    categories: List<Category>,
    initialBarcode: String?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(editingProduct?.name ?: "") }
    var barcode by remember { mutableStateOf(editingProduct?.barcode ?: initialBarcode ?: "") }
    var priceStr by remember { mutableStateOf(editingProduct?.price?.toString() ?: "") }
    var stockStr by remember { mutableStateOf(editingProduct?.stockQuantity?.toString() ?: "") }
    var expiryDateLong by remember { mutableStateOf(editingProduct?.expiryDate) }
    var selectedCategoryId by remember { mutableStateOf(editingProduct?.categoryId) }
    var localImagePath by remember { mutableStateOf(editingProduct?.imageUri) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var activeScannerInDialog by remember { mutableStateOf(false) }

    // Camera launcher contract for photo capturing
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                // Save captured thumbnail locally directly inside the private cache folder
                val cacheFile = File(context.cacheDir, "prod_${System.currentTimeMillis()}.png")
                FileOutputStream(cacheFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                localImagePath = cacheFile.absolutePath
                Toast.makeText(context, "Product photo captured!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save photo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScrollComposeActual()
            ) {
                Text(
                    text = if (editingProduct == null) "Add Grocery Product" else "Edit Grocery Product",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // IMAGE PICKER BLOCK
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp), RoundedCornerShape(8.dp))
                        .clickable {
                            if (cameraPermission.status.isGranted) {
                                cameraLauncher.launch()
                            } else {
                                cameraPermission.launchPermissionRequest()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (localImagePath != null && File(localImagePath!!).exists()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = File(localImagePath!!)),
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, "Change", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, "Take Photo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Take Product Snapshot", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Barcode scanner trigger inside the form dialog itself! (Extremely production-ready)
                if (activeScannerInDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    ) {
                        CameraScannerView(
                            modifier = Modifier.fillMaxSize(),
                            isTorchOn = false,
                            onBarcodeScanned = { str ->
                                barcode = str
                                activeScannerInDialog = false
                                Toast.makeText(context, "Barcode scanned: $str", Toast.LENGTH_SHORT).show()
                            }
                        )
                        IconButton(
                            onClick = { activeScannerInDialog = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Barcode input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (cameraPermission.status.isGranted) {
                                activeScannerInDialog = true
                            } else {
                                cameraPermission.launchPermissionRequest()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, "Scan scanner dialog")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Product Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Price and stock fields row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (DH)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stock (allows dec)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date Picker for Expiry Date
                val calendar = Calendar.getInstance()
                val dateDialog = DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        calendar.set(year, month, day)
                        expiryDateLong = calendar.timeInMillis
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dateDialog.show() }
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val expText = if (expiryDateLong != null) {
                        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(expiryDateLong!!))
                    } else {
                        "Set Expiry Date (Optional)"
                    }
                    Text(expText, style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Category Selection Dropdown
                val selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Select Category"
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(cat.colorHex)))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(cat.name)
                                    }
                                },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Control panel buttons inside dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL")
                    }

                    Button(
                        onClick = {
                            val price = priceStr.toDoubleOrNull() ?: 0.0
                            val stock = stockStr.toDoubleOrNull() ?: 0.0
                            if (barcode.isBlank() || name.isBlank() || price <= 0.0) {
                                Toast.makeText(context, "Please configure valid Barcode, Name, and Price!", Toast.LENGTH_SHORT).show()
                            } else {
                                val savedProduct = Product(
                                    id = editingProduct?.id ?: 0L,
                                    barcode = barcode,
                                    name = name,
                                    categoryId = selectedCategoryId,
                                    price = price,
                                    stockQuantity = stock,
                                    expiryDate = expiryDateLong,
                                    imageUri = localImagePath
                                )
                                onSave(savedProduct)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SAVE")
                    }
                }
            }
        }
    }
}

// Special custom basic scroll builder helper to bypass nested state exceptions
@Composable
fun Modifier.verticalScrollComposeActual(): Modifier {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    return this.then(Modifier.verticalScroll(scrollState))
}

// ==========================================
// 3. CATEGORIES MANAGER (TAB 3)
// ==========================================
@Composable
fun CategoriesScreen(viewModel: GroceryViewModel) {
    val categories by viewModel.categories.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCategoryDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            Text(
                text = "Product Categories CRUD Manager",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "Add, edit, and custom-color categories (e.g. Dairy, Spices, Bakery).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (categories.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Empty Category",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No categories created yet", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Add categories to group your products nicely inside the scanner", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(categories) { category ->
                        CategoryItemRow(
                            category = category,
                            onEdit = { editingCategory = category },
                            onDelete = { viewModel.deleteCategory(category) }
                        )
                    }
                }
            }
        }

        if (showAddCategoryDialog) {
            AddEditCategoryDialog(
                editingCategory = null,
                onDismiss = { showAddCategoryDialog = false },
                onSave = { cat ->
                    viewModel.saveCategory(cat)
                    showAddCategoryDialog = false
                }
            )
        }

        if (editingCategory != null) {
            AddEditCategoryDialog(
                editingCategory = editingCategory,
                onDismiss = { editingCategory = null },
                onSave = { cat ->
                    viewModel.saveCategory(cat)
                    editingCategory = null
                }
            )
        }
    }
}

@Composable
fun CategoryItemRow(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(category.colorHex)).copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(category.colorHex)))
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = category.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit category", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete category", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddEditCategoryDialog(
    editingCategory: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember { mutableStateOf(editingCategory?.name ?: "") }
    var selectedColorHex by remember { mutableStateOf(editingCategory?.colorHex ?: presetColors.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (editingCategory == null) "New Category" else "Edit Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Pick Category Chip Color", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))

                // Presets circular color selector row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScrollCompose(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { hexColor ->
                        val color = Color(android.graphics.Color.parseColor(hexColor))
                        val isSelected = selectedColorHex == hexColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hexColor },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val savedCat = Category(
                                    id = editingCategory?.id ?: 0L,
                                    name = name,
                                    colorHex = selectedColorHex
                                )
                                onSave(savedCat)
                            }
                        }
                    ) {
                        Text("SAVE")
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. SMART ALERTS DASHBOARD (TAB 4)
// ==========================================
@Composable
fun AlertsDashboard(viewModel: GroceryViewModel) {
    val lowStockList by viewModel.lowStockProducts.collectAsState()
    val expiringList by viewModel.expiringSoonProducts.collectAsState()
    
    val totalLowStock = lowStockList.size
    val totalExpiring = expiringList.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScrollComposeActual()
    ) {
        Text(
            text = "Smart Alerts Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Automated store metrics checking expiration and low-stock levels.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Summary metric cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (totalLowStock > 0) Color(0xFFFFF8E1) else Color(0xFFF1F8E9)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("LOW STOCK ALERT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = totalLowStock.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (totalLowStock > 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                    )
                    Text("Products < 5 units", fontSize = 10.sp)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (totalExpiring > 0) Color(0xFFFFEBEE) else Color(0xFFF1F8E9)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("EXPIRING SOON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = totalExpiring.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (totalExpiring > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                    Text("Expires in 7 days", fontSize = 10.sp)
                }
            }
        }

        // SECTIONS LISTS
        // Section A: Low Stock Products (Amber Header)
        Text(
            text = "Low Stock Register",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFFE65100),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (totalLowStock == 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, "Healthy", tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("All products have robust stock levels!", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            lowStockList.forEach { product ->
                AlertLowStockRow(product = product)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Section B: Expiring Soon Products (Red Header)
        Text(
            text = "Expiring Soon Register",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFFC62828),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (totalExpiring == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, "Fresh", tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zero raw foods expiring in 7 days!", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            expiringList.forEach { product ->
                AlertExpiringRow(product = product)
            }
        }
    }
}

@Composable
fun AlertLowStockRow(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E1)
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, "Low Stock Warning", tint = Color(0xFFFFB300), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Price: ${String.format("%.2f", product.price)} DH", fontSize = 11.sp)
            }
            Text(
                "Qty: ${product.stockQuantity}",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = Color(0xFFE65100)
            )
        }
    }
}

@Composable
fun AlertExpiringRow(product: Product) {
    val dateString = remember(product.expiryDate) {
        if (product.expiryDate != null) {
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(product.expiryDate))
        } else ""
    }
    
    val daysLeft = remember(product.expiryDate) {
        if (product.expiryDate != null) {
            val diff = product.expiryDate - System.currentTimeMillis()
            (diff / (24 * 60 * 60 * 1000L)).toInt()
        } else 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.NewReleases, "Expiry Action Needed", tint = Color(0xFFE53935), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Expires on: $dateString", fontSize = 11.sp, color = Color.DarkGray)
            }
            Text(
                text = if (daysLeft < 0) "EXPIRED" else "in $daysLeft days",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFFC62828)
            )
        }
    }
}

// ==========================================
// CENTRAL CAMERAX INSTANCE VIEW WITH MLKIT (CLEAN)
// ==========================================
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScannerView(
    modifier: Modifier = Modifier,
    isTorchOn: Boolean,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    var activeCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(isTorchOn, activeCamera) {
        try {
            activeCamera?.cameraControl?.enableTorch(isTorchOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    ) { view ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                surfaceProvider = view.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val scanner = BarcodeScanning.getClient()

            // State for length validation & debouncing (Rule 1 & Rule 2)
            var lastScannedBarcode: String? = null
            var scanCount = 0
            var firstScanTime = 0L

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                val rawValue = barcode.rawValue
                                // Rule 1: EAN-13 Length Validation. Only accept exactly 13 characters.
                                if (rawValue != null && rawValue.length == 13) {
                                    val currentTime = System.currentTimeMillis()
                                    // Rule 2: Consecutive Scan Verification / Debouncing
                                    if (rawValue == lastScannedBarcode) {
                                        if (currentTime - firstScanTime <= 400) {
                                            scanCount++
                                        } else {
                                            // The 400ms window expired. Reset counter and update timing.
                                            scanCount = 1
                                            firstScanTime = currentTime
                                        }
                                    } else {
                                        // Different barcode sequence detected, reset state.
                                        lastScannedBarcode = rawValue
                                        scanCount = 1
                                        firstScanTime = currentTime
                                    }

                                    // Check if we hit the consecutive threshold of at least 3 scans within the window
                                    if (scanCount >= 3) {
                                        onBarcodeScanned(rawValue)
                                        // Reset immediately to avoid double triggering on sequential frames
                                        lastScannedBarcode = null
                                        scanCount = 0
                                        firstScanTime = 0L
                                        break
                                    }
                                }
                            }
                        }
                        .addOnFailureListener {
                            // Suppressed analysis failure logs
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                activeCamera = camera
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }
}

@Composable
fun BackupSettingsDialog(
    viewModel: GroceryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val todayRevenue by viewModel.todayRevenue.collectAsState()
    var pastedJsonText by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "App Settings & Manual Backups",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Control database state and track store metrics offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F8E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC5E1A5))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TODAY'S REVENUE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF558B2F),
                            letterSpacing = 1.1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.2f", todayRevenue)} DH",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "Export Backup Database",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                )
                Text(
                    text = "Converts and structures catalog items, categories, and receipts into a shared text structure. Copies backup content directly to your phone clipboard automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                Button(
                    onClick = { viewModel.exportBackupAsJson(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, "Export", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export & Copy Database", fontSize = 12.sp)
                }

                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    text = "Import / Restore Database",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                )
                Text(
                    text = "Pasting database snapshot content below will restore full transaction archives and stock levels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = pastedJsonText,
                    onValueChange = { pastedJsonText = it },
                    label = { Text("Paste JSON Backup content here") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    maxLines = 6,
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (pastedJsonText.isNotBlank()) {
                            viewModel.importBackupFromJson(pastedJsonText)
                            pastedJsonText = ""
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Please paste database JSON text!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = pastedJsonText.isNotBlank()
                ) {
                    Icon(Icons.Default.Upload, "Import", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import & Restore", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
