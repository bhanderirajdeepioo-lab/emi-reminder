package com.emireminder.app.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.data.db.entity.Reminder
import com.emireminder.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RemindersScreen(
    onReminderClick: (Int) -> Unit,
    onNavigateToNotificationPreview: () -> Unit = {},
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var editingReminderId by remember { mutableStateOf<Int?>(null) }
    val onAddReminder: () -> Unit = remember { { showAddSheet = true } }
    val reminders by viewModel.reminders.collectAsState()
    val today = remember { LocalDate.now() }
    val todayDay = today.dayOfMonth

    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val overdue = remember(reminders, todayDay) {
        reminders.filter { it.isActive && it.dueDayOfMonth < todayDay }.sortedBy { it.dueDayOfMonth }
    }
    val dueSoon = remember(reminders, todayDay) {
        reminders.filter { it.isActive && it.dueDayOfMonth >= todayDay && it.dueDayOfMonth - todayDay <= 7 }.sortedBy { it.dueDayOfMonth }
    }
    val upcoming = remember(reminders, todayDay) {
        reminders.filter { it.isActive && it.dueDayOfMonth >= todayDay }.sortedBy { it.dueDayOfMonth }
    }
    val done = remember(reminders) { reminders.filter { !it.isActive } }

    val filterCounts = remember(overdue, upcoming, done) {
        mapOf("All" to reminders.size, "Overdue" to overdue.size, "Due Soon" to dueSoon.size, "Paid" to done.size)
    }

    fun applyFilters(list: List<Reminder>): List<Reminder> =
        if (searchQuery.isBlank()) list
        else list.filter { it.loanName.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Indigo600, Violet600)))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 12.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reminders", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("${reminders.size} active EMI reminders", fontSize = 13.sp, color = Indigo100)
                    }
                    IconButton(onClick = onNavigateToNotificationPreview) {
                        Icon(Icons.Default.Notifications, contentDescription = "Preview notification", tint = Color.White.copy(alpha = 0.8f))
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("All", "Overdue", "Due Soon", "Paid").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        val count = filterCounts[filter] ?: 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.2f))
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (count > 0) "$filter ($count)" else filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Indigo600 else Color.White,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search loans…", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    ),
                )

                Spacer(Modifier.height(4.dp))
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
        val showOverdue  = selectedFilter == "All" || selectedFilter == "Overdue"
        val showUpcoming = selectedFilter == "All" || selectedFilter == "Due Soon"
        val showDone     = selectedFilter == "All" || selectedFilter == "Paid"

        val filteredOverdue  = applyFilters(overdue)
        val filteredUpcoming = if (selectedFilter == "Due Soon") applyFilters(dueSoon)
                               else applyFilters(upcoming)
        val filteredDone     = applyFilters(done)

        val hasAny = (showOverdue && filteredOverdue.isNotEmpty()) ||
                     (showUpcoming && filteredUpcoming.isNotEmpty()) ||
                     (showDone && filteredDone.isNotEmpty())

        if (!hasAny) {
            EmptyReminders(
                modifier = Modifier.fillMaxSize().padding(padding),
                onAdd = onAddReminder,
                isEmpty = reminders.isEmpty(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                if (showOverdue && filteredOverdue.isNotEmpty()) {
                    item {
                        SectionHeader(label = "OVERDUE", count = filteredOverdue.size, color = UrgentRed)
                    }
                    items(filteredOverdue, key = { it.id }) { r ->
                        ReminderCard(
                            reminder = r,
                            daysText = "Overdue by ${todayDay - r.dueDayOfMonth}d",
                            chipColor = UrgentRed,
                            isOverdue = true,
                            isPaid = false,
                            onClick = { r.loanId?.let { onReminderClick(it) } },
                            onDelete = { viewModel.deleteReminder(r) },
                            onEdit = { editingReminderId = r.id; showAddSheet = true },
                            onAction = { viewModel.markAsPaid(r) },
                        )
                    }
                }

                if (showUpcoming && filteredUpcoming.isNotEmpty()) {
                    item {
                        SectionHeader(
                            label = if (selectedFilter == "Due Soon") "DUE SOON" else "UPCOMING",
                            count = filteredUpcoming.size,
                            color = SafeGreen,
                        )
                    }
                    items(filteredUpcoming, key = { it.id }) { r ->
                        val daysLeft = r.dueDayOfMonth - todayDay
                        ReminderCard(
                            reminder = r,
                            daysText = if (daysLeft == 0) "Due today!" else "Due in ${daysLeft}d",
                            chipColor = if (daysLeft <= 2) WarnOrange else SafeGreen,
                            isOverdue = false,
                            isPaid = false,
                            onClick = { r.loanId?.let { onReminderClick(it) } },
                            onDelete = { viewModel.deleteReminder(r) },
                            onEdit = { editingReminderId = r.id; showAddSheet = true },
                            onAction = { viewModel.remindNow(r) },
                        )
                    }
                }

                if (showDone && filteredDone.isNotEmpty()) {
                    item {
                        SectionHeader(label = "PAID / INACTIVE", count = filteredDone.size, color = Color(0xFF94A3B8))
                    }
                    items(filteredDone, key = { it.id }) { r ->
                        ReminderCard(
                            reminder = r,
                            daysText = "Paid",
                            chipColor = SafeGreen,
                            isOverdue = false,
                            isPaid = true,
                            onClick = { r.loanId?.let { onReminderClick(it) } },
                            onDelete = { viewModel.deleteReminder(r) },
                            onEdit = { editingReminderId = r.id; showAddSheet = true },
                            onAction = { viewModel.reactivate(r) },
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddReminderSheet(
            onDismiss = {
                showAddSheet = false
                editingReminderId = null
            },
            reminderId = editingReminderId,
        )
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
    isOverdue: Boolean,
    isPaid: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onAction: () -> Unit,
) {
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val today = remember { LocalDate.now() }
    val dueDateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
    val dueDate = remember(reminder.dueDayOfMonth) {
        val day = reminder.dueDayOfMonth.coerceIn(1, today.lengthOfMonth())
        today.withDayOfMonth(day).format(dueDateFmt)
    }
    val actionLabel = when {
        isPaid    -> "COMPLETED"
        isOverdue -> "PAY NOW"
        else      -> "REMIND"
    }
    val actionColor = when {
        isPaid    -> SafeGreen
        isOverdue -> UrgentRed
        else      -> Indigo600
    }

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
                        .background(chipColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(loanEmojiFromName(reminder.loanName), fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(reminder.loanName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Due: $dueDate",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(daysText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = chipColor)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(fmt.format(reminder.emiAmount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(actionColor)
                            .clickable(onClick = onAction)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(actionLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun loanEmojiFromName(name: String): String {
    val n = name.lowercase()
    return when {
        n.contains("home") || n.contains("house") || n.contains("property") -> "🏠"
        n.contains("car") || n.contains("auto") || n.contains("vehicle")    -> "🚗"
        n.contains("bike") || n.contains("two") || n.contains("scooter")    -> "🏍"
        n.contains("personal")                                               -> "💳"
        n.contains("education") || n.contains("student")                    -> "🎓"
        n.contains("business") || n.contains("msme")                        -> "🏢"
        n.contains("gold")                                                   -> "🪙"
        n.contains("medical") || n.contains("health")                       -> "🏥"
        else                                                                 -> "💰"
    }
}

@Composable
private fun EmptyReminders(modifier: Modifier, onAdd: () -> Unit, isEmpty: Boolean = true) {
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
        Text(
            if (isEmpty) "No Reminders Yet" else "No Results",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isEmpty) "Set up reminders for your EMIs and never miss a payment due date."
            else "No reminders match the current filter or search.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )
        if (isEmpty) {
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
}

private fun dayOrdinal(day: Int) = when {
    day in 11..13 -> "th"
    day % 10 == 1 -> "st"
    day % 10 == 2 -> "nd"
    day % 10 == 3 -> "rd"
    else -> "th"
}
