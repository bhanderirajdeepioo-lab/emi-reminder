package io.helsy.emireminder.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Savings
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
fun PrepaymentCalculatorScreen(
    onBack: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    var principal by remember { mutableStateOf("1000000") }
    var rate by remember { mutableStateOf("10") }
    var tenure by remember { mutableStateOf("120") }
    var prepayAmount by remember { mutableStateOf("100000") }
    var prepayMonth by remember { mutableStateOf("12") }
    var result by remember { mutableStateOf<PrepayResult?>(null) }
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prepayment Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Indigo600, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Calculate how much you save by making a lump-sum prepayment on your loan.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)

            // Original loan inputs
            SectionLabel("ORIGINAL LOAN DETAILS")
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputField("Loan Amount (₹)", principal) { principal = it }
                    InputField("Annual Interest Rate (%)", rate) { rate = it }
                    InputField("Loan Tenure (months)", tenure) { tenure = it }
                }
            }

            // Prepayment inputs
            SectionLabel("PREPAYMENT DETAILS")
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputField("Prepayment Amount (₹)", prepayAmount) { prepayAmount = it }
                    InputField("Prepay After Month #", prepayMonth) { prepayMonth = it }
                }
            }

            // Calculate button
            Button(
                onClick = {
                    val p = principal.toDoubleOrNull() ?: return@Button
                    val r = rate.toDoubleOrNull() ?: return@Button
                    val t = tenure.toIntOrNull() ?: return@Button
                    val pa = prepayAmount.toDoubleOrNull() ?: return@Button
                    val pm = prepayMonth.toIntOrNull() ?: return@Button
                    result = calcPrepayment(p, r, t, pa, pm)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) {
                Icon(Icons.Default.Savings, null)
                Spacer(Modifier.width(8.dp))
                Text("Calculate Savings", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            // Results
            result?.let { res ->
                // Savings hero
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(SafeGreen, Color(0xFF047857))))
                        .padding(20.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Total Savings", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(fmt.format(res.totalSavings), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("${res.monthsSaved} months off your loan tenure", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }

                // Breakdown comparison
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Comparison", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("", modifier = Modifier.weight(1.2f))
                            Text("Without", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("With Prepay", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SafeGreen)
                        }
                        Divider()
                        CompareRow("Total Interest", fmt.format(res.interestWithout), fmt.format(res.interestWith))
                        CompareRow("Total Payment", fmt.format(res.totalWithout), fmt.format(res.totalWith))
                        CompareRow("Loan Tenure", "${res.originalTenure} months", "${res.newTenure} months")
                    }
                }
            }
        }
    }
}

private data class PrepayResult(
    val interestWithout: Double,
    val totalWithout: Double,
    val interestWith: Double,
    val totalWith: Double,
    val totalSavings: Double,
    val originalTenure: Int,
    val newTenure: Int,
    val monthsSaved: Int,
)

private fun calcPrepayment(principal: Double, annualRate: Double, tenureMonths: Int, prepayAmount: Double, prepayAtMonth: Int): PrepayResult {
    val r = annualRate / (12 * 100)
    val emi = if (r == 0.0) principal / tenureMonths else
        (principal * r * (1 + r).pow(tenureMonths)) / ((1 + r).pow(tenureMonths) - 1)

    // Without prepayment
    val totalWithout = emi * tenureMonths
    val interestWithout = totalWithout - principal

    // With prepayment — simulate month by month
    var balance = principal
    var totalPaid = 0.0
    var monthsPaid = 0

    for (month in 1..tenureMonths) {
        if (balance <= 0) break
        val interest = balance * r
        val principalPaid = emi - interest
        balance -= principalPaid
        totalPaid += emi
        monthsPaid++
        if (month == prepayAtMonth && balance > 0) {
            val actualPrepay = minOf(prepayAmount, balance)
            balance -= actualPrepay
            totalPaid += actualPrepay
        }
    }

    val interestWith = totalPaid - principal
    val monthsSaved = tenureMonths - monthsPaid

    return PrepayResult(
        interestWithout = interestWithout,
        totalWithout = totalWithout,
        interestWith = interestWith,
        totalWith = totalPaid,
        totalSavings = totalWithout - totalPaid,
        originalTenure = tenureMonths,
        newTenure = monthsPaid,
        monthsSaved = monthsSaved,
    )
}

@Composable
private fun InputField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
    )
}

@Composable
private fun CompareRow(label: String, without: String, withPrepay: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(without, modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(withPrepay, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
    }
    Divider()
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
}
