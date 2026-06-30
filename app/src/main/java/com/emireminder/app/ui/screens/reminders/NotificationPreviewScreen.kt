package com.emireminder.app.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emireminder.app.ui.theme.*

@Composable
fun NotificationPreviewScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Preview", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Indigo600, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("This is how your EMI reminders appear on your device.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)

            SectionLabel("3 DAYS BEFORE DUE DATE")
            NotificationMockup(title = "EMI Due in 3 Days", body = "Your Home Loan EMI of ₹23,456 is due on 5th Jun. Ensure your account has enough balance.", time = "10:00 AM", chipColor = SafeGreen, chipText = "On Time")

            SectionLabel("1 DAY BEFORE DUE DATE")
            NotificationMockup(title = "EMI Due Tomorrow", body = "Your Home Loan EMI of ₹23,456 is due tomorrow (5th Jun). Don't miss it!", time = "09:00 AM", chipColor = WarnOrange, chipText = "Urgent")

            SectionLabel("DUE TODAY")
            NotificationMockup(title = "EMI Payment Due Today", body = "₹23,456 for Home Loan is due today. Pay now to avoid late fees.", time = "08:00 AM", chipColor = UrgentRed, chipText = "Due Today")

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Indigo50)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Notification Schedule", fontWeight = FontWeight.Bold, color = Indigo600)
                    ScheduleRow("3 days before", "10:00 AM", SafeGreen)
                    ScheduleRow("1 day before", "9:00 AM", WarnOrange)
                    ScheduleRow("On due date", "8:00 AM", UrgentRed)
                }
            }
        }
    }
}

@Composable
private fun NotificationMockup(title: String, body: String, time: String, chipColor: Color, chipText: String) {
    Box(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(Color.White)) {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Brush.horizontalGradient(listOf(Indigo600, Violet600))))
        Column(modifier = Modifier.padding(top = 4.dp).padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Indigo50), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Notifications, null, tint = Indigo600, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("EMI Reminder", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
                Text(time, fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
            Spacer(Modifier.height(10.dp))
            Text(body, fontSize = 13.sp, color = Color(0xFF475569), lineHeight = 19.sp)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(chipColor.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(chipText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = chipColor)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DISMISS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Text("PAY NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Indigo600)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
}

@Composable
private fun ScheduleRow(label: String, time: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(time, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
