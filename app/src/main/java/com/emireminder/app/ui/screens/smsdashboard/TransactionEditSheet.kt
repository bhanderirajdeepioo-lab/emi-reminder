package com.emireminder.app.ui.screens.smsdashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.domain.model.TransactionCategory
import com.emireminder.app.ui.theme.Indigo600
import com.emireminder.app.ui.theme.Slate800

private val subCategoriesFor: Map<TransactionCategory, List<String>> = mapOf(
    TransactionCategory.INCOME          to listOf("Salary", "Freelance", "Rental", "Dividend", "Refund", "Other Income"),
    TransactionCategory.EMI_AND_LOANS   to listOf("Home Loan", "Car Loan", "Personal Loan", "Education Loan", "Credit Card EMI", "Other Loan"),
    TransactionCategory.FOOD_AND_DINING to listOf("Groceries", "Restaurant", "Food Delivery", "Coffee / Tea", "Snacks"),
    TransactionCategory.TRANSPORT       to listOf("Fuel", "Auto / Taxi", "Metro / Bus", "Parking", "Flight", "Train"),
    TransactionCategory.UTILITIES       to listOf("Electricity", "Water", "Gas", "Internet", "Mobile Recharge", "Cable / DTH"),
    TransactionCategory.SHOPPING        to listOf("Clothing", "Electronics", "Home & Garden", "Books", "Online Shopping", "Other"),
    TransactionCategory.HEALTH          to listOf("Medicine", "Doctor / Clinic", "Lab Tests", "Hospital", "Gym"),
    TransactionCategory.ENTERTAINMENT   to listOf("Movies", "OTT Subscriptions", "Events", "Gaming", "Sports"),
    TransactionCategory.INVESTMENTS     to listOf("Mutual Funds", "Stocks", "SIP", "FD / RD", "Gold", "Crypto"),
    TransactionCategory.INSURANCE       to listOf("Life Insurance", "Health Insurance", "Vehicle Insurance", "Other"),
    TransactionCategory.CREDIT_CARD     to listOf("Credit Card Payment", "Credit Card Bill"),
    TransactionCategory.ATM_AND_CASH    to listOf("ATM Withdrawal", "Cash Deposit"),
    TransactionCategory.BANK_CHARGES    to listOf("Service Charge", "Interest Charge", "Penalty", "Other"),
    TransactionCategory.UNCATEGORISED   to listOf("Other"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditSheet(
    transaction: ParsedTransaction,
    isSaving: Boolean,
    saveError: String?,
    onSave: (amount: Double, category: TransactionCategory, subCategory: String?, merchantName: String?, notes: String?, lenderName: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var amountText by remember(transaction.id) { mutableStateOf(transaction.amount.toLong().toString()) }
    var selectedCategory by remember(transaction.id) { mutableStateOf(transaction.category) }
    var subCategoryText by remember(transaction.id) { mutableStateOf(transaction.subCategory ?: "") }
    var merchantText by remember(transaction.id) { mutableStateOf(transaction.merchantName ?: "") }
    var notesText by remember(transaction.id) { mutableStateOf(transaction.notes ?: "") }
    var lenderText by remember(transaction.id) { mutableStateOf("") }

    LaunchedEffect(transaction.id) {
        if (transaction.isEmi) {
            lenderText = transaction.merchantName ?: transaction.bankName
        }
    }

    var amountError by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val subCategories = subCategoriesFor[selectedCategory] ?: emptyList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                "Edit Transaction",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                transaction.bankName + if (transaction.accountLast4.isNotBlank()) " ·· ${transaction.accountLast4}" else "",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
            )

            Spacer(Modifier.height(20.dp))

            // ── Amount ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountError = false; amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount (₹)") },
                isError = amountError,
                supportingText = if (amountError) ({ Text("Enter a valid amount") }) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = sheetFieldColors(),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            // ── Category ────────────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it },
            ) {
                val catMeta = selectedCategory.meta()
                OutlinedTextField(
                    value = catMeta.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = {
                        Icon(catMeta.icon, contentDescription = null, tint = catMeta.color)
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = sheetFieldColors(),
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false },
                ) {
                    TransactionCategory.entries.forEach { cat ->
                        val meta = cat.meta()
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(meta.icon, contentDescription = null, tint = meta.color, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(meta.label)
                                }
                            },
                            onClick = {
                                selectedCategory = cat
                                subCategoryText = ""
                                categoryDropdownExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Sub-category chips ───────────────────────────────────────────
            if (subCategories.isNotEmpty()) {
                Text("Sub-category", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subCategories) { sub ->
                        val selected = sub == subCategoryText
                        FilterChip(
                            selected = selected,
                            onClick = { subCategoryText = if (selected) "" else sub },
                            label = { Text(sub, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Indigo600,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = subCategoryText,
                    onValueChange = { subCategoryText = it },
                    label = { Text("Custom sub-category (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = sheetFieldColors(),
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Merchant / Lender ────────────────────────────────────────────
            OutlinedTextField(
                value = merchantText,
                onValueChange = { merchantText = it },
                label = { Text("Merchant / Payee") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = sheetFieldColors(),
            )

            // ── Lender name (EMI only) ────────────────────────────────────────
            if (transaction.isEmi) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = lenderText,
                    onValueChange = { lenderText = it },
                    label = { Text("Lender Name") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    supportingText = { Text("Updates the linked EMI lender, not the reminder.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = sheetFieldColors(),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Notes ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Notes (optional)") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = sheetFieldColors(),
            )

            if (saveError != null) {
                Spacer(Modifier.height(8.dp))
                Text(saveError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving,
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val parsed = amountText.toDoubleOrNull()
                        if (parsed == null || parsed <= 0) {
                            amountError = true
                            return@Button
                        }
                        onSave(
                            parsed,
                            selectedCategory,
                            subCategoryText.takeIf { it.isNotBlank() },
                            merchantText.takeIf { it.isNotBlank() },
                            notesText.takeIf { it.isNotBlank() },
                            if (transaction.isEmi) lenderText.takeIf { it.isNotBlank() } else null,
                        )
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    enabled = !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun sheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Indigo600,
    unfocusedBorderColor = Color(0xFFE2E8F0),
    focusedLabelColor = Indigo600,
)
