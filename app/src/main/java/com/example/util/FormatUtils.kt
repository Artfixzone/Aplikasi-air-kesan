package com.example.util

import com.example.data.model.OrderItemEntry
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val localeId = Locale("in", "ID")
    private val rupiahFormat = NumberFormat.getCurrencyInstance(localeId).apply {
        maximumFractionDigits = 0
    }

    fun formatRupiah(amount: Double): String {
        return rupiahFormat.format(amount)
    }

    fun formatRupiah(amount: Long): String {
        return rupiahFormat.format(amount)
    }

    fun getCurrentDateFormatted(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", localeId)
        return sdf.format(Date())
    }

    fun getCurrentDateSimple(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", localeId)
        return sdf.format(Date())
    }

    fun generateSaleInvoiceNumber(): String {
        val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.US)
        return "KSN-J${sdf.format(Date())}"
    }

    fun generateOrderNumber(): String {
        val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.US)
        return "ORD-PBR${sdf.format(Date())}"
    }

    fun orderItemsToJson(items: List<OrderItemEntry>): String {
        val array = JSONArray()
        for (item in items) {
            if (item.quantity > 0) {
                val obj = JSONObject()
                obj.put("productId", item.productId)
                obj.put("productName", item.productName)
                obj.put("packagingUnit", item.packagingUnit)
                obj.put("orderPrice", item.orderPrice)
                obj.put("quantity", item.quantity)
                obj.put("subtotal", item.subtotal)
                array.put(obj)
            }
        }
        return array.toString()
    }

    fun jsonToOrderItems(jsonString: String): List<OrderItemEntry> {
        val list = mutableListOf<OrderItemEntry>()
        if (jsonString.isBlank()) return list
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    OrderItemEntry(
                        productId = obj.optLong("productId", 0L),
                        productName = obj.optString("productName", "-"),
                        packagingUnit = obj.optString("packagingUnit", ""),
                        orderPrice = obj.optDouble("orderPrice", 0.0),
                        quantity = obj.optInt("quantity", 0)
                    )
                )
            }
        } catch (_: Exception) {
            // fallback
        }
        return list
    }
}
