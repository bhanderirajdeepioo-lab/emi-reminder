package io.helsy.emireminder.ui.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
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
import io.helsy.emireminder.data.db.entity.Loan
import io.helsy.emireminder.ui.theme.Indigo100
import io.helsy.emireminder.ui.theme.Indigo50
import io.helsy.emireminder.ui.theme.Indigo600
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AddReminderScreen(
    loanId: Int,
    onBack: () -> Unit,
    viewModel: AddReminderViewModel = hiltViewModel(),
) {
    val loans by viewModel.loans.collectAsState()
    var selectedLoan by remember(loanId, loans) {
        mutableStateOf(loans.firstOrNull { it.id == loanId } ?: loans.firstOrNull())
    }
    var loanDropdownOpen by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val handleDismiss = { viewModel.resetForm(); onBack() }
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    // Update selectedLoan when loans load
    LaunchedEffect(loans) {
        if (selectedLoan == null && loans.isNotEmpty()) {
            selectedLoan = loans.firstOrNull { it.id == loanId } ?: loans.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Reminder", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = handleDismiss) { Icon(Icons.Default.ArrowBack, null) } },
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // Loan selector
            FormLabel("Select Loan")
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable { loanDropdownOpen = true }
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (selectedLoan != null) {
                                Text(selectedLoan!!.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "${selectedLoan!!.type.capitalize()} • EMI: ${fmt.format(selectedLoan!!.emiAmount)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text("No loans added yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                DropdownMenu(
                    expanded = loanDropdownOpen,
                    onDismissRequest = { loanDropdownOpen = false },
                    modifier = Modifier.fillMaxWidth(0.9f),
                ) {
                    loans.forEach { loan ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(loan.name, fontWeight = FontWeight.Medium)
                                    Text("EMI: ${fmt.format(loan.emiAmount)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                selectedLoan = loan
                                loanDropdownOpen = false
                            },
                        )
                    }
                    if (loans.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No loans found", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {},
                            enabled = false,
                        )
                    }
                }
            }

            // EMI amount display (read-only from loan)
            if (selectedLoan != null) {
                FormLabel("EMI Amount")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Indigo50)
                        .padding(16.dp),
                ) {
                    Text(
                        fmt.format(selectedLoan!!.emiAmount),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Indigo600,
                    )
                }
            }

            // Due day of month
            FormLabel("Due Day of Month")
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

            // Day picker chips (1-31)
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

            // Notes
            FormLabel("Notes (optional)")
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("E.g. HDFC Home Loan account") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
            )

            // Summary card
            if (selectedLoan != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Indigo50),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Reminder Summary", fontWeight = FontWeight.Bold, color = Indigo600, fontSize = 13.sp)
                        SummaryRow("Loan", selectedLoan!!.name)
                        SummaryRow("EMI Amount", fmt.format(selectedLoan!!.emiAmount))
                        SummaryRow("Due every", "${dueDay.ifBlank { "—" }}${dueDay.toIntOrNull()?.let { dayOrdinal(it) } ?: ""} of month")
                        SummaryRow("Frequency", "Monthly")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    val loan = selectedLoan ?: return@Button
                    val day = dueDay.toIntOrNull() ?: return@Button
                    saving = true
                    viewModel.saveReminder(
                        loanId = loan.id,
                        loanName = loan.name,
                        emiAmount = loan.emiAmount,
                        dueDayOfMonth = day,
                        notes = notes,
                        onSuccess = onBack,
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                enabled = !saving && selectedLoan != null && dueDay.isNotBlank(),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save Reminder", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun dayOrdinal(day: Int) = when {
    day in 11..13 -> "th"
    day % 10 == 1 -> "st"
    day % 10 == 2 -> "nd"
    day % 10 == 3 -> "rd"
    else -> "th"
}

private fun String.capitalize() = replaceFirstChar { it.uppercase() }
