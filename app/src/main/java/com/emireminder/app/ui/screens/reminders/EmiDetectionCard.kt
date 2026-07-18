package com.emireminder.app.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emireminder.app.data.db.entity.AutoDetectedEmi
import com.emireminder.app.ui.theme.Indigo50
import com.emireminder.app.ui.theme.Indigo600
import java.text.NumberFormat
import java.util.Locale

@Composable
fun EmiDetectionCard(
    emi: AutoDetectedEmi,
    isUpdate: Boolean,
    previousAmount: Double?,
    currencySymbol: String,
    onAddReminder: () -> Unit,
    onDismiss: () -> Unit,
) {
    val numFmt = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }
    val cardBg = if (isUpdate) Color(0xFFFFFBEB) else Color(0xFFEEF2FF)
    val accentColor = if (isUpdate) Color(0xFFD97706) else Indigo600
    val iconBg = if (isUpdate) Color(0xFFFEF3C7) else Indigo50

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isUpdate) "EMI amount changed" else "New EMI detected",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        letterSpacing = 0.3.sp,
                    )
                    val amountStr = "$currencySymbol${numFmt.format(emi.emiAmount)}/mo"
                    Text(
                        if (isUpdate && previousAmount != null) {
                            "Your ${emi.lenderName} EMI: $currencySymbol${numFmt.format(previousAmount)} → $amountStr"
                        } else {
                            "New loan: ${emi.lenderName} $amountStr"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text("Dismiss", fontSize = 13.sp)
                }
                Button(
                    onClick = onAddReminder,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                ) {
                    Text(
                        if (isUpdate) "Update" else "Add Reminder",
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun EmiDetectionAdBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F5F9)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.Campaign,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(18.dp),
            )
            Text(
                "Advertisement",
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
