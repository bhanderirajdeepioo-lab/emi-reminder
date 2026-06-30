package com.emireminder.app.ui.screens.reminders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.ui.theme.Indigo50
import com.emireminder.app.ui.theme.Indigo600

private val FREQUENCIES = listOf("Monthly", "Weekly", "Quarterly", "Yearly")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    loanId: Int,
    onBack: () -> Unit,
    viewModel: AddReminderViewModel = hiltViewModel(),
) {
    val handleDismiss = { viewModel.resetForm(); onBack() }
    var freqDropdownOpen by remember { mutableStateOf(false) }

    BackHandler(onBack = handleDismiss)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Reminder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = handleDismiss) { Icon(Icons.Default.ArrowBack, null) }
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // Loan Name
            FormLabel("Loan Name *")
            OutlinedTextField(
                value = viewModel.loanName,
                onValueChange = { viewModel.onLoanNameChange(it) },
                label = { Text("E.g. HDFC Home Loan") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
            )

            // Bank / Lender
            FormLabel("Bank / Lender")
            OutlinedTextField(
                value = viewModel.bankName,
                onValueChange = { viewModel.onBankNameChange(it) },
                label = { Text("E.g. HDFC Bank") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
            )

            // EMI Amount
            FormLabel("EMI Amount (₹) *")
            OutlinedTextField(
                value = viewModel.emiAmount,
                onValueChange = { viewModel.onEmiAmountChange(it) },
                label = { Text("E.g. 12500") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
            )

            // Due Day
            FormLabel("Due Day of Month *")
            OutlinedTextField(
                value = viewModel.dueDay,
                onValueChange = { viewModel.onDueDayChange(it) },
                label = { Text("Day (1–31)") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null, tint = Indigo600) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
            )

            // Day quick-pick chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
                    val selected = viewModel.dueDay.toIntOrNull() == day
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Indigo600 else Indigo50)
                            .clickable { viewModel.onDueDayChange(day.toString()) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "$day",
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else Indigo600,
                        )
                    }
                }
            }

            // Repeat Frequency
            FormLabel("Repeat")
            ExposedDropdownMenuBox(
                expanded = freqDropdownOpen,
                onExpandedChange = { freqDropdownOpen = it },
            ) {
                OutlinedTextField(
                    value = viewModel.repeatFrequency,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqDropdownOpen) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )
                ExposedDropdownMenu(
                    expanded = freqDropdownOpen,
                    onDismissRequest = { freqDropdownOpen = false },
                ) {
                    FREQUENCIES.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq) },
                            onClick = {
                                viewModel.onRepeatFrequencyChange(freq)
                                freqDropdownOpen = false
                            },
                        )
                    }
                }
            }

            // Notes
            FormLabel("Notes (optional)")
            OutlinedTextField(
                value = viewModel.notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = { Text("Any details about this reminder") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
            )

            // Notification toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (viewModel.notificationEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (viewModel.notificationEnabled) Indigo600 else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column {
                        Text("Notifications", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            if (viewModel.notificationEnabled) "Enabled" else "Disabled",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = viewModel.notificationEnabled,
                    onCheckedChange = { viewModel.onNotificationToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Indigo600,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    viewModel.saveReminder(onSuccess = { viewModel.resetForm(); onBack() })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                enabled = viewModel.isFormValid && !viewModel.isSaving,
            ) {
                if (viewModel.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Save Reminder", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
