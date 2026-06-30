package io.helsy.emireminder.ui.screens.finance

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val loanCategories = listOf(
    "Home Loan", "Car Loan", "Personal Loan", "Education Loan",
    "Business Loan", "Gold Loan", "Bike Loan", "Medical Loan",
    "Consumer Loan", "Agricultural Loan", "Mortgage Loan", "Credit Card"
)

@Composable
fun LoanCategoriesScreen(
    onCategorySelected: (String) -> Unit = {},
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select a loan category to calculate EMI", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            loanCategories.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { cat ->
                        OutlinedButton(
                            onClick = { onCategorySelected(cat) },
                            modifier = Modifier.weight(1f)
                        ) { Text(cat, maxLines = 1) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
