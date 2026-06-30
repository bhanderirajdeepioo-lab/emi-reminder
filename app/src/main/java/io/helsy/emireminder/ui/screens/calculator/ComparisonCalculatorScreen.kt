package io.helsy.emireminder.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.helsy.emireminder.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

@Composable
fun ComparisonCalculatorScreen(
    onBack: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    var p1 by remember { mutableStateOf("1000000") }
    var r1 by remember { mutableStateOf("10") }
    var t1 by remember { mutableStateOf("60") }
    var p2 by remember { mutableStateOf("1000000") }
    var r2 by remember { mutableStateOf("12") }
    var t2 by remember { mutableStateOf("48") }
    var compared by remember { mutableStateOf(false) }
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    val emi1 = p1.toDoubleOrNull()?.let { p -> r1.toDoubleOrNull()?.let { r -> t1.toIntOrNull()?.let { t -> viewModel.calculateEmi(p, r, t) } } }
    val emi2 = p2.toDoubleOrNull()?.let { p -> r2.toDoubleOrNull()?.let { r -> t2.toIntOrNull()?.let { t -> viewModel.calculateEmi(p, r, t) } } }
    val total1 = emi1?.let { it * (t1.toIntOrNull() ?: 0) }
    val total2 = emi2?.let { it * (t2.toIntOrNull() ?: 0) }
    val interest1 = total1?.minus(p1.toDoubleOrNull() ?: 0.0)
    val interest2 = total2?.minus(p2.toDoubleOrNull() ?: 0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Comparison", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Indigo600, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Two loan input panels side by side
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LoanInputPanel(
                    label = "Loan A",
                    labelColor = Indigo600,
                    principal = p1, onPrincipalChange = { p1 = it },
                    rate = r1, onRateChange = { r1 = it },
                    tenure = t1, onTenureChange = { t1 = it },
                    modifier = Modifier.weight(1f),
                )
                LoanInputPanel(
                    label = "Loan B",
                    labelColor = Color(0xFF0891B2),
                    principal = p2, onPrincipalChange = { p2 = it },
                    rate = r2, onRateChange = { r2 = it },
                    tenure = t2, onTenureChange = { t2 = it },
                    modifier = Modifier.weight(1f),
                )
            }

            // Compare button
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Button(
                    onClick = { compared = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                ) {
                    Icon(Icons.Default.CompareArrows, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Compare Loans", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (compared && emi1 != null && emi2 != null && total1 != null && total2 != null && interest1 != null && interest2 != null) {
                Spacer(Modifier.height(20.dp))

                // Comparison result table
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("COMPARISON RESULTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            // Header
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text("", modifier = Modifier.weight(1.2f))
                                Text("Loan A", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Indigo600, fontSize = 13.sp)
                                Text("Loan B", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF0891B2), fontSize = 13.sp)
                            }
                            Divider()
                            CompareRow3("Monthly EMI", fmt.format(emi1), fmt.format(emi2), good1 = emi1 < emi2, good2 = emi1 > emi2)
                            CompareRow3("Total Interest", fmt.format(interest1), fmt.format(interest2), good1 = interest1 < interest2, good2 = interest1 > interest2)
                            CompareRow3("Total Payment", fmt.format(total1), fmt.format(total2), good1 = total1 < total2, good2 = total1 > total2)
                        }
                    }

                    // Winner card
                    val winnerText = when {
                        total1 < total2 -> "Loan A saves you ${fmt.format(total2 - total1)} overall."
                        total2 < total1 -> "Loan B saves you ${fmt.format(total1 - total2)} overall."
                        else -> "Both loans have identical total costs."
                    }
                    val winnerColor = if (total1 <= total2) Indigo600 else Color(0xFF0891B2)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = winnerColor.copy(alpha = 0.1f)),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🏆", fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(winnerText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = winnerColor, lineHeight = 20.sp)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LoanInputPanel(
    label: String,
    labelColor: Color,
    principal: String, onPrincipalChange: (String) -> Unit,
    rate: String, onRateChange: (String) -> Unit,
    tenure: String, onTenureChange: (String) -> Unit,
    modifier: Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(labelColor).padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            CompactField("Amount (₹)", principal, onPrincipalChange)
            CompactField("Rate (%)", rate, onRateChange)
            CompactField("Months", tenure, onTenureChange)
        }
    }
}

@Composable
private fun CompactField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
    )
}

@Composable
private fun CompareRow3(label: String, value1: String, value2: String, good1: Boolean, good2: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value1,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            fontWeight = if (good1) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (good1) SafeGreen else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            value2,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            fontWeight = if (good2) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (good2) SafeGreen else MaterialTheme.colorScheme.onSurface,
        )
    }
    Divider()
}
