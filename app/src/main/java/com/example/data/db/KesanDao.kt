package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.OrderTransaction
import com.example.data.model.Product
import com.example.data.model.SaleTransaction
import com.example.data.model.StoreProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface KesanDao {
    // Store Profile per account
    @Query("SELECT * FROM store_profile WHERE accountEmail = :email LIMIT 1")
    fun getStoreProfile(email: String): Flow<StoreProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoreProfile(profile: StoreProfile)

    // Products per account
    @Query("SELECT * FROM products WHERE accountEmail = :email ORDER BY id ASC")
    fun getAllProducts(email: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id AND accountEmail = :email LIMIT 1")
    suspend fun getProductById(id: Long, email: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET stock = stock + :quantityDelta WHERE id = :productId AND accountEmail = :email")
    suspend fun updateProductStock(productId: Long, email: String, quantityDelta: Int)

    @Delete
    suspend fun deleteProduct(product: Product)

    // Sale Transactions per account
    @Query("SELECT * FROM sale_transactions WHERE accountEmail = :email ORDER BY timestamp DESC")
    fun getAllSales(email: String): Flow<List<SaleTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleTransaction): Long

    @Update
    suspend fun updateSale(sale: SaleTransaction)

    @Delete
    suspend fun deleteSale(sale: SaleTransaction)

    // Order Transactions per account
    @Query("SELECT * FROM order_transactions WHERE accountEmail = :email ORDER BY timestamp DESC")
    fun getAllOrders(email: String): Flow<List<OrderTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderTransaction): Long

    @Update
    suspend fun updateOrder(order: OrderTransaction)

    @Delete
    suspend fun deleteOrder(order: OrderTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<SaleTransaction>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderTransaction>)
}
