package io.helsy.emireminder.ui.screens.finance

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun FinanceMonthlyEMIScreen(
    onNavigateToLoanDetail: (Int) -> Unit,
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Monthly Finance", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Monthly EMI Overview", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Your monthly EMI obligations and payment calendar.",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
