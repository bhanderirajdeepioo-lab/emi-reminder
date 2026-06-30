package com.emireminder.app.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
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
fun AddReminderSheet(
    onDismiss: () -> Unit,
    viewModel: AddReminderViewModel = hiltViewModel(),
) {
    var showDayPicker by remember { mutableStateOf(false) }
    var showRepeatDropdown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { viewModel.resetForm(); onDismiss() },
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .imePadding(),
        ) {
            // Title row with close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Add Loan Reminder",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { viewModel.resetForm(); onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Close, contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Loan Name — full width
            SheetLabel("LOAN NAME")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = viewModel.loanName,
                onValueChange = viewModel::onLoanNameChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. SBI Home Loan") },
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))

            // Bank/Lender + EMI Amount — two columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SheetLabel("BANK / LENDER")
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = viewModel.bankName,
                        onValueChange = viewModel::onBankNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("State Bank") },
                        singleLine = true,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    SheetLabel("EMI AMOUNT")
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = viewModel.emiAmount,
                        onValueChange = viewModel::onEmiAmountChange,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("32,450") },
                        prefix = { Text("₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Due Date + Repeat — two columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SheetLabel("DUE DATE")
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { showDayPicker = true }
                            .padding(horizontal = 14.dp, vertical = 15.dp),
                    ) {
                        val day = viewModel.dueDay.toIntOrNull()
                        Text(
                            if (day != null) "${day}${dayOrdinalSuffix(day)} of month"
                            else "Pick day",
                            fontSize = 13.sp,
                            color = if (day != null) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    SheetLabel("REPEAT")
                    Spacer(Modifier.height(6.dp))
                    Box {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { showRepeatDropdown = true }
                                .padding(horizontal = 14.dp, vertical = 15.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    viewModel.repeatFrequency,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.Default.ExpandMore, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showRepeatDropdown,
                            onDismissRequest = { showRepeatDropdown = false },
                        ) {
                            FREQUENCIES.forEach { freq ->
                                DropdownMenuItem(
                                    text = { Text(freq) },
                                    onClick = {
                                        viewModel.onRepeatFrequencyChange(freq)
                                        showRepeatDropdown = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Note — full width
            SheetLabel("NOTE (OPTIONAL)")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = viewModel.notes,
                onValueChange = viewModel::onNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Add a note…") },
                maxLines = 3,
            )
            Spacer(Modifier.height(16.dp))

            // Notification toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Indigo50)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Send notification reminder",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = viewModel.notificationEnabled,
                        onCheckedChange = { viewModel.onNotificationToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Indigo600,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // Save CTA
            Button(
                onClick = {
                    viewModel.saveReminder(onSuccess = { viewModel.resetForm(); onDismiss() })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                enabled = !viewModel.isSaving && viewModel.isFormValid,
            ) {
                if (viewModel.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Save Reminder", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDayPicker) {
        DayPickerDialog(
            currentDay = viewModel.dueDay.toIntOrNull() ?: 1,
            onDaySelected = { day ->
                viewModel.onDueDayChange(day.toString())
                showDayPicker = false
            },
            onDismiss = { showDayPicker = false },
        )
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.5.sp,
    )
}

@Composable
private fun DayPickerDialog(
    currentDay: Int,
    onDaySelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Due Day of Month", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(200.dp),
            ) {
                items((1..31).toList()) { day ->
                    val selected = day == currentDay
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Indigo600 else Indigo50)
                            .clickable { onDaySelected(day) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$day",
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else Indigo600,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

private fun dayOrdinalSuffix(day: Int) = when {
    day in 11..13 -> "th"
    day % 10 == 1 -> "st"
    day % 10 == 2 -> "nd"
    day % 10 == 3 -> "rd"
    else -> "th"
}
