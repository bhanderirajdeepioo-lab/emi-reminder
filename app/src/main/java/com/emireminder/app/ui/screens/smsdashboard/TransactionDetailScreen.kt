package com.emireminder.app.ui.screens.smsdashboard

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emireminder.app.data.db.entity.ParsedTransaction
import com.emireminder.app.domain.model.TransactionDirection
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val txnAmtFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}

private fun fmtTxnAmt(amount: Double): String = txnAmtFmt.format(amount.toLong())

private fun fmtTxnDate(epochMs: Long): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · hh:mm a")
    return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(formatter)
}

private fun fmtTxnDateShort(epochMs: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(formatter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (uiState.editSheetOpen && uiState.transaction != null) {
        TransactionEditSheet(
            transaction = uiState.transaction!!,
            isSaving = uiState.editSaveInProgress,
            saveError = uiState.editSaveError,
            onSave = { amount, category, subCat, merchant, notes, lender ->
                viewModel.saveEdit(amount, category, subCat, merchant, notes, lender)
            },
            onDismiss = viewModel::dismissEditSheet,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val txn = uiState.transaction
                    if (txn != null) {
                        IconButton(onClick = {
                            val shareText = buildShareText(txn)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share transaction"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo600,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            val txn = uiState.transaction
            if (txn != null) {
                Surface(shadowElevation = 8.dp) {
                    Column(modifier = Modifier.imePadding()) {
                        BottomActionBar(
                            isVerified = txn.userVerified,
                            onToggleVerified = viewModel::toggleVerified,
                            onEdit = viewModel::openEditSheet,
                        )
                        BannerAdPlaceholder(modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }
        },
        containerColor = Indigo50,
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Indigo600)
            }

            uiState.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.error ?: "Unknown error", color = Color(0xFFEF4444), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = viewModel::retry,
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    ) { Text("Retry") }
                }
            }

            uiState.transaction == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                TransactionNotFoundContent(onBack = onBack, onRetry = viewModel::retry)
            }

            else -> {
                val txn = uiState.transaction!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    HeroSection(txn = txn)
                    Spacer(Modifier.height(16.dp))
                    DetailsCard(txn = txn, onReassign = viewModel::openEditSheet)
                    if (txn.rawSmsBody != null) {
                        Spacer(Modifier.height(12.dp))
                        ExpandableSmsSection(rawBody = txn.rawSmsBody)
                    }
                    Spacer(Modifier.height(12.dp))
                    key(txn.id, txn.notes) {
                        NotesSection(
                            initialNotes = txn.notes,
                            onSave = viewModel::saveNotes,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ─── Share text builder ────────────────────────────────────────────────────────

private fun buildShareText(txn: ParsedTransaction): String {
    val meta = txn.category.meta()
    val prefix = if (txn.direction == TransactionDirection.CREDIT) "+" else "-"
    val amtStr = "${prefix}₹${txnAmtFmt.format(txn.amount.toLong())}"
    val merchant = txn.merchantName?.takeIf { it.isNotBlank() } ?: txn.bankName
    val date = fmtTxnDateShort(txn.transactionDate)
    val account = "${txn.bankName} ···· ${txn.accountLast4}"
    return "$amtStr | ${meta.label} | $merchant | $date | $account"
}

// ─── Hero section ──────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(txn: ParsedTransaction) {
    val meta = txn.category.meta()
    val isCredit = txn.direction == TransactionDirection.CREDIT
    val prefix = if (isCredit) "+" else "-"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Indigo600)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(meta.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            meta.label.uppercase(),
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${prefix}₹${fmtTxnAmt(txn.amount)}",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isCredit) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
        )
        Spacer(Modifier.height(4.dp))
        Text(fmtTxnDate(txn.transactionDate), fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (txn.isEmi) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("EMI", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (txn.userVerified) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF16A34A).copy(alpha = 0.3f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("✓ Verified", fontSize = 10.sp, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Details card ──────────────────────────────────────────────────────────────

@Composable
private fun DetailsCard(txn: ParsedTransaction, onReassign: () -> Unit) {
    val meta = txn.category.meta()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
            Spacer(Modifier.height(8.dp))

            if (!txn.merchantName.isNullOrBlank()) {
                DetailRow(label = "Merchant", value = txn.merchantName)
                RowDivider()
            }
            DetailRow(label = "Bank", value = txn.bankName)
            RowDivider()
            DetailRow(label = "Account", value = "···· ···· ···· ${txn.accountLast4}")
            RowDivider()
            // Category with re-assign chip
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Category", fontSize = 13.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(100.dp))
                Text(
                    meta.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate800,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = onReassign,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = meta.color.copy(alpha = 0.1f),
                        contentColor = meta.color,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, meta.color.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Re-assign", fontSize = 11.sp)
                }
            }
            if (!txn.subCategory.isNullOrBlank()) {
                RowDivider()
                DetailRow(label = "Sub-category", value = txn.subCategory)
            }
            if (!txn.vpa.isNullOrBlank()) {
                RowDivider()
                DetailRow(label = "VPA", value = txn.vpa)
            }
            if (!txn.utrRef.isNullOrBlank()) {
                RowDivider()
                DetailRow(label = "UTR/Ref", value = txn.utrRef)
            }
            RowDivider()
            ConfidenceRow(score = txn.confidenceScore)
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(100.dp))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Slate800,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ConfidenceRow(score: Int) {
    val (label, barColor) = when {
        score >= 80 -> "High" to SafeGreen
        score >= 50 -> "Medium" to Amber700
        else -> "Low" to UrgentRed
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Confidence", fontSize = 13.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(100.dp))
            Text(
                "$score% — $label",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = barColor,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row {
            Spacer(Modifier.width(100.dp))
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = barColor,
                trackColor = Color(0xFFF1F5F9),
            )
        }
    }
}

// ─── Expandable SMS section ────────────────────────────────────────────────────

@Composable
private fun ExpandableSmsSection(rawBody: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Sms, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Original SMS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF92400E),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse SMS" else "Expand SMS",
                        tint = Color(0xFF94A3B8),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(300)) + fadeOut(tween(300)),
            ) {
                Column {
                    HorizontalDivider(color = Color(0xFFFDE68A))
                    Text(
                        rawBody,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF78350F),
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

// ─── Notes section ─────────────────────────────────────────────────────────────

@Composable
private fun NotesSection(initialNotes: String?, onSave: (String) -> Unit) {
    var noteText by remember { mutableStateOf(initialNotes ?: "") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notes, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Notes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = {
                    Text("Add a note…", fontSize = 13.sp, color = Color(0xFFCBD5E1))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) onSave(noteText) },
                minLines = 2,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    color = Slate800,
                    lineHeight = 20.sp,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = Indigo600,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(8.dp),
            )
        }
    }
}

// ─── Transaction not found state ──────────────────────────────────────────────

@Composable
private fun TransactionNotFoundContent(onBack: () -> Unit, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEE2E2)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Transaction Not Found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Slate800,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This transaction may have been deleted\nor is no longer available.",
            fontSize = 13.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Back to Finance", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onRetry) {
            Text("Retry", color = Indigo600, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Bottom action bar ─────────────────────────────────────────────────────────

@Composable
private fun BottomActionBar(
    isVerified: Boolean,
    onToggleVerified: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(
            checked = isVerified,
            onCheckedChange = { onToggleVerified() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SafeGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE2E8F0),
                uncheckedBorderColor = Color(0xFFCBD5E1),
            ),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "Mark Verified",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate800,
            )
            Text(
                if (isVerified) "✓ Verified" else "Not verified",
                fontSize = 11.sp,
                color = if (isVerified) SafeGreen else Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onEdit,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
