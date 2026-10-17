package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.OrderTransaction
import com.example.data.model.Product
import com.example.data.model.SaleTransaction
import com.example.data.model.StoreProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class CloudDataBundle(
    val profile: StoreProfile?,
    val products: List<Product>,
    val sales: List<SaleTransaction>,
    val orders: List<OrderTransaction>
)

data class CloudSyncResult(
    val success: Boolean,
    val message: String,
    val productsCount: Int = 0,
    val salesCount: Int = 0,
    val ordersCount: Int = 0
)

class FirestoreRepository(
    private val context: Context? = null
) {

    companion object {
        private const val TAG = "FirestoreRepository"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_PRODUCTS = "products"
        private const val COLLECTION_SALES = "sales"
        private const val COLLECTION_ORDERS = "orders"
        private const val DOC_PROFILE = "store_profile"
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.kesanwater.azkqwv")
                    .setProjectId("gen-lang-client-0645171996")
                    .setApiKey("AIzaSyDefaultPlaceholderApiKey")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseFirestore not ready: ${e.message}")
            null
        }
    }

    fun getFirebaseAuth(): FirebaseAuth? {
        return try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.kesanwater.azkqwv")
                    .setProjectId("gen-lang-client-0645171996")
                    .setApiKey("AIzaSyDefaultPlaceholderApiKey")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseAuth not ready: ${e.message}")
            null
        }
    }

    suspend fun ensureFirebaseAuth(): FirebaseUser? {
        val auth = getFirebaseAuth() ?: return null
        return try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            auth.currentUser
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth sign-in status: ${e.message}")
            auth.currentUser
        }
    }

    private fun getUserDoc(userId: String): DocumentReference? {
        val db = getFirestore() ?: return null
        return db.collection(COLLECTION_USERS).document(userId.ifBlank { "anonymous" })
    }

    // Store Profile Sync
    suspend fun saveStoreProfile(userId: String, profile: StoreProfile) {
        val userDoc = getUserDoc(userId) ?: return
        try {
            val map = hashMapOf(
                "accountEmail" to profile.accountEmail,
                "storeName" to profile.storeName,
                "tagline" to profile.tagline,
                "address" to profile.address,
                "phone" to profile.phone,
                "ownerName" to profile.ownerName,
                "updatedAt" to System.currentTimeMillis()
            )
            userDoc.collection("profile")
                .document(DOC_PROFILE)
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile to Firestore", e)
        }
    }

    fun getStoreProfileFlow(userId: String): Flow<StoreProfile?> = callbackFlow {
        val userDoc = getUserDoc(userId)
        if (userDoc == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = userDoc.collection("profile")
            .document(DOC_PROFILE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for profile", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = StoreProfile(
                        accountEmail = snapshot.getString("accountEmail") ?: "",
                        storeName = snapshot.getString("storeName") ?: "AGEN KESAN",
                        tagline = snapshot.getString("tagline") ?: "Tetesan berkahnya gak abis abis",
                        address = snapshot.getString("address") ?: "Bajul Kedunglerep Modo Lamongan",
                        phone = snapshot.getString("phone") ?: "082229831728",
                        ownerName = snapshot.getString("ownerName") ?: "AGEN KESAN"
                    )
                    trySend(profile)
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    // Products Sync
    suspend fun saveProduct(userId: String, product: Product) {
        val userDoc = getUserDoc(userId) ?: return
        try {
            val map = hashMapOf(
                "id" to product.id,
                "accountEmail" to product.accountEmail,
                "name" to product.name,
                "packagingUnit" to product.packagingUnit,
                "sellPrice" to product.sellPrice,
                "orderPrice" to product.orderPrice,
                "stock" to product.stock,
                "minStockAlert" to product.minStockAlert,
                "updatedAt" to System.currentTimeMillis()
            )
            userDoc.collection(COLLECTION_PRODUCTS)
                .document(product.id.toString())
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving product to Firestore", e)
        }
    }

    suspend fun deleteProduct(userId: String, productId: Long) {
        val userDoc = getUserDoc(userId) ?: return
        try {
            userDoc.collection(COLLECTION_PRODUCTS)
                .document(productId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting product from Firestore", e)
        }
    }

    fun getProductsFlow(userId: String): Flow<List<Product>> = callbackFlow {
        val userDoc = getUserDoc(userId)
        if (userDoc == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = userDoc.collection(COLLECTION_PRODUCTS)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for products", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = mutableListOf<Product>()
                snapshots?.forEach { doc ->
                    val prod = Product(
                        id = doc.getLong("id") ?: (doc.id.toLongOrNull() ?: 0L),
                        accountEmail = doc.getString("accountEmail") ?: "",
                        name = doc.getString("name") ?: "",
                        packagingUnit = doc.getString("packagingUnit") ?: "Dus",
                        sellPrice = doc.getDouble("sellPrice") ?: 0.0,
                        orderPrice = doc.getDouble("orderPrice") ?: 0.0,
                        stock = doc.getLong("stock")?.toInt() ?: 0,
                        minStockAlert = doc.getLong("minStockAlert")?.toInt() ?: 10
                    )
                    list.add(prod)
                }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // Sale Transactions Sync
    suspend fun saveSale(userId: String, sale: SaleTransaction) {
        val userDoc = getUserDoc(userId) ?: return
        try {
            val map = hashMapOf(
                "id" to sale.id,
                "accountEmail" to sale.accountEmail,
                "invoiceNumber" to sale.invoiceNumber,
                "dateString" to sale.dateString,
                "timestamp" to sale.timestamp,
                "customerName" to sale.customerName,
                "productId" to sale.productId,
                "productName" to sale.productName,
                "unitPrice" to sale.unitPrice,
                "quantity" to sale.quantity,
                "totalPrice" to sale.totalPrice,
                "isDebt" to sale.isDebt,
                "notes" to sale.notes,
                "updatedAt" to System.currentTimeMillis()
            )
            userDoc.collection(COLLECTION_SALES)
                .document(if (sale.id > 0) sale.id.toString() else sale.invoiceNumber)
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving sale to Firestore", e)
        }
    }

    suspend fun deleteSale(userId: String, saleId: Long) {
        val userDoc = getUserDoc(userId) ?: return
        try {
            userDoc.collection(COLLECTION_SALES)
                .document(saleId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting sale from Firestore", e)
        }
    }

    fun getSalesFlow(userId: String): Flow<List<SaleTransaction>> = callbackFlow {
        val userDoc = getUserDoc(userId)
        if (userDoc == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = userDoc.collection(COLLECTION_SALES)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for sales", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = mutableListOf<SaleTransaction>()
                snapshots?.forEach { doc ->
                    val sale = SaleTransaction(
                        id = doc.getLong("id") ?: (doc.id.toLongOrNull() ?: 0L),
                        accountEmail = doc.getString("accountEmail") ?: "",
                        invoiceNumber = doc.getString("invoiceNumber") ?: doc.id,
                        dateString = doc.getString("dateString") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        customerName = doc.getString("customerName") ?: "",
                        productId = doc.getLong("productId") ?: 0L,
                        productName = doc.getString("productName") ?: "",
                        unitPrice = doc.getDouble("unitPrice") ?: 0.0,
                        quantity = doc.getLong("quantity")?.toInt() ?: 1,
                        totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                        isDebt = doc.getBoolean("isDebt") ?: false,
                        notes = doc.getString("notes") ?: ""
                    )
                    list.add(sale)
                }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // Order Transactions Sync
    suspend fun saveOrder(userId: String, order: OrderTransaction) {
        val userDoc = getUserDoc(userId) ?: return
        try {
            val map = hashMapOf(
                "id" to order.id,
                "accountEmail" to order.accountEmail,
                "orderNumber" to order.orderNumber,
                "dateString" to order.dateString,
                "timestamp" to order.timestamp,
                "agentName" to order.agentName,
                "itemsSummary" to order.itemsSummary,
                "itemsJson" to order.itemsJson,
                "totalAmount" to order.totalAmount,
                "paymentStatus" to order.paymentStatus,
                "status" to order.status,
                "isStockAdded" to order.isStockAdded,
                "notes" to order.notes,
                "updatedAt" to System.currentTimeMillis()
            )
            userDoc.collection(COLLECTION_ORDERS)
                .document(if (order.id > 0) order.id.toString() else order.orderNumber)
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving order to Firestore", e)
        }
    }

    suspend fun deleteOrder(userId: String, orderId: Long) {
        val userDoc = getUserDoc(userId) ?: return
        try {
            userDoc.collection(COLLECTION_ORDERS)
                .document(orderId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting order from Firestore", e)
        }
    }

    fun getOrdersFlow(userId: String): Flow<List<OrderTransaction>> = callbackFlow {
        val userDoc = getUserDoc(userId)
        if (userDoc == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = userDoc.collection(COLLECTION_ORDERS)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for orders", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = mutableListOf<OrderTransaction>()
                snapshots?.forEach { doc ->
                    val ord = OrderTransaction(
                        id = doc.getLong("id") ?: (doc.id.toLongOrNull() ?: 0L),
                        accountEmail = doc.getString("accountEmail") ?: "",
                        orderNumber = doc.getString("orderNumber") ?: doc.id,
                        dateString = doc.getString("dateString") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        agentName = doc.getString("agentName") ?: "AGEN KESAN",
                        itemsSummary = doc.getString("itemsSummary") ?: "",
                        itemsJson = doc.getString("itemsJson") ?: "[]",
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        paymentStatus = doc.getString("paymentStatus") ?: "CASH",
                        status = doc.getString("status") ?: "MENUNGGU",
                        isStockAdded = doc.getBoolean("isStockAdded") ?: false,
                        notes = doc.getString("notes") ?: ""
                    )
                    list.add(ord)
                }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // Batch full backup / push to cloud
    suspend fun syncFullData(
        userId: String,
        profile: StoreProfile,
        products: List<Product>,
        sales: List<SaleTransaction>,
        orders: List<OrderTransaction>
    ): CloudSyncResult {
        ensureFirebaseAuth()
        return try {
            saveStoreProfile(userId, profile)
            var pCount = 0
            for (prod in products) {
                saveProduct(userId, prod)
                pCount++
            }
            var sCount = 0
            for (sale in sales) {
                saveSale(userId, sale)
                sCount++
            }
            var oCount = 0
            for (ord in orders) {
                saveOrder(userId, ord)
                oCount++
            }
            CloudSyncResult(
                success = true,
                message = "Berhasil menyinkronkan data ke Cloud Firestore ($userId)",
                productsCount = pCount,
                salesCount = sCount,
                ordersCount = oCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            CloudSyncResult(
                success = false,
                message = "Gagal sinkron: ${e.message}"
            )
        }
    }

    // Full restore / pull from cloud
    suspend fun fetchFullDataFromCloud(userId: String): CloudDataBundle? {
        val userDoc = getUserDoc(userId) ?: return null
        ensureFirebaseAuth()
        return try {
            val profileSnap = userDoc.collection("profile").document(DOC_PROFILE).get().await()
            val profile = if (profileSnap.exists()) {
                StoreProfile(
                    accountEmail = profileSnap.getString("accountEmail") ?: userId,
                    storeName = profileSnap.getString("storeName") ?: "AGEN KESAN",
                    tagline = profileSnap.getString("tagline") ?: "Tetesan berkahnya gak abis abis",
                    address = profileSnap.getString("address") ?: "Bajul Kedunglerep Modo Lamongan",
                    phone = profileSnap.getString("phone") ?: "082229831728",
                    ownerName = profileSnap.getString("ownerName") ?: "AGEN KESAN"
                )
            } else null

            val prodSnaps = userDoc.collection(COLLECTION_PRODUCTS).get().await()
            val products = prodSnaps.documents.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                Product(
                    id = id,
                    accountEmail = doc.getString("accountEmail") ?: userId,
                    name = doc.getString("name") ?: "",
                    packagingUnit = doc.getString("packagingUnit") ?: "Dus",
                    sellPrice = doc.getDouble("sellPrice") ?: 0.0,
                    orderPrice = doc.getDouble("orderPrice") ?: 0.0,
                    stock = doc.getLong("stock")?.toInt() ?: 0,
                    minStockAlert = doc.getLong("minStockAlert")?.toInt() ?: 10
                )
            }

            val saleSnaps = userDoc.collection(COLLECTION_SALES).get().await()
            val sales = saleSnaps.documents.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
                SaleTransaction(
                    id = id,
                    accountEmail = doc.getString("accountEmail") ?: userId,
                    invoiceNumber = doc.getString("invoiceNumber") ?: doc.id,
                    dateString = doc.getString("dateString") ?: "",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                    customerName = doc.getString("customerName") ?: "",
                    productId = doc.getLong("productId") ?: 0L,
                    productName = doc.getString("productName") ?: "",
                    unitPrice = doc.getDouble("unitPrice") ?: 0.0,
                    quantity = doc.getLong("quantity")?.toInt() ?: 1,
                    totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                    isDebt = doc.getBoolean("isDebt") ?: false,
                    notes = doc.getString("notes") ?: ""
                )
            }

            val orderSnaps = userDoc.collection(COLLECTION_ORDERS).get().await()
            val orders = orderSnaps.documents.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
                OrderTransaction(
                    id = id,
                    accountEmail = doc.getString("accountEmail") ?: userId,
                    orderNumber = doc.getString("orderNumber") ?: doc.id,
                    dateString = doc.getString("dateString") ?: "",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                    agentName = doc.getString("agentName") ?: "AGEN KESAN",
                    itemsSummary = doc.getString("itemsSummary") ?: "",
                    itemsJson = doc.getString("itemsJson") ?: "[]",
                    totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                    paymentStatus = doc.getString("paymentStatus") ?: "CASH",
                    status = doc.getString("status") ?: "MENUNGGU",
                    isStockAdded = doc.getBoolean("isStockAdded") ?: false,
                    notes = doc.getString("notes") ?: ""
                )
            }

            CloudDataBundle(profile, products, sales, orders)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching full cloud data: ${e.message}")
            null
        }
    }
}
