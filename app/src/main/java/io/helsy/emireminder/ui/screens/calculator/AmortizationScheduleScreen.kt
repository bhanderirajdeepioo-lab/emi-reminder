package io.helsy.emireminder.ui.screens.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AmortizationScheduleScreen(
    principal: Double,
    rate: Double,
    tenureMonths: Int,
    onBack: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    val schedule = remember { viewModel.buildAmortizationSchedule(principal, rate, tenureMonths) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amortization Schedule", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("Month", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Interest", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    Text("Principal", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                }
                Divider()
            }
            itemsIndexed(schedule, key = { index, _ -> index }) { _, row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("${row.first}", modifier = Modifier.weight(1f))
                    Text("₹%.0f".format(row.second), modifier = Modifier.weight(2f))
                    Text("₹%.0f".format(row.third), modifier = Modifier.weight(2f))
                }
            }
        }
    }
}
