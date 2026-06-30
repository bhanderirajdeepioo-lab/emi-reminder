package io.helsy.emireminder.ui.screens.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun EMICalculatorScreen(
    onBack: () -> Unit,
    onShowResults: (Double, Double, Int) -> Unit,
    onInterestTypeSelector: (Double, Double, Int, String) -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    var principal by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var tenure by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMI Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = principal, onValueChange = { principal = it },
                label = { Text("Loan Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = rate, onValueChange = { rate = it },
                label = { Text("Annual Interest Rate (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = tenure, onValueChange = { tenure = it },
                label = { Text("Tenure (months)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val p = principal.toDoubleOrNull() ?: return@Button
                    val r = rate.toDoubleOrNull() ?: return@Button
                    val t = tenure.toIntOrNull() ?: return@Button
                    onShowResults(p, r, t)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Calculate EMI") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val p = principal.toDoubleOrNull() ?: 1_000_000.0
                    val r = rate.toDoubleOrNull() ?: 10.0
                    val t = tenure.toIntOrNull() ?: 60
                    onInterestTypeSelector(p, r, t, "REDUCING")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Change Interest Type") }
        }
    }
}
