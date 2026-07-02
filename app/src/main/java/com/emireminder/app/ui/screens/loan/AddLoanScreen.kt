package com.emireminder.app.ui.screens.loan

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
import com.emireminder.app.domain.model.LoanType
import com.emireminder.app.ui.theme.Indigo50
import com.emireminder.app.ui.theme.Indigo600
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AddLoanScreen(
    onBack: () -> Unit,
    viewModel: AddLoanViewModel = hiltViewModel(),
) {
    var saving by remember { mutableStateOf(false) }
    var typeDropdownOpen by remember { mutableStateOf(false) }
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Loan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // Loan Name
            FormField("Loan Name *") {
                OutlinedTextField(
                    value = viewModel.loanName,
                    onValueChange = { viewModel.loanName = it },
                    label = { Text("E.g. HDFC Home Loan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                )
            }

            // Loan Type
            FormField("Loan Type *") {
                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { typeDropdownOpen = true }
                            .then(
                                Modifier.padding(16.dp)
                            ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                viewModel.selectedType.displayName,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    DropdownMenu(
                        expanded = typeDropdownOpen,
                        onDismissRequest = { typeDropdownOpen = false },
                        modifier = Modifier.fillMaxWidth(0.9f),
                    ) {
                        LoanType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    viewModel.selectedType = type
                                    typeDropdownOpen = false
                                },
                            )
                        }
                    }
                }
            }

            // Bank Name
            FormField("Bank / Lender Name") {
                OutlinedTextField(
                    value = viewModel.bankName,
                    onValueChange = { viewModel.bankName = it },
                    label = { Text("E.g. HDFC Bank, SBI") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                )
            }

            // Principal Amount
            FormField("Principal Amount (₹) *") {
                OutlinedTextField(
                    value = viewModel.principal,
                    onValueChange = viewModel::onPrincipalChange,
                    label = { Text("Loan amount") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    prefix = { Text("₹") },
                )
            }

            // Interest Rate + Tenure in a row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField("Interest Rate (% p.a.)", modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = viewModel.interestRate,
                        onValueChange = viewModel::onInterestRateChange,
                        label = { Text("Rate") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        suffix = { Text("%") },
                    )
                }
                FormField("Tenure (months)", modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = viewModel.tenureMonths,
                        onValueChange = viewModel::onTenureChange,
                        label = { Text("Months") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
            }

            // Interest Calculation Type
            FormField("Interest Calculation") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("REDUCING" to "Reducing Balance", "FLAT" to "Flat Rate").forEach { (value, label) ->
                        val selected = viewModel.interestType == value
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Indigo600 else Indigo50)
                                .clickable { viewModel.interestType = value; viewModel.onEmiOverrideChange("") }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) Color.White else Indigo600,
                            )
                        }
                    }
                }
            }

            // EMI Amount (auto-calculated, editable)
            FormField("Monthly EMI (₹) *") {
                val autoEmi = viewModel.autoEmi
                if (autoEmi > 0 && viewModel.emiOverride.isBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Indigo50),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-calculated", fontSize = 11.sp, color = Indigo600)
                                Text(
                                    fmt.format(autoEmi),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Indigo600,
                                )
                            }
                            TextButton(onClick = { viewModel.onEmiOverrideChange(viewModel.displayEmi) }) {
                                Text("Edit", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = viewModel.emiOverride,
                        onValueChange = viewModel::onEmiOverrideChange,
                        label = { Text("Monthly EMI") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        prefix = { Text("₹") },
                        trailingIcon = {
                            TextButton(onClick = { viewModel.onEmiOverrideChange("") }) {
                                Text("Auto", fontSize = 12.sp, color = Indigo600)
                            }
                        },
                    )
                }
            }

            // Due Day of Month
            FormField("EMI Due Day (1–31)") {
                OutlinedTextField(
                    value = viewModel.dueDayOfMonth,
                    onValueChange = { viewModel.dueDayOfMonth = it },
                    label = { Text("Day of month") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
                        val selected = viewModel.dueDayOfMonth.toIntOrNull() == day
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Indigo600 else Indigo50)
                                .clickable { viewModel.dueDayOfMonth = day.toString() }
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
            }

            // Account Number (optional)
            FormField("Account / Loan ID (optional)") {
                OutlinedTextField(
                    value = viewModel.accountNumber,
                    onValueChange = { viewModel.accountNumber = it },
                    label = { Text("Loan account number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                )
            }

            // Notes (optional)
            FormField("Notes (optional)") {
                OutlinedTextField(
                    value = viewModel.notes,
                    onValueChange = { viewModel.notes = it },
                    label = { Text("Any additional info") },
                    modifier = Modifier.fillMaxWidth().height(88.dp),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 3,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    saving = true
                    viewModel.save(onSuccess = onBack)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                enabled = !saving && viewModel.isValid,
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save Loan", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FormField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}
