package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.OrderTransaction
import com.example.data.model.Product
import com.example.data.model.SaleTransaction
import com.example.data.model.StoreProfile
import java.io.File
import java.io.FileOutputStream

object PdfReportHelper {

    fun generateAndShareFinancialReportPdf(
        context: Context,
        profile: StoreProfile,
        periodTitle: String,
        totalSalesRevenue: Double,
        totalCashIn: Double,
        totalSalesDebt: Double,
        totalOrderExpense: Double,
        totalFactoryDebt: Double,
        estimatedProfit: Double,
        products: List<Product>,
        sales: List<SaleTransaction>
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points (72 dpi)
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val primaryColor = Color.rgb(0, 168, 107) // Kesan bright green
            val darkText = Color.rgb(15, 23, 42)
            val grayText = Color.rgb(100, 116, 139)
            val lightBg = Color.rgb(240, 253, 244)
            val debtRed = Color.rgb(220, 38, 38)

            // Header Background Banner
            paint.color = primaryColor
            canvas.drawRect(0f, 0f, 595f, 95f, paint)

            // Header Texts
            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 22f
            canvas.drawText(profile.storeName, 30f, 38f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            paint.textSize = 11f
            canvas.drawText("\"${profile.tagline}\"", 30f, 55f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10f
            canvas.drawText("${profile.address} • HP/WA: ${profile.phone}", 30f, 72f, paint)

            // Document Title
            paint.color = darkText
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 16f
            canvas.drawText("LAPORAN KEUANGAN & PENJUALAN PRODUK KESAN", 30f, 125f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10f
            paint.color = grayText
            canvas.drawText("Periode: $periodTitle | Dicetak: ${FormatUtils.getCurrentDateFormatted()}", 30f, 142f, paint)

            // Divider line
            paint.color = Color.rgb(226, 232, 240)
            paint.strokeWidth = 1.5f
            canvas.drawLine(30f, 155f, 565f, 155f, paint)

            // Summary Grid Cards
            var yPos = 175f

            // Box 1: Pemasukan Kas
            drawSummaryBox(
                canvas, 30f, yPos, 260f, 65f,
                "TOTAL PEMASUKAN KAS (CASH)",
                FormatUtils.formatRupiah(totalCashIn),
                Color.rgb(236, 253, 245),
                primaryColor
            )

            // Box 2: Pengeluaran Order Pabrik
            drawSummaryBox(
                canvas, 305f, yPos, 260f, 65f,
                "PENGELUARAN ORDER PABRIK",
                FormatUtils.formatRupiah(totalOrderExpense),
                Color.rgb(254, 242, 242),
                Color.rgb(185, 28, 28)
            )

            yPos += 75f

            // Box 3: Estimasi Laba Bersih
            drawSummaryBox(
                canvas, 30f, yPos, 260f, 65f,
                "ESTIMASI KEUNTUNGAN / LABA",
                FormatUtils.formatRupiah(estimatedProfit),
                Color.rgb(239, 246, 255),
                Color.rgb(29, 78, 216)
            )

            // Box 4: Hutang / Piutang
            drawSummaryBox(
                canvas, 305f, yPos, 260f, 65f,
                "PIUTANG JUAL / HUTANG KONSUMEN",
                FormatUtils.formatRupiah(totalSalesDebt),
                Color.rgb(254, 242, 242),
                debtRed
            )

            yPos += 75f

            // Box 5: Hutang ke Pabrik
            drawSummaryBox(
                canvas, 30f, yPos, 535f, 55f,
                "KEWAJIBAN HUTANG AGEN KE PABRIK",
                FormatUtils.formatRupiah(totalFactoryDebt),
                Color.rgb(255, 251, 235),
                Color.rgb(180, 83, 9)
            )

            yPos += 75f

            // Table Title: Peta Penjualan Produk
            paint.color = darkText
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            canvas.drawText("PETA DISTRIBUSI PENJUALAN PER JENIS PRODUK", 30f, yPos, paint)
            yPos += 15f

            // Table Header
            paint.color = Color.rgb(220, 252, 231)
            canvas.drawRect(30f, yPos, 565f, yPos + 22f, paint)

            paint.color = darkText
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            canvas.drawText("NO", 36f, yPos + 15f, paint)
            canvas.drawText("NAMA PRODUK", 65f, yPos + 15f, paint)
            canvas.drawText("KEMASAN", 230f, yPos + 15f, paint)
            canvas.drawText("TERJUAL", 320f, yPos + 15f, paint)
            canvas.drawText("SISA STOK", 390f, yPos + 15f, paint)
            canvas.drawText("TOTAL OMZET", 470f, yPos + 15f, paint)

            yPos += 22f

            // Group sales by product
            val salesByProduct = sales.groupBy { it.productId }
            var rowNum = 1

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f

            for (prod in products) {
                val prodSales = salesByProduct[prod.id] ?: emptyList()
                val totalQtySold = prodSales.sumOf { it.quantity }
                val totalRevenue = prodSales.sumOf { it.totalPrice }

                if (rowNum % 2 == 0) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(30f, yPos, 565f, yPos + 20f, paint)
                }

                paint.color = darkText
                canvas.drawText("$rowNum", 36f, yPos + 14f, paint)
                canvas.drawText(prod.name.take(28), 65f, yPos + 14f, paint)
                canvas.drawText(prod.packagingUnit, 230f, yPos + 14f, paint)
                canvas.drawText("$totalQtySold unit", 320f, yPos + 14f, paint)
                canvas.drawText("${prod.stock} unit", 390f, yPos + 14f, paint)
                canvas.drawText(FormatUtils.formatRupiah(totalRevenue), 470f, yPos + 14f, paint)

                // row border
                paint.color = Color.rgb(241, 245, 249)
                paint.strokeWidth = 0.5f
                canvas.drawLine(30f, yPos + 20f, 565f, yPos + 20f, paint)

                yPos += 20f
                rowNum++
            }

            // Footer Signatures / Notes
            yPos = 720f
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(30f, yPos, 565f, yPos + 75f, paint)

            paint.color = darkText
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            canvas.drawText("PENGESAHAN LAPORAN KEUANGAN", 45f, yPos + 22f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f
            paint.color = grayText
            canvas.drawText("Agen Resmi Air Demineral KESAN", 45f, yPos + 38f, paint)
            canvas.drawText("dev by artfixzone", 45f, yPos + 52f, paint)
            canvas.drawText("( ${profile.ownerName.ifBlank { "AGEN KESAN" }} )", 420f, yPos + 60f, paint)

            // Finish page
            pdfDocument.finishPage(page)

            // Save PDF to cache directory
            val cacheDir = File(context.cacheDir, "reports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val pdfFile = File(cacheDir, "Laporan_Keuangan_Kesan_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Share / Open PDF Intent
            val uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.kesanwater.azkqwv.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan ${profile.storeName}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Berikut terlampir Laporan Keuangan resmi ${profile.storeName} - Air Demineral KESAN."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan / Buka Laporan PDF"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawSummaryBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        bgColor: Int,
        accentColor: Int
    ) {
        val p = Paint()
        p.color = bgColor
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, p)

        p.color = accentColor
        canvas.drawRoundRect(x, y, x + 6f, y + height, 4f, 4f, p)

        p.color = Color.rgb(226, 232, 240)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1f
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, p)

        p.style = Paint.Style.FILL
        p.color = Color.rgb(100, 116, 139)
        p.textSize = 8.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, x + 15f, y + 22f, p)

        p.color = accentColor
        p.textSize = 14f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + 15f, y + 46f, p)
    }


