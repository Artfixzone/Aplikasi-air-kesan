package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_profile")
data class StoreProfile(
    @PrimaryKey val accountEmail: String = "",
    val storeName: String = "AGEN KESAN",
    val tagline: String = "Tetesan berkahnya gak abis abis",
    val address: String = "dev by artfixzone",
    val phone: String = "dev by artfixzone",
    val ownerName: String = "AGEN KESAN"
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountEmail: String = "",
    val name: String,
    val packagingUnit: String = "Dus", // "Galon", "Dus (Isi 24)", "Dus (Isi 48)", "Botol"
    val sellPrice: Double,
    val orderPrice: Double, // Modal / Harga Beli dari Pabrik
    val stock: Int,
    val minStockAlert: Int = 10
)

@Entity(tableName = "sale_transactions")
data class SaleTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountEmail: String = "",
    val invoiceNumber: String,
    val dateString: String,
    val timestamp: Long = System.currentTimeMillis(),
    val customerName: String,
    val productId: Long,
    val productName: String,
    val unitPrice: Double,
    val quantity: Int,
    val totalPrice: Double,
    val isDebt: Boolean = false, // true = HUTANG (Tanda Merah), false = CASH
    val notes: String = ""
)

@Entity(tableName = "order_transactions")
data class OrderTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountEmail: String = "",
    val orderNumber: String,
    val dateString: String,
    val timestamp: Long = System.currentTimeMillis(),
    val agentName: String = "AGEN KESAN",
    val itemsSummary: String, // e.g., "Galon 19L (50), Dus 600ml (30)"
    val itemsJson: String, // serialized list of items for detailed calculations
    val totalAmount: Double,
    val paymentStatus: String = "CASH", // "CASH", "HUTANG PABRIK"
    val status: String = "MENUNGGU", // "MENUNGGU", "DITERIMA"
    val isStockAdded: Boolean = false, // When true, stock already added
    val notes: String = ""
)

data class OrderItemEntry(
    val productId: Long,
    val productName: String,
    val packagingUnit: String,
    val orderPrice: Double,
    var quantity: Int = 0
) {
    val subtotal: Double
        get() = orderPrice * quantity
}
