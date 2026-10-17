package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.OrderTransaction
import com.example.data.model.Product
import com.example.data.model.SaleTransaction
import com.example.data.model.StoreProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DriveUploadResult(
    val success: Boolean,
    val fileId: String? = null,
    val fileName: String,
    val message: String
)

class GoogleDriveRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "GoogleDriveRepository"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
    }

    /**
     * Serializes complete application data for the current account into a clean JSON string.
     */
    fun createBackupJson(
        accountEmail: String,
        profile: StoreProfile,
        products: List<Product>,
        sales: List<SaleTransaction>,
        orders: List<OrderTransaction>
    ): String {
        val root = JSONObject()
        val meta = JSONObject()
        meta.put("app", "AGEN KESAN - Air Demineral")
        meta.put("accountEmail", accountEmail)
        meta.put("timestamp", System.currentTimeMillis())
        meta.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("id", "ID")).format(Date()))
        meta.put("productsCount", products.size)
        meta.put("salesCount", sales.size)
        meta.put("ordersCount", orders.size)
        root.put("metadata", meta)

        // Profile
        val profObj = JSONObject()
        profObj.put("accountEmail", profile.accountEmail)
        profObj.put("storeName", profile.storeName)
        profObj.put("tagline", profile.tagline)
        profObj.put("address", profile.address)
        profObj.put("phone", profile.phone)
        profObj.put("ownerName", profile.ownerName)
        root.put("profile", profObj)

        // Products
        val prodArr = JSONArray()
        products.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("accountEmail", p.accountEmail)
            o.put("name", p.name)
            o.put("packagingUnit", p.packagingUnit)
            o.put("sellPrice", p.sellPrice)
            o.put("orderPrice", p.orderPrice)
            o.put("stock", p.stock)
            o.put("minStockAlert", p.minStockAlert)
            prodArr.put(o)
        }
        root.put("products", prodArr)

        // Sales
        val salesArr = JSONArray()
        sales.forEach { s ->
            val o = JSONObject()
            o.put("id", s.id)
            o.put("accountEmail", s.accountEmail)
            o.put("invoiceNumber", s.invoiceNumber)
            o.put("dateString", s.dateString)
            o.put("timestamp", s.timestamp)
            o.put("customerName", s.customerName)
            o.put("productId", s.productId)
            o.put("productName", s.productName)
            o.put("unitPrice", s.unitPrice)
            o.put("quantity", s.quantity)
            o.put("totalPrice", s.totalPrice)
            o.put("isDebt", s.isDebt)
            o.put("notes", s.notes)
            salesArr.put(o)
        }
        root.put("sales", salesArr)

        // Orders
        val ordersArr = JSONArray()
        orders.forEach { ord ->
            val o = JSONObject()
            o.put("id", ord.id)
            o.put("accountEmail", ord.accountEmail)
            o.put("orderNumber", ord.orderNumber)
            o.put("dateString", ord.dateString)
            o.put("timestamp", ord.timestamp)
            o.put("agentName", ord.agentName)
            o.put("itemsSummary", ord.itemsSummary)
            o.put("itemsJson", ord.itemsJson)
            o.put("totalAmount", ord.totalAmount)
            o.put("paymentStatus", ord.paymentStatus)
            o.put("status", ord.status)
            o.put("isStockAdded", ord.isStockAdded)
            o.put("notes", ord.notes)
            ordersArr.put(o)
        }
        root.put("orders", ordersArr)

        return root.toString(2)
    }

    /**
     * Uploads the backup JSON directly to Google Drive via Drive v3 REST API.
     */
    suspend fun uploadToDriveApi(
        backupJson: String,
        fileName: String,
        accessToken: String?
    ): DriveUploadResult = withContext(Dispatchers.IO) {
        if (accessToken.isNullOrBlank()) {
            return@withContext DriveUploadResult(
                success = false,
                fileName = fileName,
                message = "Token OAuth Google Drive tidak ditemukan. Gunakan Simpan ke Google Drive melalui sistem."
            )
        }

        try {
            val metadataPart = JSONObject().apply {
                put("name", fileName)
                put("mimeType", "application/json")
                put("description", "Cadangan data transaksi dan produk AGEN KESAN")
            }.toString()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    metadataPart.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    fileName,
                    backupJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url(DRIVE_UPLOAD_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val resBody = response.body?.string() ?: "{}"
                val jsonRes = JSONObject(resBody)
                val fileId = jsonRes.optString("id", "")
                DriveUploadResult(
                    success = true,
                    fileId = fileId,
                    fileName = fileName,
                    message = "Cadangan berhasil diunggah ke Google Drive (ID: $fileId)"
                )
            } else {
                val err = response.body?.string() ?: response.message
                Log.w(TAG, "Drive upload response error: $err")
                DriveUploadResult(
                    success = false,
                    fileName = fileName,
                    message = "Gagal mengunggah ke Google Drive: ${response.code}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading to Google Drive", e)
            DriveUploadResult(
                success = false,
                fileName = fileName,
                message = "Terjadi kesalahan koneksi saat upload Google Drive: ${e.message}"
            )
        }
    }

    /**
     * Saves backup JSON to internal cache and builds an Intent to upload/save directly
     * to Google Drive or other cloud storage on the device.
     */
    suspend fun saveBackupFileLocally(
        context: Context,
        backupJson: String,
        fileName: String
    ): File = withContext(Dispatchers.IO) {
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val backupFile = File(backupDir, fileName)
        FileOutputStream(backupFile).use { out ->
            out.write(backupJson.toByteArray(Charsets.UTF_8))
        }
        backupFile
    }

    fun buildGoogleDriveUploadIntent(
        context: Context,
        backupFile: File
    ): Intent {
        val uri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )
        } catch (_: Exception) {
            Uri.fromFile(backupFile)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, backupFile.name)
            putExtra(Intent.EXTRA_TEXT, "File Cadangan Data AGEN KESAN - ${backupFile.name}")
            // Prefer Google Drive if installed
            setPackage("com.google.android.apps.docs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Fallback to chooser if package is not explicitly resolved
        return if (intent.resolveActivity(context.packageManager) != null) {
            intent
        } else {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, backupFile.name)
                putExtra(Intent.EXTRA_TEXT, "File Cadangan Data AGEN KESAN - ${backupFile.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(fallback, "Upload Data ke Google Drive")
        }
    }
}
