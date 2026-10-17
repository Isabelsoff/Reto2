package com.isa.reto2.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isa.reto2.data.TransactionEntity
import com.isa.reto2.ui.theme.AuditRed
import com.isa.reto2.ui.theme.AuditRedBg
import com.isa.reto2.ui.theme.CardGray
import com.isa.reto2.ui.theme.CardText
import com.isa.reto2.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(ts: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts))

fun money(v: Double): String = String.format(Locale.US, "%,.2f", v)

@Composable
fun TransactionCard(tx: TransactionEntity, email: String? = null, adminView: Boolean = false) {
    val flagged = tx.requiresAudit
    val txPhoto by produceState<ImageBitmap?>(initialValue = null, tx.photoPath) {
        val path = tx.photoPath
        value = if (path == null) null else withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (adminView && flagged) AuditRedBg else CardGray,
            contentColor = CardText
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${tx.transactionType} · ${tx.currencyPair}", fontWeight = FontWeight.Bold)
                Text(
                    if (flagged) "AUDITORÍA" else "SUCCESS",
                    color = if (flagged) AuditRed else SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            if (email != null) Text("Usuario: $email")
            Text("Monto: ${money(tx.amount)} USD")
            Text("Tasa: ${String.format(Locale.US, "%,.4f", tx.currentExchangeRate)}")
            if (adminView) Text("Desviación: ${String.format(Locale.US, "%.2f", tx.deviationPercent)} %")
            Text(formatDate(tx.timestamp), color = Color(0xFF5F6368))

            val img = txPhoto
            if (img != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Image(
                        bitmap = img,
                        contentDescription = "Foto adjunta",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            if (adminView && flagged) {
                Text("⚠ Requiere auditoría", color = AuditRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}
