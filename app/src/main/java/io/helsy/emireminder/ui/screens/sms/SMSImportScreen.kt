package io.helsy.emireminder.ui.screens.sms

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
import com.google.accompanist.permissions.*
import io.helsy.emireminder.ui.theme.*

private enum class SmsState { MATCHED, UNCERTAIN, UNMATCHED }

private data class SmsItem(
    val id: Int,
    val sender: String,
    val message: String,
    val state: SmsState,
    val detectedAmount: Double? = null,
    val detectedBank: String? = null,
)

private val sampleSmsItems = listOf(
    SmsItem(1, "HDFCBK", "Your Home Loan EMI of Rs 23,456 has been debited from account xx1234 on 01-Jun.", SmsState.MATCHED, 23456.0, "HDFC Bank"),
    SmsItem(2, "ICICIBK", "Dear Customer, EMI of Rs 8,750 due on 5-Jun for your Car Loan. Ensure adequate balance.", SmsState.MATCHED, 8750.0, "ICICI Bank"),
    SmsItem(3, "SBIMSG", "Loan instalment Rs 12000 credited to account. Ref no: 8761234.", SmsState.UNCERTAIN, 12000.0, "SBI"),
    SmsItem(4, "AXISBK", "Your a/c XX4321 is debited INR 5,200. UPI Ref: 893721.", SmsState.UNMATCHED),
    SmsItem(5, "KOTAKBK", "Personal Loan EMI of INR 6,500 debited on 10-Jun. Principal OS: Rs 1,45,000.", SmsState.MATCHED, 6500.0, "Kotak Bank"),
    SmsItem(6, "ADCBK", "Transaction of Rs 3,000 debited. OTP: 789231.", SmsState.UNMATCHED),
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SMSImportScreen(onBack: () -> Unit) {
    val smsPermission = rememberPermissionState(android.Manifest.permission.READ_SMS)
    var scanned by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf(sampleSmsItems) }
    var importedCount by remember { mutableIntStateOf(0) }

    val matched = items.filter { it.state == SmsState.MATCHED }
    val uncertain = items.filter { it.state == SmsState.UNCERTAIN }
    val unmatched = items.filter { it.state == SmsState.UNMATCHED }

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
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                    Text("SMS Import", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                if (scanned) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip("${matched.size} Matched", SafeGreen)
                        StatusChip("${uncertain.size} Uncertain", WarnOrange)
                        StatusChip("${unmatched.size} No match", Color(0xFF94A3B8))
                    }
                }
            }
        },
    ) { padding ->
        if (!smsPermission.status.isGranted) {
            PermissionRequest(modifier = Modifier.fillMaxSize().padding(padding)) { smsPermission.launchPermissionRequest() }
        } else if (!scanned) {
            ScanPrompt(modifier = Modifier.fillMaxSize().padding(padding)) { scanned = true }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (matched.isNotEmpty()) {
                    item { SectionHeader("MATCHED EMIs", matched.size, SafeGreen) }
                    items(matched, key = { it.id }) { item ->
                        SmsCard(item = item, onImport = { items = items.filterNot { s -> s.id == item.id }; importedCount++ }, onDismiss = { items = items.filterNot { s -> s.id == item.id } })
                    }
                }
                if (uncertain.isNotEmpty()) {
                    item { SectionHeader("NEEDS REVIEW", uncertain.size, WarnOrange) }
                    items(uncertain, key = { it.id }) { item ->
                        SmsCard(item = item, onImport = { items = items.filterNot { s -> s.id == item.id }; importedCount++ }, onDismiss = { items = items.filterNot { s -> s.id == item.id } })
                    }
                }
                if (unmatched.isNotEmpty()) {
                    item { SectionHeader("NOT DETECTED", unmatched.size, Color(0xFF94A3B8)) }
                    items(unmatched, key = { it.id }) { item ->
                        SmsCard(item = item, onImport = null, onDismiss = { items = items.filterNot { s -> s.id == item.id } })
                    }
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
                                Text("$importedCount loan${if (importedCount > 1) "s" else ""} imported!", fontWeight = FontWeight.SemiBold, color = SafeGreen)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmsCard(item: SmsItem, onImport: (() -> Unit)?, onDismiss: () -> Unit) {
    val stateColor = when (item.state) {
        SmsState.MATCHED -> SafeGreen
        SmsState.UNCERTAIN -> WarnOrange
        SmsState.UNMATCHED -> Color(0xFF94A3B8)
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
                Text(item.sender, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                if (item.detectedAmount != null) {
                    Text("₹${"%,.0f".format(item.detectedAmount)}", fontWeight = FontWeight.ExtraBold, color = stateColor, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(item.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
            if (item.detectedBank != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, null, tint = Indigo600, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${item.detectedBank} detected", fontSize = 12.sp, color = Indigo600, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onImport != null) {
                    Button(
                        onClick = onImport,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        contentPadding = PaddingValues(0.dp),
                    ) { Text("Import", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
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
        Text("Allow SMS access to automatically detect your EMI loans from bank messages.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
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
        Text("We'll analyze your SMS inbox to find EMI-related bank messages and import them automatically.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
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
