package io.helsy.emireminder.ui.screens.sms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import io.helsy.emireminder.sms.BankSource
import io.helsy.emireminder.sms.SmsParseResult

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SMSImportScreen(
    onBack: () -> Unit,
    viewModel: SMSImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val smsPermission = rememberPermissionState(android.Manifest.permission.READ_SMS)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Import", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            when (val s = state) {
                is SmsImportState.Idle -> IdleContent(
                    hasPermission = smsPermission.status.isGranted,
                    onScan = {
                        if (smsPermission.status.isGranted) viewModel.scanSmsInbox()
                        else smsPermission.launchPermissionRequest()
                    },
                )
                is SmsImportState.Scanning -> ScanningContent()
                is SmsImportState.Results  -> ResultsContent(
                    items = s.items,
                    onImport = viewModel::importResult,
                    onReset = viewModel::reset,
                )
                is SmsImportState.Error -> ErrorContent(message = s.message, onRetry = viewModel::reset)
            }
        }
    }
}

@Composable
private fun IdleContent(hasPermission: Boolean, onScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Auto-detect EMI from SMS",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Scans your SMS inbox for HDFC, SBI, and ICICI bank messages and auto-fills loan details.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!hasPermission) {
            Spacer(Modifier.height(8.dp))
            Text(
                "SMS permission is optional — tap below to grant it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
            Text(if (hasPermission) "Scan SMS Messages" else "Grant Permission & Scan")
        }
    }
}

@Composable
private fun ScanningContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Scanning SMS inbox…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ResultsContent(
    items: List<SmsParseResult>,
    onImport: (SmsParseResult) -> Unit,
    onReset: () -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Found ${items.size} EMI messages", fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onReset) { Text("Clear") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.rawBody.hashCode() }) { result ->
                SmsResultCard(result = result, onImport = { onImport(result) })
            }
        }
    }
}

@Composable
private fun SmsResultCard(result: SmsParseResult, onImport: () -> Unit) {
    var imported by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isConfident) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(result.bank.label, fontWeight = FontWeight.Bold)
                Text("₹%.2f".format(result.emiAmount), fontWeight = FontWeight.Bold)
            }
            Text(
                "Confidence: ${"%.0f".format(result.confidence * 100)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (result.loanAccount.isNotBlank()) {
                Text(
                    "A/c: ****${result.loanAccount}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onImport(); imported = true },
                enabled = !imported,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (imported) "Imported" else "Import")
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) { Text("Back") }
    }
}

private val BankSource.label: String
    get() = when (this) {
        BankSource.HDFC    -> "HDFC Bank"
        BankSource.SBI     -> "SBI"
        BankSource.ICICI   -> "ICICI Bank"
        BankSource.GENERIC -> "Bank (Generic)"
        BankSource.UNKNOWN -> "Unknown"
    }
