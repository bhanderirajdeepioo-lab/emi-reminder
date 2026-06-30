package io.helsy.emireminder.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import io.helsy.emireminder.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    var testSent by remember { mutableStateOf(false) }
    var showAdvanceDaysPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    val notifPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else null

    if (showAdvanceDaysPicker) {
        AdvanceDaysDialog(
            current = prefs.advanceReminderDays,
            onDismiss = { showAdvanceDaysPicker = false },
            onConfirm = { days ->
                viewModel.setAdvanceReminderDays(days)
                showAdvanceDaysPicker = false
            },
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            hour = prefs.reminderTimeHour,
            minute = prefs.reminderTimeMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                viewModel.setReminderTime(h, m)
                showTimePicker = false
            },
        )
    }

    if (showThemePicker) {
        ThemePickerDialog(
            current = prefs.theme,
            onDismiss = { showThemePicker = false },
            onConfirm = { theme ->
                viewModel.setTheme(theme)
                showThemePicker = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo600,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Profile card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Indigo600, Violet600))
                    )
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF312E81)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFA5B4FC),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Your Profile", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Tap to set up name & photo", fontSize = 13.sp, color = Color(0xFFC7D2FE))
                        Text("EMI Reminder App", fontSize = 11.sp, color = Color(0xFFA5B4FC))
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF312E81)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit profile",
                            tint = Color(0xFFE0E7FF), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // NOTIFICATIONS section
            SettingsSectionHeader("NOTIFICATIONS")
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    if (notifPermission != null && !notifPermission.status.isGranted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { notifPermission.launchPermissionRequest() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Notifications, null,
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Grant Notification Permission", fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("Required for EMI reminders", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                            Icon(Icons.Default.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 60.dp))
                    }

                    ToggleSettingRow(
                        icon = Icons.Default.Notifications,
                        iconBg = Indigo50,
                        iconTint = Indigo600,
                        label = "EMI Reminders",
                        subtitle = "Notify before EMI due date",
                        checked = prefs.emiRemindersEnabled,
                        onCheckedChange = { viewModel.setEmiRemindersEnabled(it) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp))

                    NavigableSettingRow(
                        icon = Icons.Default.DateRange,
                        iconBg = Color(0xFFF0FDF4),
                        iconTint = SafeGreen,
                        label = "Advance Reminder",
                        subtitle = "Days before due date",
                        value = "${prefs.advanceReminderDays} days",
                        onClick = { showAdvanceDaysPicker = true },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp))

                    NavigableSettingRow(
                        icon = Icons.Default.Schedule,
                        iconBg = Color(0xFFFFF7ED),
                        iconTint = WarnOrange,
                        label = "Reminder Time",
                        subtitle = "When to send notification",
                        value = "%02d:%02d %s".format(
                            if (prefs.reminderTimeHour % 12 == 0) 12 else prefs.reminderTimeHour % 12,
                            prefs.reminderTimeMinute,
                            if (prefs.reminderTimeHour < 12) "AM" else "PM",
                        ),
                        onClick = { showTimePicker = true },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp))

                    ToggleSettingRow(
                        icon = Icons.Default.Warning,
                        iconBg = Color(0xFFFEF2F2),
                        iconTint = UrgentRed,
                        label = "Overdue Alerts",
                        subtitle = "Alert if payment missed",
                        checked = prefs.overdueAlertsEnabled,
                        onCheckedChange = { viewModel.setOverdueAlertsEnabled(it) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp))

                    // Test notification button
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Button(
                            onClick = {
                                viewModel.sendTestNotification()
                                testSent = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !testSent,
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        ) {
                            Icon(Icons.Default.Notifications, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (testSent) "Test sent — check in ~5s" else "Send Test Notification")
                        }
                    }
                    if (testSent) {
                        Text(
                            "A test notification will appear within 5 seconds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        TextButton(
                            onClick = { testSent = false },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) { Text("Reset") }
                    }
                }
            }

            // APPEARANCE section
            SettingsSectionHeader("APPEARANCE")
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    NavigableSettingRow(
                        icon = Icons.Default.Palette,
                        iconBg = Color(0xFFF3E8FF),
                        iconTint = Violet600,
                        label = "Theme",
                        subtitle = "Colour scheme",
                        value = prefs.theme,
                        onClick = { showThemePicker = true },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp))

                    NavigableSettingRow(
                        icon = Icons.Default.AttachMoney,
                        iconBg = Color(0xFFF0FDF4),
                        iconTint = SafeGreen,
                        label = "Currency",
                        subtitle = "Symbol shown in amounts",
                        value = "₹ ${prefs.currency}",
                        onClick = { /* future */ },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp))

                    NavigableSettingRow(
                        icon = Icons.Default.Language,
                        iconBg = Color(0xFFEFF6FF),
                        iconTint = HomeLoanColor,
                        label = "Language",
                        subtitle = "App display language",
                        value = prefs.language,
                        onClick = { /* future */ },
                    )
                }
            }

            // DATA & BACKUP section
            SettingsSectionHeader("DATA & BACKUP")
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    NavigableSettingRow(
                        icon = Icons.Default.Upload,
                        iconBg = Color(0xFFEFF6FF),
                        iconTint = HomeLoanColor,
                        label = "Export data (CSV)",
                        subtitle = "Download all loan data",
                        value = null,
                        onClick = { /* future */ },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp))

                    NavigableSettingRow(
                        icon = Icons.Default.Cloud,
                        iconBg = Color(0xFFF0FDF4),
                        iconTint = SafeGreen,
                        label = "Backup to Google Drive",
                        subtitle = "Auto-backup enabled",
                        value = "Backup",
                        onClick = { /* future */ },
                    )
                }
            }

            // App version
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("App Version", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
                Text("1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Indigo600,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun ToggleSettingRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Indigo600),
        )
    }
}

@Composable
private fun NavigableSettingRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    subtitle: String,
    value: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (value != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = "$value ›",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        } else {
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdvanceDaysDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var days by remember { mutableIntStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advance Reminder Days") },
        text = {
            Column {
                Text("Notify this many days before due date:")
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { if (days > 1) days-- }) {
                        Icon(Icons.Default.Remove, null)
                    }
                    Text(
                        "$days days", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    IconButton(onClick = { if (days < 30) days++ }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(days) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TimePickerDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var h by remember { mutableIntStateOf(hour) }
    var m by remember { mutableIntStateOf(minute) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder Time") },
        text = {
            Column {
                Text("Set the daily notification time:")
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { h = (h + 1) % 24 }) { Icon(Icons.Default.KeyboardArrowUp, null) }
                        Text("%02d".format(h), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { h = (h - 1 + 24) % 24 }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                    }
                    Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { m = (m + 15) % 60 }) { Icon(Icons.Default.KeyboardArrowUp, null) }
                        Text("%02d".format(m), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { m = (m - 15 + 60) % 60 }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (h < 12) "AM" else "PM", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        color = Indigo600)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(h, m) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ThemePickerDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Theme") },
        text = {
            Column {
                listOf("System", "Light", "Dark").forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selected = option }
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = option == selected, onClick = { selected = option })
                        Spacer(Modifier.width(8.dp))
                        Text(option, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
