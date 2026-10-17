package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.GoogleUser
import com.example.data.model.OrderItemEntry
import com.example.data.model.OrderTransaction
import com.example.data.model.Product
import com.example.data.model.SaleTransaction
import com.example.data.model.StoreProfile
import com.example.data.repository.GoogleAuthManager
import com.example.data.repository.KesanRepository
import com.example.util.FormatUtils
import android.content.Context
import android.content.Intent
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.GoogleDriveRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SaleFilterType {
    ALL, CASH, DEBT
}

enum class FinancePeriod {
    ALL, THIS_MONTH, TODAY
}

data class ProductDistribution(
    val product: Product,
    val quantitySold: Int,
    val totalRevenue: Double,
    val percentageOfSales: Float
)

data class FinanceSummary(
    val period: FinancePeriod,
    val totalSalesRevenue: Double,
    val totalCashIn: Double,
    val totalSalesDebt: Double,
    val totalOrderExpense: Double,
    val totalFactoryDebt: Double,
    val estimatedProfit: Double,
    val distributionList: List<ProductDistribution>
)

class KesanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KesanRepository
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(application)
    private val googleDriveRepository: GoogleDriveRepository = GoogleDriveRepository()
    private val googleAuthManager: GoogleAuthManager = GoogleAuthManager(application)

    // Google User Session & Data Sync States
    private val _currentUser = MutableStateFlow<GoogleUser?>(googleAuthManager.getLoggedInUser())
    val currentUser: StateFlow<GoogleUser?> = _currentUser.asStateFlow()

    private val _savedAccounts = MutableStateFlow<List<GoogleUser>>(googleAuthManager.getSavedAccounts())
    val savedAccounts: StateFlow<List<GoogleUser>> = _savedAccounts.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(
        googleAuthManager.getLoggedInUser()?.lastSyncTimestamp ?: System.currentTimeMillis()
    )
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow("Tersinkronisasi dengan akun Google & Cloud Firestore")
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    private val _isPullingFromCloud = MutableStateFlow(false)
    val isPullingFromCloud: StateFlow<Boolean> = _isPullingFromCloud.asStateFlow()

    private val _autoSyncEnabled = MutableStateFlow(true)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    // Google Drive Upload States
    private val _isDriveUploading = MutableStateFlow(false)
    val isDriveUploading: StateFlow<Boolean> = _isDriveUploading.asStateFlow()

    private val _driveUploadStatus = MutableStateFlow<String?>(null)
    val driveUploadStatus: StateFlow<String?> = _driveUploadStatus.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = KesanRepository(db.kesanDao())
    }

    // Master flows isolated by logged in Google Account
    @OptIn(ExperimentalCoroutinesApi::class)
    val storeProfile: StateFlow<StoreProfile> = _currentUser.flatMapLatest { user ->
        val email = user?.email.orEmpty()
        if (email.isBlank()) {
            flowOf(StoreProfile())
        } else {
            repository.getStoreProfile(email).map {
                it ?: StoreProfile(
                    accountEmail = email,
                    ownerName = user?.displayName ?: "AGEN KESAN"
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StoreProfile()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<Product>> = _currentUser.flatMapLatest { user ->
        val email = user?.email.orEmpty()
        if (email.isBlank()) flowOf(emptyList())
        else repository.getAllProducts(email)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val sales: StateFlow<List<SaleTransaction>> = _currentUser.flatMapLatest { user ->
        val email = user?.email.orEmpty()
        if (email.isBlank()) flowOf(emptyList())
        else repository.getAllSales(email)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val orders: StateFlow<List<OrderTransaction>> = _currentUser.flatMapLatest { user ->
        val email = user?.email.orEmpty()
        if (email.isBlank()) flowOf(emptyList())
        else repository.getAllOrders(email)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI state: Filters
    private val _saleSearchQuery = MutableStateFlow("")
    val saleSearchQuery: StateFlow<String> = _saleSearchQuery.asStateFlow()

    private val _saleFilterType = MutableStateFlow(SaleFilterType.ALL)
    val saleFilterType: StateFlow<SaleFilterType> = _saleFilterType.asStateFlow()

    private val _financePeriod = MutableStateFlow(FinancePeriod.ALL)
    val financePeriod: StateFlow<FinancePeriod> = _financePeriod.asStateFlow()

    // Dialog & Interaction State
    private val _selectedProductForSale = MutableStateFlow<Product?>(null)
    val selectedProductForSale: StateFlow<Product?> = _selectedProductForSale.asStateFlow()

    private val _saleToEdit = MutableStateFlow<SaleTransaction?>(null)
    val saleToEdit: StateFlow<SaleTransaction?> = _saleToEdit.asStateFlow()

    private val _receiptPreviewSale = MutableStateFlow<SaleTransaction?>(null)
    val receiptPreviewSale: StateFlow<SaleTransaction?> = _receiptPreviewSale.asStateFlow()

    private val _orderPreview = MutableStateFlow<OrderTransaction?>(null)
    val orderPreview: StateFlow<OrderTransaction?> = _orderPreview.asStateFlow()

    private val _isNewOrderDialogOpen = MutableStateFlow(false)
    val isNewOrderDialogOpen: StateFlow<Boolean> = _isNewOrderDialogOpen.asStateFlow()

    private val _orderToEdit = MutableStateFlow<OrderTransaction?>(null)
    val orderToEdit: StateFlow<OrderTransaction?> = _orderToEdit.asStateFlow()

    private val _productToEdit = MutableStateFlow<Product?>(null)
    val productToEdit: StateFlow<Product?> = _productToEdit.asStateFlow()

    private val _isProductDialogOpen = MutableStateFlow(false)
    val isProductDialogOpen: StateFlow<Boolean> = _isProductDialogOpen.asStateFlow()

    // Filtered Sales Flow
    val filteredSales: StateFlow<List<SaleTransaction>> = combine(
        sales,
        _saleSearchQuery,
        _saleFilterType
    ) { list, query, filter ->
        list.filter { sale ->
            val matchQuery = query.isBlank() ||
                    sale.customerName.contains(query, ignoreCase = true) ||
                    sale.productName.contains(query, ignoreCase = true) ||
                    sale.invoiceNumber.contains(query, ignoreCase = true)

            val matchFilter = when (filter) {
                SaleFilterType.ALL -> true
                SaleFilterType.CASH -> !sale.isDebt
                SaleFilterType.DEBT -> sale.isDebt
            }

            matchQuery && matchFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Finance Summary Flow
    val financeSummary: StateFlow<FinanceSummary> = combine(
        sales,
        orders,
        products,
        _financePeriod
    ) { allSales, allOrders, allProducts, period ->
        val now = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val filteredSalesList = allSales.filter {
            when (period) {
                FinancePeriod.ALL -> true
                FinancePeriod.TODAY -> it.timestamp >= startOfToday
                FinancePeriod.THIS_MONTH -> it.timestamp >= startOfMonth
            }
        }

        val filteredOrdersList = allOrders.filter {
            when (period) {
                FinancePeriod.ALL -> true
                FinancePeriod.TODAY -> it.timestamp >= startOfToday
                FinancePeriod.THIS_MONTH -> it.timestamp >= startOfMonth
            }
        }

        val totalSalesRevenue = filteredSalesList.sumOf { it.totalPrice }
        val totalCashIn = filteredSalesList.filter { !it.isDebt }.sumOf { it.totalPrice }
        val totalSalesDebt = filteredSalesList.filter { it.isDebt }.sumOf { it.totalPrice }

        val totalOrderExpense = filteredOrdersList.sumOf { it.totalAmount }
        val totalFactoryDebt = filteredOrdersList.filter { it.paymentStatus == "HUTANG PABRIK" }.sumOf { it.totalAmount }

        // Estimated profit = Sales Revenue - Cost of Goods Sold (using product order price)
        var totalCogs = 0.0
        val productMap = allProducts.associateBy { it.id }
        for (s in filteredSalesList) {
            val prod = productMap[s.productId]
            val costPrice = prod?.orderPrice ?: 0.0
            totalCogs += (costPrice * s.quantity)
        }
        val estimatedProfit = (totalSalesRevenue - totalCogs).coerceAtLeast(0.0)

        // Peta Distribusi Penjualan Produk
        val salesByProduct = filteredSalesList.groupBy { it.productId }
        val distributionList = allProducts.map { prod ->
            val salesOfThis = salesByProduct[prod.id] ?: emptyList()
            val qtySold = salesOfThis.sumOf { it.quantity }
            val rev = salesOfThis.sumOf { it.totalPrice }
            val share = if (totalSalesRevenue > 0) ((rev / totalSalesRevenue) * 100f).toFloat() else 0f
            ProductDistribution(
                product = prod,
                quantitySold = qtySold,
                totalRevenue = rev,
                percentageOfSales = share
            )
        }.sortedByDescending { it.totalRevenue }

        FinanceSummary(
            period = period,
            totalSalesRevenue = totalSalesRevenue,
            totalCashIn = totalCashIn,
            totalSalesDebt = totalSalesDebt,
            totalOrderExpense = totalOrderExpense,
            totalFactoryDebt = totalFactoryDebt,
            estimatedProfit = estimatedProfit,
            distributionList = distributionList
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FinanceSummary(
            FinancePeriod.ALL,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            emptyList()
        )
    )

    // Setters for UI interactions
    fun setSaleSearchQuery(q: String) { _saleSearchQuery.value = q }
    fun setSaleFilterType(t: SaleFilterType) { _saleFilterType.value = t }
    fun setFinancePeriod(p: FinancePeriod) { _financePeriod.value = p }

    fun openSaleDialogForProduct(product: Product) { _selectedProductForSale.value = product }
    fun closeSaleDialog() { _selectedProductForSale.value = null }

    fun openEditSale(sale: SaleTransaction) { _saleToEdit.value = sale }
    fun closeEditSale() { _saleToEdit.value = null }

    fun showReceiptPreview(sale: SaleTransaction) { _receiptPreviewSale.value = sale }
    fun closeReceiptPreview() { _receiptPreviewSale.value = null }

    fun showOrderPreview(order: OrderTransaction) { _orderPreview.value = order }
    fun closeOrderPreview() { _orderPreview.value = null }

    fun openNewOrderDialog() { _isNewOrderDialogOpen.value = true }
    fun closeNewOrderDialog() { _isNewOrderDialogOpen.value = false }

    fun openEditOrder(order: OrderTransaction) { _orderToEdit.value = order }
    fun closeEditOrder() { _orderToEdit.value = null }

    fun openNewProductDialog() {
        _productToEdit.value = null
        _isProductDialogOpen.value = true
    }

    fun openEditProduct(product: Product) {
        _productToEdit.value = product
        _isProductDialogOpen.value = true
    }

    fun closeProductDialog() {
        _productToEdit.value = null
        _isProductDialogOpen.value = false
    }

    // Business Logic Actions
    fun createSale(
        product: Product,
        customerName: String,
        dateString: String,
        quantity: Int,
        isDebt: Boolean,
        notes: String,
        onCreated: (SaleTransaction) -> Unit
    ) {
        viewModelScope.launch {
            val userEmail = _currentUser.value?.email.orEmpty()
            val invoiceNo = FormatUtils.generateSaleInvoiceNumber()
            val totalPrice = product.sellPrice * quantity
            val sale = SaleTransaction(
                accountEmail = userEmail,
                invoiceNumber = invoiceNo,
                dateString = dateString.ifBlank { FormatUtils.getCurrentDateFormatted() },
                timestamp = System.currentTimeMillis(),
                customerName = customerName.ifBlank { "Umum / Pelanggan" },
                productId = product.id,
                productName = product.name,
                unitPrice = product.sellPrice,
                quantity = quantity,
                totalPrice = totalPrice,
                isDebt = isDebt,
                notes = notes
            )
            val newId = repository.addSale(sale, autoDecrementStock = true)
            val finalSale = sale.copy(id = newId)
            triggerAutoSync()
            closeSaleDialog()
            onCreated(finalSale)
        }
    }

    fun updateSaleTransaction(sale: SaleTransaction) {
        viewModelScope.launch {
            val userEmail = _currentUser.value?.email.orEmpty()
            repository.updateSale(sale.copy(accountEmail = userEmail))
            triggerAutoSync()
            closeEditSale()
        }
    }

    fun deleteSaleTransaction(sale: SaleTransaction) {
        viewModelScope.launch {
            repository.deleteSale(sale, restoreStock = true)
            triggerAutoSync()
            closeEditSale()
        }
    }

    fun markSaleAsPaid(sale: SaleTransaction) {
        viewModelScope.launch {
            val userEmail = _currentUser.value?.email.orEmpty()
            repository.updateSale(sale.copy(isDebt = false, accountEmail = userEmail))
            triggerAutoSync()
        }
    }

    fun createOrder(
        dateString: String,
        agentName: String,
        items: List<OrderItemEntry>,
        paymentStatus: String,
        notes: String,
        onCreated: (OrderTransaction) -> Unit
    ) {
        viewModelScope.launch {
            val userEmail = _currentUser.value?.email.orEmpty()
            val orderNumber = FormatUtils.generateOrderNumber()
            val totalAmount = items.sumOf { it.subtotal }
            val itemsSummary = items.filter { it.quantity > 0 }
                .joinToString(", ") { "${it.productName} (${it.quantity})" }
            val itemsJson = FormatUtils.orderItemsToJson(items)

            val order = OrderTransaction(
                accountEmail = userEmail,
                orderNumber = orderNumber,
                dateString = dateString.ifBlank { FormatUtils.getCurrentDateFormatted() },
                timestamp = System.currentTimeMillis(),
                agentName = agentName.ifBlank { "AGEN KESAN" },
                itemsSummary = itemsSummary,
                itemsJson = itemsJson,
                totalAmount = totalAmount,
                paymentStatus = paymentStatus,
                status = "MENUNGGU",
                isStockAdded = false,
                notes = notes
            )
            val id = repository.addOrder(order)
            val finalOrder = order.copy(id = id)
            triggerAutoSync()
            closeNewOrderDialog()
            onCreated(finalOrder)
        }
    }

    fun confirmOrderReceived(order: OrderTransaction) {
        viewModelScope.launch {
            repository.confirmReceiveOrder(order)
            triggerAutoSync()
        }
    }

    fun updateOrderTransaction(order: OrderTransaction) {
        viewModelScope.launch {
            val userEmail = _currentUser.value?.email.orEmpty()
            repository.updateOrder(order.copy(accountEmail = userEmail))
            triggerAutoSync()
            closeEditOrder()
        }
    }

    fun deleteOrderTransaction(order: OrderTransaction) {
        viewModelScope.launch {
            repository.deleteOrder(order)
            triggerAutoSync()
            closeEditOrder()
        }
    }

    fun saveProductItem(
        id: Long,
        name: String,
        packagingUnit: String,
        sellPrice: Double,
        orderPrice: Double,
        stock: Int,
        minStock: Int
    ) {
        viewModelScope.launch {
            val userEmail = _currentUser.value?.email.orEmpty()
            if (id == 0L) {
                repository.addProduct(
                    Product(
                        accountEmail = userEmail,
                        name = name,
                        packagingUnit = packagingUnit,
                        sellPrice = sellPrice,
                        orderPrice = orderPrice,
                        stock = stock,
                        minStockAlert = minStock
                    )
                )
            } else {
                repository.updateProduct(
                    Product(
                        id = id,
                        accountEmail = userEmail,
                        name = name,
                        packagingUnit = packagingUnit,
                        sellPrice = sellPrice,
                        orderPrice = orderPrice,
                        stock = stock,
                        minStockAlert = minStock
                    )
                )
            }
            triggerAutoSync()
            closeProductDialog()
        }
    }

    fun deleteProductItem(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            triggerAutoSync()
            closeProductDialog()
        }
    }

    fun saveProfile(profile: StoreProfile) {
        viewModelScope.launch {
            val userEmail = _currentUser.value?.email.orEmpty()
            repository.saveStoreProfile(profile.copy(accountEmail = userEmail))
            triggerAutoSync()
        }
    }

    // Google Auth & Sync Operations
    fun loginWithGoogle(name: String, email: String) {
        val user = googleAuthManager.saveLogin(name, email)
        _currentUser.value = user
        _savedAccounts.value = googleAuthManager.getSavedAccounts()
        _lastSyncTimestamp.value = user.lastSyncTimestamp
        syncDataWithGoogle()
    }

    fun logoutGoogle() {
        googleAuthManager.logout()
        _currentUser.value = null
        _savedAccounts.value = googleAuthManager.getSavedAccounts()
    }

    fun removeSavedAccount(email: String) {
        googleAuthManager.removeSavedAccount(email)
        _savedAccounts.value = googleAuthManager.getSavedAccounts()
        if (_currentUser.value?.email.equals(email, ignoreCase = true)) {
            _currentUser.value = null
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        if (enabled) {
            triggerAutoSync()
        }
    }

    fun syncDataWithGoogle(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val user = _currentUser.value ?: run {
            onComplete(false, "Akun Google belum dipilih.")
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusMessage.value = "Mengunggah data ke Cloud Firestore..."

            val result = firestoreRepository.syncFullData(
                userId = user.email,
                profile = storeProfile.value,
                products = products.value,
                sales = sales.value,
                orders = orders.value
            )

            delay(500)
            val newTimestamp = googleAuthManager.updateLastSyncTimestamp()
            _lastSyncTimestamp.value = newTimestamp
            _currentUser.value = user.copy(lastSyncTimestamp = newTimestamp)
            _savedAccounts.value = googleAuthManager.getSavedAccounts()
            _isSyncing.value = false
            _syncStatusMessage.value = "Tersinkron dengan Cloud Firestore (${user.email})"
            val msg = if (result.success) {
                "Berhasil menyinkronkan data ke Cloud Firestore (${result.productsCount} produk, ${result.salesCount} penjualan, ${result.ordersCount} order)."
            } else {
                result.message
            }
            onComplete(result.success, msg)
        }
    }

    fun pullDataFromCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val user = _currentUser.value ?: run {
            onComplete(false, "Silakan pilih/tautkan akun Google terlebih dahulu.")
            return
        }
        viewModelScope.launch {
            _isPullingFromCloud.value = true
            _syncStatusMessage.value = "Mengunduh data cadangan dari Cloud Firestore..."
            try {
                val bundle = firestoreRepository.fetchFullDataFromCloud(user.email)
                if (bundle != null && (bundle.profile != null || bundle.products.isNotEmpty() || bundle.sales.isNotEmpty() || bundle.orders.isNotEmpty())) {
                    repository.restoreFromCloudBundle(bundle)
                    val newTimestamp = googleAuthManager.updateLastSyncTimestamp()
                    _lastSyncTimestamp.value = newTimestamp
                    _currentUser.value = user.copy(lastSyncTimestamp = newTimestamp)
                    _savedAccounts.value = googleAuthManager.getSavedAccounts()
                    _isPullingFromCloud.value = false
                    _syncStatusMessage.value = "Data berhasil dipulihkan dari Cloud Firestore"
                    val summary = "Data berhasil dipulihkan: ${bundle.products.size} produk, ${bundle.sales.size} penjualan, ${bundle.orders.size} order."
                    onComplete(true, summary)
                } else {
                    _isPullingFromCloud.value = false
                    _syncStatusMessage.value = "Tidak ada data di cloud untuk ${user.email}"
                    onComplete(false, "Belum ada data cadangan di Cloud Firestore untuk akun ${user.email}. Silakan lakukan 'Upload ke Cloud' terlebih dahulu.")
                }
            } catch (e: Exception) {
                _isPullingFromCloud.value = false
                _syncStatusMessage.value = "Gagal mengunduh: ${e.message}"
                onComplete(false, "Gagal mengunduh data cloud: ${e.message}")
            }
        }
    }

    fun uploadDataToGoogleDrive(
        context: Context,
        onComplete: (Boolean, String, Intent?) -> Unit
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isDriveUploading.value = true
            _driveUploadStatus.value = "Menyiapkan file cadangan KESAN..."

            // Ensure Cloud Firestore is synced
            try {
                firestoreRepository.syncFullData(
                    userId = user.email,
                    profile = storeProfile.value,
                    products = products.value,
                    sales = sales.value,
                    orders = orders.value
                )
            } catch (_: Exception) {}

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanEmail = user.email.substringBefore('@').replace(".", "_")
            val fileName = "KESAN_Cadangan_${cleanEmail}_$dateStr.json"

            val backupJson = googleDriveRepository.createBackupJson(
                accountEmail = user.email,
                profile = storeProfile.value,
                products = products.value,
                sales = sales.value,
                orders = orders.value
            )

            val file = googleDriveRepository.saveBackupFileLocally(context, backupJson, fileName)
            val driveIntent = googleDriveRepository.buildGoogleDriveUploadIntent(context, file)

            _isDriveUploading.value = false
            _driveUploadStatus.value = "File $fileName siap diunggah ke Google Drive"
            onComplete(true, "Data siap diunggah ke Google Drive!", driveIntent)
        }
    }

    private fun triggerAutoSync() {
        if (!_autoSyncEnabled.value) return
        val user = _currentUser.value ?: return
        val newTimestamp = googleAuthManager.updateLastSyncTimestamp()
        _lastSyncTimestamp.value = newTimestamp
        _currentUser.value = user.copy(lastSyncTimestamp = newTimestamp)
        _savedAccounts.value = googleAuthManager.getSavedAccounts()

        // Background update to Firestore
        viewModelScope.launch {
            try {
                firestoreRepository.syncFullData(
                    userId = user.email,
                    profile = storeProfile.value,
                    products = products.value,
                    sales = sales.value,
                    orders = orders.value
                )
            } catch (_: Exception) {}
        }
    }
}

