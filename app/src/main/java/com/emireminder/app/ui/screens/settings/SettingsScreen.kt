package com.emireminder.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.emireminder.app.data.db.entity.Loan
import com.emireminder.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToSmsIntelligence: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val driveBackupState by viewModel.driveBackupState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var testSent by remember { mutableStateOf(false) }
    var showAdvanceDaysPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showNameEditor by remember { mutableStateOf(false) }

    val dateTag = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.performBackup(it) } }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.performRestore(it) } }

    LaunchedEffect(driveBackupState) {
        when (val state = driveBackupState) {
            is DriveBackupUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearDriveBackupState()
            }
            is DriveBackupUiState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
                viewModel.clearDriveBackupState()
            }
            else -> Unit
        }
    }

    val notifPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else null

    if (showNameEditor) {
        NameEditDialog(
            current = prefs.userName,
            onDismiss = { showNameEditor = false },
            onConfirm = { name ->
                viewModel.setUserName(name)
                showNameEditor = false
            },
        )
    }

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

    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            current = prefs.currency,
            onDismiss = { showCurrencyPicker = false },
            onConfirm = { currency ->
                viewModel.setCurrency(currency)
                showCurrencyPicker = false
            },
        )
    }

    if (showLanguagePicker) {
        LanguagePickerDialog(
            current = prefs.language,
            onDismiss = { showLanguagePicker = false },
            onConfirm = { displayName, tag ->
                viewModel.setLanguage(displayName)
                AppCompatDelegate.setApplicationLocales(
                    if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                    else LocaleListCompat.forLanguageTags(tag)
                )
                showLanguagePicker = false
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    .background(Brush.linearGradient(listOf(Indigo600, Violet600)))
                    .clickable { showNameEditor = true }
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
                        if (prefs.userName.isNotBlank()) {
                            val initials = prefs.userName.trim().split(" ").take(2)
                                .mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                            Text(initials, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (prefs.userName.isNotBlank()) prefs.userName else "Your Profile",
                            fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White,
                        )
                        if (prefs.userName.isNotBlank()) {
                            Text(
                                "Tap to edit your name",
                                fontSize = 13.sp, color = Color(0xFFC7D2FE),
                            )
                        } else {
                            Text(
                                "Tap to set up name & photo",
                                fontSize = 13.sp, color = Color(0xFFC7D2FE),
                            )
                            Text(
                                "EMI Reminder App",
                                fontSize = 13.sp, color = Color(0xFFC7D2FE),
                            )
                        }
                    }
                    Icon(Icons.Default.Edit, contentDescription = "Edit name", tint = Color(0xFFA5B4FC), modifier = Modifier.size(20.dp))
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
                        enabled = prefs.emiRemindersEnabled,
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
                        enabled = prefs.emiRemindersEnabled,
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
                        enabled = prefs.emiRemindersEnabled,
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
                            enabled = !testSent && (notifPermission == null || notifPermission.status.isGranted),
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
                        value = "${prefs.currencySymbol} ${prefs.currency}",
                        onClick = { showCurrencyPicker = true },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp))

                    NavigableSettingRow(
                        icon = Icons.Default.Language,
                        iconBg = Color(0xFFEFF6FF),
                        iconTint = HomeLoanColor,
                        label = "Language",
                        subtitle = "App display language",
                        value = prefs.language,
                        onClick = { showLanguagePicker = true },
                    )
                }
            }

            // SMS IMPORT section
            SettingsSectionHeader("SMS IMPORT")
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    val smsPermission = rememberPermissionState(android.Manifest.permission.READ_SMS)
                    ToggleSettingRow(
                        icon = Icons.Default.Message,
                        iconBg = Color(0xFFF0FDF4),
                        iconTint = SafeGreen,
                        label = "Auto-import from SMS",
                        subtitle = if (smsPermission.status.isGranted)
                            "Permission granted — detects bank EMI SMSes"
                        else
                            "Off by default — grant permission to enable",
                        checked = prefs.smsImportEnabled && smsPermission.status.isGranted,
                        onCheckedChange = { wantEnabled ->
                            if (wantEnabled && !smsPermission.status.isGranted) {
                                smsPermission.launchPermissionRequest()
                            } else {
                                viewModel.setSmsImportEnabled(wantEnabled)
                            }
                        },
                    )
                    if (prefs.smsImportEnabled && !smsPermission.status.isGranted) {
                        Text(
                            "READ_SMS permission not granted. Tap the toggle to request it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp))
                    NavigableSettingRow(
                        icon = Icons.Default.AutoAwesome,
                        iconBg = Color(0xFFEDE9FE),
                        iconTint = Violet600,
                        label = "SMS Intelligence",
                        subtitle = if (smsPermission.status.isGranted)
                            "Active — auto-detecting EMIs from bank SMS"
                        else
                            "Tap to set up SMS Intelligence",
                        value = null,
                        onClick = onNavigateToSmsIntelligence,
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
                        onClick = {
                            scope.launch {
                                val loans = viewModel.getActiveLoansForExport()
                                if (loans.isEmpty()) {
                                    snackbarHostState.showSnackbar("No loans to export")
                                } else {
                                    try {
                                        exportLoansCsv(context, loans)
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Export failed: ${e.message}")
                                    }
                                }
                            }
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp))

                    NavigableSettingRow(
                        icon = Icons.Default.Cloud,
                        iconBg = Color(0xFFF0FDF4),
                        iconTint = SafeGreen,
                        label = "Backup Data",
                        subtitle = "Export backup file to any storage location",
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
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
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
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
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
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled, onClick = onClick)
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

private fun String.escapeCsv(): String {
    return if (contains(',') || contains('"') || contains('\n')) {
        "\"${replace("\"", "\"\"")}\""
    } else this
}

