package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SaleTransaction
import com.example.ui.viewmodel.FinancePeriod
import com.example.util.FormatUtils

private enum class SalesRecapFilter {
    ALL, CASH_ONLY, DEBT_ONLY
}

@Composable
fun SalesTransactionRecapCard(
    sales: List<SaleTransaction>,
    period: FinancePeriod,
    onPrintReceipt: (SaleTransaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var filterType by remember { mutableStateOf(SalesRecapFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isExpandedList by remember { mutableStateOf(false) }

    // Summary calculations for current period sales
    val totalRevenue = remember(sales) { sales.sumOf { it.totalPrice } }
    val totalUnitsSold = remember(sales) { sales.sumOf { it.quantity } }
    val cashSales = remember(sales) { sales.filter { !it.isDebt } }
    val debtSales = remember(sales) { sales.filter { it.isDebt } }
    val totalCashRevenue = remember(cashSales) { cashSales.sumOf { it.totalPrice } }
    val totalDebtRevenue = remember(debtSales) { debtSales.sumOf { it.totalPrice } }
    val avgPerTrx = remember(sales, totalRevenue) {
        if (sales.isNotEmpty()) totalRevenue / sales.size else 0.0
    }

    // Top 3 customers in this period
    val topCustomers = remember(sales) {
        sales.groupBy { it.customerName.trim().ifBlank { "Pelanggan Umum" } }
            .map { (name, trxList) ->
                Triple(name, trxList.sumOf { it.totalPrice }, trxList.size)
            }
            .sortedByDescending { it.second }
            .take(3)
    }

    // Filtered transaction list
    val filteredList = remember(sales, filterType, searchQuery) {
        sales.filter { sale ->
            val matchFilter = when (filterType) {
                SalesRecapFilter.ALL -> true
                SalesRecapFilter.CASH_ONLY -> !sale.isDebt
                SalesRecapFilter.DEBT_ONLY -> sale.isDebt
            }
            val matchQuery = searchQuery.isBlank() ||
                    sale.customerName.contains(searchQuery, ignoreCase = true) ||
                    sale.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    sale.productName.contains(searchQuery, ignoreCase = true) ||
                    sale.notes.contains(searchQuery, ignoreCase = true)
            matchFilter && matchQuery
        }
    }

    val periodLabel = when (period) {
        FinancePeriod.ALL -> "Semua Waktu"
        FinancePeriod.THIS_MONTH -> "Bulan Ini"
        FinancePeriod.TODAY -> "Hari Ini"
    }

    Card(
        modifier = modifier
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
            // Header: Title & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFECFDF5), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "REKAP TRANSAKSI PENJUALAN",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "Rekapitulasi omzet & penjualan: $periodLabel",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                ) {
                    Text(
                        text = "${sales.size} Trx",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Highlight Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Total Omzet Penjualan
                Surface(
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Total Omzet",
                            fontSize = 10.5.sp,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FormatUtils.formatRupiah(totalRevenue),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857)
                        )
                        Text(
                            text = "$totalUnitsSold unit terjual",
                            fontSize = 9.5.sp,
                            color = Color(0xFF065F46)
                        )
                    }
                }

                // Card 2: Penjualan Tunai / Lunas
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF059669), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tunai (${cashSales.size})",
                                fontSize = 10.5.sp,
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FormatUtils.formatRupiah(totalCashRevenue),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                        Text(
                            text = "Sudah lunas",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Card 3: Penjualan Tempo / Hutang
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFFDC2626), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tempo (${debtSales.size})",
                                fontSize = 10.5.sp,
                                color = Color(0xFFB91C1C),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FormatUtils.formatRupiah(totalDebtRevenue),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                        Text(
                            text = "Belum bayar",
                            fontSize = 9.sp,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Top Customers Section (If any sales exist)
            if (topCustomers.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Pelanggan Teraktif ($periodLabel)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Text(
                                text = "Rata-rata: ${FormatUtils.formatRupiah(avgPerTrx)}/nota",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            topCustomers.forEachIndexed { idx, (custName, amt, count) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text(
                                            text = "#${idx + 1} $custName",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = FormatUtils.formatRupiah(amt),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "$count transaksi",
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Search Bar for transactions
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Cari nama pembeli, nota, produk...",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-Filter Chips: Semua, Tunai, Tempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterType == SalesRecapFilter.ALL,
                    onClick = { filterType = SalesRecapFilter.ALL },
                    label = { Text("Semua (${sales.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = filterType == SalesRecapFilter.CASH_ONLY,
                    onClick = { filterType = SalesRecapFilter.CASH_ONLY },
                    label = { Text("Tunai (${cashSales.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF059669),
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = filterType == SalesRecapFilter.DEBT_ONLY,
                    onClick = { filterType = SalesRecapFilter.DEBT_ONLY },
                    label = { Text("Tempo (${debtSales.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFDC2626),
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transaction Items List
            if (filteredList.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Tidak ada transaksi yang cocok dengan pencarian"
                            else "Belum ada transaksi penjualan pada periode $periodLabel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                // Determine items to display (compact with expand/collapse)
                val displayList = if (isExpandedList) filteredList else filteredList.take(5)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayList.forEach { sale ->
                        RecapSaleItemRow(
                            sale = sale,
                            onPrintReceipt = { onPrintReceipt(sale) }
                        )
                    }

                    // Show expand/collapse toggle if total filtered items > 5
                    if (filteredList.size > 5) {
                        OutlinedButton(
                            onClick = { isExpandedList = !isExpandedList },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpandedList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isExpandedList) "Tampilkan Lebih Sedikit (5 Nota)"
                                else "Lihat Semua (${filteredList.size} Nota)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecapSaleItemRow(
    sale: SaleTransaction,
    onPrintReceipt: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Invoice No & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sale.invoiceNumber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )

                Text(
                    text = sale.dateString,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Customer Name & Total Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sale.customerName.ifBlank { "Pelanggan Umum" },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1
                    )
                }

                Text(
                    text = FormatUtils.formatRupiah(sale.totalPrice),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (sale.isDebt) Color(0xFFDC2626) else Color(0xFF059669)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 3: Product Detail & Payment Status Badge + Struk Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${sale.productName} • ${sale.quantity} unit",
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Payment Status Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (sale.isDebt) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                        border = BorderStroke(
                            1.dp,
                            if (sale.isDebt) Color(0xFFFECACA) else Color(0xFFA7F3D0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (sale.isDebt) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (sale.isDebt) Color(0xFFDC2626) else Color(0xFF059669),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (sale.isDebt) "TEMPO" else "LUNAS",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sale.isDebt) Color(0xFFB91C1C) else Color(0xFF047857)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Struk preview / print button
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.clickable { onPrintReceipt() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Cetak Struk",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Struk",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Optional notes
            if (sale.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ket: ${sale.notes}",
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
