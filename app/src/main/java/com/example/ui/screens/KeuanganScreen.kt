package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.SaleTransaction
import com.example.data.model.StoreProfile
import com.example.ui.components.KesanHeader
import com.example.ui.components.SalesTransactionRecapCard
import com.example.ui.viewmodel.FinancePeriod
import com.example.ui.viewmodel.FinanceSummary
import com.example.util.FormatUtils
import com.example.util.PdfReportHelper
import java.util.Calendar

@Composable
fun KeuanganScreen(
    profile: StoreProfile,
    summary: FinanceSummary,
    products: List<Product>,
    sales: List<SaleTransaction>,
    onPeriodChange: (FinancePeriod) -> Unit,
    onPrintReceipt: (SaleTransaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Filter sales to match the active finance period exactly
    val periodSales = remember(sales, summary.period) {
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

        sales.filter {
            when (summary.period) {
                FinancePeriod.ALL -> true
                FinancePeriod.TODAY -> it.timestamp >= startOfToday
                FinancePeriod.THIS_MONTH -> it.timestamp >= startOfMonth
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Store Header
        KesanHeader(profile = profile)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Period Selector & PDF Export Action
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Periode Laporan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            // Pro Export PDF Button
                            Button(
                                onClick = {
                                    val periodTitle = when (summary.period) {
                                        FinancePeriod.ALL -> "Keseluruhan / Semua Waktu"
                                        FinancePeriod.THIS_MONTH -> "Bulan Ini"
                                        FinancePeriod.TODAY -> "Hari Ini"
                                    }
                                    PdfReportHelper.generateAndShareFinancialReportPdf(
                                        context = context,
                                        profile = profile,
                                        periodTitle = periodTitle,
                                        totalSalesRevenue = summary.totalSalesRevenue,
                                        totalCashIn = summary.totalCashIn,
                                        totalSalesDebt = summary.totalSalesDebt,
                                        totalOrderExpense = summary.totalOrderExpense,
                                        totalFactoryDebt = summary.totalFactoryDebt,
                                        estimatedProfit = summary.estimatedProfit,
                                        products = products,
                                        sales = periodSales
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626) // PDF Red Icon
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = summary.period == FinancePeriod.ALL,
                                onClick = { onPeriodChange(FinancePeriod.ALL) },
                                label = { Text("Semua Waktu") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = summary.period == FinancePeriod.THIS_MONTH,
                                onClick = { onPeriodChange(FinancePeriod.THIS_MONTH) },
                                label = { Text("Bulan Ini") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = summary.period == FinancePeriod.TODAY,
                                onClick = { onPeriodChange(FinancePeriod.TODAY) },
                                label = { Text("Hari Ini") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 1. BORDER BERISI LAPORAN KEUANGAN MASUK & KELUAR, KEUNTUNGAN, HUTANG JUAL, HUTANG ORDER
            item {
                FinancialBorderBox(summary = summary)
            }

            // 2. REKAP TRANSAKSI PENJUALAN
            item {
                SalesTransactionRecapCard(
                    sales = periodSales,
                    period = summary.period,
                    onPrintReceipt = onPrintReceipt
                )
            }

            // 3. PETA PENJUALAN PRODUK
            item {
                ProductSalesMapCard(summary = summary)
            }
        }
    }
}

/**
 * Border berisi laporan keuangan masuk dan keluar, keuntungan, hutang jual dan hutang order produk dari pabrik
 */
@Composable
private fun FinancialBorderBox(summary: FinanceSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF059669), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LAPORAN KEUANGAN USAHA",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46),
                            letterSpacing = 0.5.sp
                        )
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = "Laporan Pro",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Profit Box (Keuntungan)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF0FDF4)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Estimasi Keuntungan Bersih",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                        Text(
                            text = "Omzet Penjualan - Modal Produk Terjual",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Text(
                        text = FormatUtils.formatRupiah(summary.estimatedProfit),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF047857)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 1: Keuangan Masuk vs Keuangan Keluar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Keuangan Masuk (Cash)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pemasukan Kas", fontSize = 11.sp, color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatRupiah(summary.totalCashIn),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                        Text(
                            text = "Total Omzet: ${FormatUtils.formatRupiah(summary.totalSalesRevenue)}",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Keuangan Keluar (Order Pabrik)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pengeluaran Order", fontSize = 11.sp, color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatRupiah(summary.totalOrderExpense),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = "Belanja Produk Pabrik",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Hutang Jual (Piutang Konsumen) vs Hutang Order Pabrik
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Hutang Jual (Piutang belum bayar)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hutang Jual (Piutang)", fontSize = 10.5.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatRupiah(summary.totalSalesDebt),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB91C1C)
                        )
                        Text(
                            text = "Tagihan ke Pembeli",
                            fontSize = 9.5.sp,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }

                // Hutang Order Pabrik
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hutang Order Pabrik", fontSize = 10.5.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatRupiah(summary.totalFactoryDebt),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309)
                        )
                        Text(
                            text = "Kewajiban ke Pabrik",
                            fontSize = 9.5.sp,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Peta penjualan produk (Visual chart/progress breakdown of sales distribution)
 */
@Composable
private fun ProductSalesMapCard(summary: FinanceSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PETA PENJUALAN PRODUK KESAN",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1)
                        )
                    )
                }
                Text(
                    text = "${summary.distributionList.size} Produk",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Peta kontribusi volume terjual dan omzet setiap varian produk air demineral KESAN:",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (summary.distributionList.isEmpty()) {
                Text("Belum ada data penjualan", color = Color(0xFF94A3B8), fontSize = 12.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    summary.distributionList.forEachIndexed { index, item ->
                        val progressFraction = (item.percentageOfSales / 100f).coerceIn(0f, 1f)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}. ${item.product.name}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    text = "${item.quantitySold} unit (${String.format("%.1f", item.percentageOfSales)}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0284C7)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Custom Visual Progress Bar
                            LinearProgressIndicator(
                                progress = { if (summary.totalSalesRevenue > 0) progressFraction else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = when (index % 3) {
                                    0 -> Color(0xFF10B981) // Green
                                    1 -> Color(0xFF0EA5E9) // Sky
                                    else -> Color(0xFF6366F1) // Indigo
                                },
                                trackColor = Color(0xFFF1F5F9)
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Sisa stok gudang: ${item.product.stock} ${item.product.packagingUnit}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Omzet: ${FormatUtils.formatRupiah(item.totalRevenue)}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
