package io.helsy.emireminder.ui.screens.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CalculatorResultsScreen(
    principal: Double,
    rate: Double,
    tenureMonths: Int,
    onBack: () -> Unit,
    onViewAmortization: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    val emi = remember { viewModel.calculateEmi(principal, rate, tenureMonths) }
    val totalInterest = remember { viewModel.calculateTotalInterest(emi, tenureMonths, principal) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculator Results", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ResultRow("Loan Amount", "₹%.2f".format(principal))
            ResultRow("Annual Rate", "%.2f%%".format(rate))
            ResultRow("Tenure", "$tenureMonths months")
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            ResultRow("Monthly EMI", "₹%.2f".format(emi), bold = true)
            ResultRow("Total Interest", "₹%.2f".format(totalInterest))
            ResultRow("Total Amount", "₹%.2f".format(principal + totalInterest))
            Spacer(Modifier.height(24.dp))
            Button(onClick = onViewAmortization, modifier = Modifier.fillMaxWidth()) {
                Text("View Amortization Schedule")
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (bold) 18.sp else 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
