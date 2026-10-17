package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.EditOrderDialog
import com.example.ui.components.EditSaleDialog
import com.example.ui.components.NewOrderDialog
import com.example.ui.components.NotaDialog
import com.example.ui.components.OrderStrukDialog
import com.example.ui.components.ProductEditDialog
import com.example.ui.components.TransactionDialog
import com.example.ui.screens.AkunScreen
import com.example.ui.screens.JualScreen
import com.example.ui.screens.KeuanganScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OrderScreen
import com.example.ui.screens.RiwayatPenjualanScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.KesanViewModel
import kotlinx.coroutines.launch

data class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                KesanApp()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KesanApp(viewModel: KesanViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
    val isPullingFromCloud by viewModel.isPullingFromCloud.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val isDriveUploading by viewModel.isDriveUploading.collectAsStateWithLifecycle()
    val driveUploadStatus by viewModel.driveUploadStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val filteredSales by viewModel.filteredSales.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.saleSearchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.saleFilterType.collectAsStateWithLifecycle()
    val financeSummary by viewModel.financeSummary.collectAsStateWithLifecycle()

    // Dialog states
    val selectedProductForSale by viewModel.selectedProductForSale.collectAsStateWithLifecycle()
    val saleToEdit by viewModel.saleToEdit.collectAsStateWithLifecycle()
    val receiptPreviewSale by viewModel.receiptPreviewSale.collectAsStateWithLifecycle()
    val orderPreview by viewModel.orderPreview.collectAsStateWithLifecycle()
    val isNewOrderDialogOpen by viewModel.isNewOrderDialogOpen.collectAsStateWithLifecycle()
    val orderToEdit by viewModel.orderToEdit.collectAsStateWithLifecycle()
    val productToEdit by viewModel.productToEdit.collectAsStateWithLifecycle()
    val isProductDialogOpen by viewModel.isProductDialogOpen.collectAsStateWithLifecycle()

    // If not logged in, show Google Login Screen with multi-account selection
    if (currentUser == null) {
        LoginScreen(
            savedAccounts = savedAccounts,
            onLoginSuccess = { name, email ->
                viewModel.loginWithGoogle(name, email)
            },
            onRemoveAccount = { email ->
                viewModel.removeSavedAccount(email)
            }
        )
        return
    }

    val tabs = listOf(
        NavigationTab("Jual", Icons.Filled.Storefront, Icons.Outlined.Storefront, "tab_jual"),
        NavigationTab("Riwayat", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_riwayat"),
        NavigationTab("Order", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping, "tab_order"),
        NavigationTab("Keuangan", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "tab_keuangan"),
        NavigationTab("Akun", Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts, "tab_akun")
    )

    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = pagerState.currentPage == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 5 Menu Halaman yang bisa di-geser (HorizontalPager)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> JualScreen(
                        profile = storeProfile,
                        products = products,
                        onProductSelected = { prod ->
                            viewModel.openSaleDialogForProduct(prod)
                        },
                        onAddNewProduct = {
                            viewModel.openNewProductDialog()
                        }
                    )
                    1 -> RiwayatPenjualanScreen(
                        profile = storeProfile,
                        sales = filteredSales,
                        allSales = sales,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { q -> viewModel.setSaleSearchQuery(q) },
                        filterType = filterType,
                        onFilterChange = { t -> viewModel.setSaleFilterType(t) },
                        onEditSale = { sale -> viewModel.openEditSale(sale) },
                        onMarkAsPaid = { sale -> viewModel.markSaleAsPaid(sale) },
                        onPrintReceipt = { sale -> viewModel.showReceiptPreview(sale) }
                    )
                    2 -> OrderScreen(
                        profile = storeProfile,
                        orders = orders,
                        onOpenNewOrder = { viewModel.openNewOrderDialog() },
                        onConfirmReceived = { order -> viewModel.confirmOrderReceived(order) },
                        onEditOrder = { order -> viewModel.openEditOrder(order) },
                        onPrintStruk = { order -> viewModel.showOrderPreview(order) }
                    )
                    3 -> KeuanganScreen(
                        profile = storeProfile,
                        summary = financeSummary,
                        products = products,
                        sales = sales,
                        onPeriodChange = { p -> viewModel.setFinancePeriod(p) },
                        onPrintReceipt = { sale -> viewModel.showReceiptPreview(sale) }
                    )
                    4 -> AkunScreen(
                        profile = storeProfile,
                        products = products,
                        currentUser = currentUser,
                        isSyncing = isSyncing,
                        lastSyncTimestamp = lastSyncTimestamp,
                        syncStatusMessage = syncStatusMessage,
                        isPullingFromCloud = isPullingFromCloud,
                        autoSyncEnabled = autoSyncEnabled,
                        isDriveUploading = isDriveUploading,
                        driveUploadStatus = driveUploadStatus,
                        onSyncNow = {
                            viewModel.syncDataWithGoogle { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onPullFromCloud = {
                            viewModel.pullDataFromCloud { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        onToggleAutoSync = { enabled ->
                            viewModel.toggleAutoSync(enabled)
                            val statusText = if (enabled) "Sinkronisasi otomatis aktif" else "Sinkronisasi otomatis nonaktif"
                            Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                        },
                        onUploadGoogleDrive = {
                            viewModel.uploadDataToGoogleDrive(context) { success, msg, intent ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                intent?.let {
                                    try {
                                        context.startActivity(it)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Membuka penyimpanan Google Drive...", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onLogoutGoogle = { viewModel.logoutGoogle() },
                        onSaveProfile = { prof -> viewModel.saveProfile(prof) },
                        onAddNewProduct = { viewModel.openNewProductDialog() },
                        onEditProduct = { prod -> viewModel.openEditProduct(prod) },
                        onDeleteProduct = { prod -> viewModel.deleteProductItem(prod) }
                    )
                }
            }
        }
    }

    // 1. Transaction Dialog (when product box in Jual is clicked)
    selectedProductForSale?.let { product ->
        TransactionDialog(
            initialProduct = product,
            allProducts = products,
            onDismiss = { viewModel.closeSaleDialog() },
            onSaveTransaction = { prod, customer, date, qty, isDebt, notes, printNota ->
                viewModel.createSale(
                    product = prod,
                    customerName = customer,
                    dateString = date,
                    quantity = qty,
                    isDebt = isDebt,
                    notes = notes
                ) { newSale ->
                    if (printNota) {
                        viewModel.showReceiptPreview(newSale)
                    }
                }
            }
        )
    }

    // 2. Receipt Preview Dialog (Cetak Nota)
    receiptPreviewSale?.let { sale ->
        NotaDialog(
            sale = sale,
            profile = storeProfile,
            onDismiss = { viewModel.closeReceiptPreview() }
        )
    }

    // 3. Edit Sale Dialog
    saleToEdit?.let { sale ->
        EditSaleDialog(
            sale = sale,
            onDismiss = { viewModel.closeEditSale() },
            onSaveEdit = { updated -> viewModel.updateSaleTransaction(updated) },
            onDelete = { toDelete -> viewModel.deleteSaleTransaction(toDelete) }
        )
    }

    // 4. New Factory Order Dialog
    if (isNewOrderDialogOpen) {
        NewOrderDialog(
            profile = storeProfile,
            allProducts = products,
            onDismiss = { viewModel.closeNewOrderDialog() },
            onSubmitOrder = { date, agent, items, paymentStatus, notes ->
                viewModel.createOrder(
                    dateString = date,
                    agentName = agent,
                    items = items,
                    paymentStatus = paymentStatus,
                    notes = notes
                ) { newOrder ->
                    viewModel.showOrderPreview(newOrder)
                }
            }
        )
    }

    // 5. Factory Order Slip Dialog (Cetak Struk Order)
    orderPreview?.let { order ->
        OrderStrukDialog(
            order = order,
            profile = storeProfile,
            onDismiss = { viewModel.closeOrderPreview() }
        )
    }

    // 6. Edit Factory Order Dialog
    orderToEdit?.let { order ->
        EditOrderDialog(
            order = order,
            onDismiss = { viewModel.closeEditOrder() },
            onSaveEdit = { updated -> viewModel.updateOrderTransaction(updated) },
            onDelete = { toDelete -> viewModel.deleteOrderTransaction(toDelete) }
        )
    }

    // 7. Add/Edit Product Dialog
    if (isProductDialogOpen) {
        ProductEditDialog(
            product = productToEdit,
            onDismiss = { viewModel.closeProductDialog() },
            onSaveProduct = { id, name, unit, sell, orderPrice, stock, minStock ->
                viewModel.saveProductItem(id, name, unit, sell, orderPrice, stock, minStock)
            },
            onDeleteProduct = { toDelete ->
                viewModel.deleteProductItem(toDelete)
            }
        )
    }
}
