package com.example.data.repository

import com.example.data.db.KesanDao
import com.example.data.model.OrderTransaction
import com.example.data.model.Product
import com.example.data.model.SaleTransaction
import com.example.data.model.StoreProfile
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.Flow

class KesanRepository(private val dao: KesanDao) {

    fun getStoreProfile(email: String): Flow<StoreProfile?> = dao.getStoreProfile(email)
    fun getAllProducts(email: String): Flow<List<Product>> = dao.getAllProducts(email)
    fun getAllSales(email: String): Flow<List<SaleTransaction>> = dao.getAllSales(email)
    fun getAllOrders(email: String): Flow<List<OrderTransaction>> = dao.getAllOrders(email)

    suspend fun saveStoreProfile(profile: StoreProfile) {
        dao.insertStoreProfile(profile)
    }

    suspend fun addProduct(product: Product): Long {
        return dao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        dao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        dao.deleteProduct(product)
    }

    suspend fun updateProductStock(productId: Long, email: String, delta: Int) {
        dao.updateProductStock(productId, email, delta)
    }

    suspend fun addSale(sale: SaleTransaction, autoDecrementStock: Boolean = true): Long {
        val id = dao.insertSale(sale)
        if (autoDecrementStock) {
            dao.updateProductStock(sale.productId, sale.accountEmail, -sale.quantity)
        }
        return id
    }

    suspend fun updateSale(sale: SaleTransaction) {
        dao.updateSale(sale)
    }

    suspend fun deleteSale(sale: SaleTransaction, restoreStock: Boolean = true) {
        dao.deleteSale(sale)
        if (restoreStock) {
            dao.updateProductStock(sale.productId, sale.accountEmail, sale.quantity)
        }
    }

    suspend fun addOrder(order: OrderTransaction): Long {
        return dao.insertOrder(order)
    }

    suspend fun updateOrder(order: OrderTransaction) {
        dao.updateOrder(order)
    }

    suspend fun deleteOrder(order: OrderTransaction) {
        dao.deleteOrder(order)
    }

    suspend fun confirmReceiveOrder(order: OrderTransaction) {
        if (!order.isStockAdded) {
            val items = FormatUtils.jsonToOrderItems(order.itemsJson)
            for (item in items) {
                if (item.quantity > 0 && item.productId > 0) {
                    dao.updateProductStock(item.productId, order.accountEmail, item.quantity)
                }
            }
            val updatedOrder = order.copy(
                status = "DITERIMA",
                isStockAdded = true
            )
            dao.updateOrder(updatedOrder)
        }
    }

    suspend fun restoreFromCloudBundle(bundle: CloudDataBundle) {
        bundle.profile?.let { dao.insertStoreProfile(it) }
        if (bundle.products.isNotEmpty()) dao.insertProducts(bundle.products)
        if (bundle.sales.isNotEmpty()) dao.insertSales(bundle.sales)
        if (bundle.orders.isNotEmpty()) dao.insertOrders(bundle.orders)
    }
}
