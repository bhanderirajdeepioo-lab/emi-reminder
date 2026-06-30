package com.emireminder.app.ui.screens.sms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.emireminder.app.sms.BankSource
import com.emireminder.app.sms.SmsParseResult
import com.emireminder.app.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SMSImportScreen(
    onBack: () -> Unit,
    viewModel: SMSImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val smsPermission = rememberPermissionState(android.Manifest.permission.READ_SMS)

    val matched = (state as? SmsImportState.Results)?.items?.filter { it.isConfident } ?: emptyList()
    val uncertain = (state as? SmsImportState.Results)?.items?.filter { it.isUncertain } ?: emptyList()
    val importedCount = (state as? SmsImportState.Results)?.importedCount ?: 0
    val isResults = state is SmsImportState.Results

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Indigo600, Violet600)))
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Text("SMS Import", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                if (isResults) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusChip("${matched.size} Matched", SafeGreen)
                        StatusChip("${uncertain.size} Uncertain", WarnOrange)
                    }
                }
            }
        },
    ) { padding ->
        when {
            !smsPermission.status.isGranted ->
                PermissionRequest(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onRequest = { smsPermission.launchPermissionRequest() },
                )

            state is SmsImportState.Scanning ->
                ScanningIndicator(modifier = Modifier.fillMaxSize().padding(padding))

            state is SmsImportState.Error ->
                ErrorState(
                    message = (state as SmsImportState.Error).message,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onRetry = { viewModel.reset() },
                )

            !isResults ->
                ScanPrompt(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onScan = { viewModel.scanSmsInbox() },
                )

            else -> {
                val results = state as SmsImportState.Results
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    if (matched.isNotEmpty()) {
                        item { SectionHeader("MATCHED EMIs", matched.size, SafeGreen) }
                        items(matched, key = { it.rawBody.hashCode().toLong() * 31 + it.bank.ordinal }) { result ->
                            SmsCard(
                                result = result,
                                onImport = { viewModel.importResult(result) },
                                onDismiss = { viewModel.dismissResult(result) },
                            )
                        }
                    }
                    if (uncertain.isNotEmpty()) {
                        item { SectionHeader("NEEDS REVIEW", uncertain.size, WarnOrange) }
                        items(uncertain, key = { it.rawBody.hashCode().toLong() * 31 + it.bank.ordinal + 1000L }) { result ->
                            SmsCard(
                                result = result,
                                onImport = { viewModel.importResult(result) },
                                onDismiss = { viewModel.dismissResult(result) },
                            )
                        }
                    }
                    if (results.items.isEmpty() && importedCount == 0) {
                        item { EmptyResults(Modifier.fillParentMaxSize()) }
                    }
                    if (importedCount > 0) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = SafeGreen, modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "$importedCount loan${if (importedCount > 1) "s" else ""} imported!",
                                        fontWeight = FontWeight.SemiBold,
                                        color = SafeGreen,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmsCard(
    result: SmsParseResult,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val stateColor = when {
        result.isConfident -> SafeGreen
        result.isUncertain -> WarnOrange
        else               -> Color(0xFF94A3B8)
    }
    val bankLabel = when (result.bank) {
        BankSource.HDFC    -> "HDFC Bank"
        BankSource.SBI     -> "State Bank of India"
        BankSource.ICICI   -> "ICICI Bank"
        BankSource.GENERIC -> "Bank (generic)"
        BankSource.UNKNOWN -> null
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.clip(CircleShape).background(stateColor.copy(alpha = 0.1f)).padding(6.dp),
                ) {
                    Icon(Icons.Default.Message, null, tint = stateColor, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(result.senderAddress.ifEmpty { result.bank.name }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                if (result.emiAmount > 0) {
                    Text(
                        "₹${"%,.0f".format(result.emiAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        color = stateColor,
                        fontSize = 15.sp,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                result.rawBody,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )
            if (bankLabel != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, null, tint = Indigo600, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$bankLabel detected", fontSize = 12.sp, color = Indigo600, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onImport,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    contentPadding = PaddingValues(0.dp),
                ) { Text("Import", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) { Text("Dismiss", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.5.sp)
        Spacer(Modifier.weight(1f))
        Text("$count", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun PermissionRequest(modifier: Modifier, onRequest: () -> Unit) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Indigo50), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Message, null, tint = Indigo600, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("SMS Permission Required", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Allow SMS access to automatically detect your EMI loans from bank messages.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
        ) {
            Icon(Icons.Default.Lock, null)
            Spacer(Modifier.width(8.dp))
            Text("Grant SMS Permission", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ScanPrompt(modifier: Modifier, onScan: () -> Unit) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Indigo50), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Search, null, tint = Indigo600, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Scan Bank Messages", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "We'll analyze your SMS inbox to find EMI-related bank messages and import them automatically.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onScan,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
        ) {
            Icon(Icons.Default.Search, null)
            Spacer(Modifier.width(8.dp))
            Text("Scan SMS Messages", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ScanningIndicator(modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = Indigo600, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            "Scanning messages…",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier, onRetry: () -> Unit) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry) { Text("Try Again") }
    }
}

@Composable
private fun EmptyResults(modifier: Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CheckCircle, null, tint = SafeGreen, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "All done! All messages have been imported or dismissed.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