    fun generateReceiptText(profile: StoreProfile, sale: SaleTransaction): String {
        return buildString {
            appendLine("================================")
            appendLine("       ${profile.storeName}       ")
            appendLine("   \"${profile.tagline}\"   ")
            appendLine("================================")
            appendLine("${profile.address}")
            appendLine("Telp/WA: ${profile.phone}")
            appendLine("--------------------------------")
            appendLine("No. Nota : ${sale.invoiceNumber}")
            appendLine("Tanggal  : ${sale.dateString}")
            appendLine("Pembeli  : ${sale.customerName}")
            appendLine("--------------------------------")
            appendLine("${sale.productName}")
            appendLine("  ${sale.quantity} x ${FormatUtils.formatRupiah(sale.unitPrice)} = ${FormatUtils.formatRupiah(sale.totalPrice)}")
            appendLine("--------------------------------")
            appendLine("TOTAL    : ${FormatUtils.formatRupiah(sale.totalPrice)}")
            if (sale.isDebt) {
                appendLine("STATUS   : [ BELUM LUNAS - HUTANG ]")
            } else {
                appendLine("STATUS   : [ LUNAS - CASH ]")
            }
            if (sale.notes.isNotBlank()) {
                appendLine("Catatan  : ${sale.notes}")
            }
            appendLine("================================")
            appendLine(" Terima Kasih Atas Pembelian Anda ")
            appendLine("   Air Demineral New KESAN   ")
            appendLine("================================")
        }
    }

    fun generateOrderReceiptText(profile: StoreProfile, order: OrderTransaction): String {
        val items = FormatUtils.jsonToOrderItems(order.itemsJson)
        return buildString {
            appendLine("=================================")
            appendLine("     SURAT PESANAN PABRIK       ")
            appendLine("      AIR DEMINERAL KESAN       ")
            appendLine("=================================")
            appendLine("Nama Agen : ${order.agentName}")
            appendLine("Alamat    : ${profile.address}")
            appendLine("Kontak    : ${profile.phone}")
            appendLine("No. Order : ${order.orderNumber}")
            appendLine("Tanggal   : ${order.dateString}")
            appendLine("Status    : ${order.status}")
            appendLine("Bayar     : ${order.paymentStatus}")
            appendLine("---------------------------------")
            appendLine("DETAIL ITEM ORDER:")
            for (it in items) {
                appendLine("- ${it.productName} (${it.packagingUnit})")
                appendLine("  ${it.quantity} x ${FormatUtils.formatRupiah(it.orderPrice)} = ${FormatUtils.formatRupiah(it.subtotal)}")
            }
            appendLine("---------------------------------")
            appendLine("TOTAL ORDER : ${FormatUtils.formatRupiah(order.totalAmount)}")
            if (order.notes.isNotBlank()) {
                appendLine("Catatan     : ${order.notes}")
            }
            appendLine("=================================")
            appendLine("Mohon diproses untuk pengiriman.")
            appendLine("Tetesan Berkahnya Gak Abis-Abis!")
            appendLine("=================================")
        }
    }

    fun shareTextReceipt(context: Context, title: String, text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            this.type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Cetak / Kirim Nota"))
    }
}