private suspend fun exportLoansCsv(context: Context, loans: List<Loan>) {
    val file = withContext(Dispatchers.IO) {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val csv = buildString {
            appendLine("Loan Name,Type,Principal,Interest Rate,Tenure (months),EMI Amount,Due Day,Bank Name,Start Date,Status")
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            loans.forEach { loan ->
                val startDate = dateFmt.format(Date(loan.startDate))
                val status = if (loan.isActive) "Active" else "Closed"
                appendLine(
                    "${loan.name.escapeCsv()},${loan.type},${loan.principalAmount}," +
                    "${loan.interestRate},${loan.tenureMonths},${loan.emiAmount}," +
                    "${loan.emiDueDay},${loan.bankName.escapeCsv()},$startDate,$status"
                )
            }
        }
        File(context.cacheDir, "emi_reminder_export_$dateStr.csv").also { it.writeText(csv, Charsets.UTF_8) }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "EMI Reminder — Loan Export")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Loan Data"))
}

@Composable
private fun NameEditDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name") },
                placeholder = { Text("e.g. Raj Bhanderi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private data class LanguageOption(val displayName: String, val tag: String)

@Composable
private fun LanguagePickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (displayName: String, tag: String) -> Unit,
) {
    val options = remember {
        listOf(
            LanguageOption("English", ""),
            LanguageOption("हिंदी", "hi"),
            LanguageOption("ગુજરાતી", "gu"),
        )
    }
    var selected by remember { mutableStateOf(options.firstOrNull { it.displayName == current } ?: options[0]) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Language / भाषा / ભાષા") },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option }
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { selected = option },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected.displayName, selected.tag) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private data class CurrencyOption(val code: String, val symbol: String, val name: String)

@Composable
private fun CurrencyPickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val options = remember {
        listOf(
            CurrencyOption("INR", "₹", "Indian Rupee"),
            CurrencyOption("USD", "$", "US Dollar"),
            CurrencyOption("EUR", "€", "Euro"),
            CurrencyOption("GBP", "£", "British Pound"),
            CurrencyOption("AED", "د.إ", "UAE Dirham"),
            CurrencyOption("SGD", "S$", "Singapore Dollar"),
        )
    }
    var selected by remember { mutableStateOf(options.firstOrNull { it.code == current } ?: options[0]) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Currency") },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option }
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = option == selected, onClick = { selected = option })
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${option.symbol}  ${option.name} (${option.code})",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected.code) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
