package io.helsy.emireminder.ui.screens.finance

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FinanceToolsHubScreen(
    onNavigateToEmiCalculator: () -> Unit,
    onNavigateToComparison: () -> Unit,
    onNavigateToPrepayment: () -> Unit,
    onNavigateToFdRd: () -> Unit,
    onNavigateToSip: () -> Unit,
    onNavigateToLoanCategories: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Finance Tools", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Calculators & Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ToolButton("EMI Calculator", onNavigateToEmiCalculator)
            ToolButton("Loan Comparison", onNavigateToComparison)
            ToolButton("Prepayment Calculator", onNavigateToPrepayment)
            ToolButton("FD / RD Calculator", onNavigateToFdRd)
            ToolButton("SIP Calculator", onNavigateToSip)
            ToolButton("Loan Categories", onNavigateToLoanCategories)
        }
    }
}

@Composable
private fun ToolButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}
