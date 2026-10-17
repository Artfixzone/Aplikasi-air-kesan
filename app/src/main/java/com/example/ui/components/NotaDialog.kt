package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.SaleTransaction
import com.example.data.model.StoreProfile
import com.example.util.FormatUtils
import com.example.util.PdfReportHelper

@Composable
fun NotaDialog(
    sale: SaleTransaction,
    profile: StoreProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Struk / Nota Penjualan",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Printable Receipt Canvas Container
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                    color = Color(0xFFFAFAFA),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store Logo
                        Image(
                            painter = painterResource(id = R.drawable.img_kesan_logo),
                            contentDescription = "Logo KESAN",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = profile.storeName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        )
                        Text(
                            text = "\"${profile.tagline}\"",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFF059669)
                            )
                        )
                        Text(
                            text = profile.address,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        )
                        Text(
                            text = "HP/WA: ${profile.phone}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color(0xFFCBD5E1)
                        )

                        // Meta row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("No. Nota:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(sale.invoiceNumber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tanggal:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(sale.dateString, fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pembeli:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(sale.customerName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color(0xFFCBD5E1)
                        )

                        // Product item row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sale.productName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${sale.quantity} x ${FormatUtils.formatRupiah(sale.unitPrice)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Text(
                                text = FormatUtils.formatRupiah(sale.totalPrice),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color(0xFFCBD5E1)
                        )

                        // Total and Payment Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL BAYAR",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = FormatUtils.formatRupiah(sale.totalPrice),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (sale.isDebt) Color(0xFFFEE2E2) else Color(0xFFD1FAE5),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (sale.isDebt) Color(0xFFDC2626) else Color(0xFF10B981),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (sale.isDebt) "STATUS: BELUM LUNAS (HUTANG)" else "STATUS: LUNAS (CASH)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (sale.isDebt) Color(0xFFB91C1C) else Color(0xFF065F46)
                            )
                        }

                        if (sale.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Catatan: ${sale.notes}",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontStyle = FontStyle.Italic
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "~ Tetesan berkahnya gak abis abis ~",
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF047857)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Share Nota / Cetak
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tutup")
                    }

                    Button(
                        onClick = {
                            val receiptText = PdfReportHelper.generateReceiptText(profile, sale)
                            PdfReportHelper.shareTextReceipt(
                                context = context,
                                title = "Nota ${sale.invoiceNumber} - ${profile.storeName}",
                                text = receiptText
                            )
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak / Kirim Nota", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
