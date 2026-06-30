package io.helsy.emireminder.ui.screens.loan

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoanDetailScreen(
    loanId: Int,
    onBack: () -> Unit,
    onViewAmortization: (Double, Double, Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Loan #$loanId", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Loan details and payment history.",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { onViewAmortization(1_000_000.0, 10.0, 60) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("View Amortization Schedule") }
        }
    }
}
