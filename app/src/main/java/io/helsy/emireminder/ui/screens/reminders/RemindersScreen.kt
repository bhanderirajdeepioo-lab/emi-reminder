package io.helsy.emireminder.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.helsy.emireminder.data.db.entity.Reminder
import io.helsy.emireminder.ui.theme.*
import java.time.LocalDate
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RemindersScreen(
    onAddReminder: () -> Unit,
    onReminderClick: (Int) -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val reminders by viewModel.reminders.collectAsState()
    val today = remember { LocalDate.now() }
    val todayDay = today.dayOfMonth

    val upcoming = remember(reminders, todayDay) {
        reminders.filter { it.isActive && it.dueDayOfMonth >= todayDay }.sortedBy { it.dueDayOfMonth }
    }
    val overdue = remember(reminders, todayDay) {
        reminders.filter { it.isActive && it.dueDayOfMonth < todayDay }.sortedBy { it.dueDayOfMonth }
    }
    val done = remember(reminders) { reminders.filter { !it.isActive } }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Indigo600, Violet600)))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text("Reminders", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("${reminders.size} active EMI reminders", fontSize = 13.sp, color = Indigo100)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddReminder,
                containerColor = Indigo600,
                shape = CircleShape,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (reminders.isEmpty()) {
            EmptyReminders(
                modifier = Modifier.fillMaxSize().padding(padding),
                onAdd = onAddReminder,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                if (overdue.isNotEmpty()) {
                    item {
                        SectionHeader(
                            label = "OVERDUE",
                            count = overdue.size,
                            color = UrgentRed,
                        )
                    }
                    items(overdue, key = { it.id }) { r ->
                        ReminderCard(
                            reminder = r,
                            daysText = "Overdue by ${todayDay - r.dueDayOfMonth}d",
                            chipColor = UrgentRed,
                            onClick = { onReminderClick(r.loanId) },
                            onDelete = { viewModel.deleteReminder(r) },
                        )
                    }
                }

                if (upcoming.isNotEmpty()) {
                    item {
                        SectionHeader(
                            label = "UPCOMING",
                            count = upcoming.size,
                            color = SafeGreen,
                        )
                    }
                    items(upcoming, key = { it.id }) { r ->
                        val daysLeft = r.dueDayOfMonth - todayDay
                        ReminderCard(
                            reminder = r,
                            daysText = if (daysLeft == 0) "Due today!" else "Due in ${daysLeft}d",
                            chipColor = if (daysLeft <= 2) WarnOrange else SafeGreen,
                            onClick = { onReminderClick(r.loanId) },
                            onDelete = { viewModel.deleteReminder(r) },
                        )
                    }
                }

                if (done.isNotEmpty()) {
                    item {
                        SectionHeader(
                            label = "PAID / INACTIVE",
                            count = done.size,
                            color = Color(0xFF94A3B8),
                        )
                    }
                    items(done, key = { it.id }) { r ->
                        ReminderCard(
                            reminder = r,
                            daysText = "Paid",
                            chipColor = Color(0xFF94A3B8),
                            onClick = { onReminderClick(r.loanId) },
                            onDelete = { viewModel.deleteReminder(r) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(width = 3.dp, height = 14.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.5.sp)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text("$count", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    daysText: String,
    chipColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(chipColor),
            )
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Indigo50),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (reminder.isActive) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (reminder.isActive) Indigo600 else Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(reminder.loanName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Due on ${reminder.dueDayOfMonth}${dayOrdinal(reminder.dueDayOfMonth)} every month",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(fmt.format(reminder.emiAmount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(chipColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(daysText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = chipColor)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyReminders(modifier: Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Indigo50),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Indigo600, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("No Reminders Yet", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(
            "Set up reminders for your EMIs and never miss a payment due date.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add First Reminder", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun dayOrdinal(day: Int) = when {
    day in 11..13 -> "th"
    day % 10 == 1 -> "st"
    day % 10 == 2 -> "nd"
    day % 10 == 3 -> "rd"
    else -> "th"
}
