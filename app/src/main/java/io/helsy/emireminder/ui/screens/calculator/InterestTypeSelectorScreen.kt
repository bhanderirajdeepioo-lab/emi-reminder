package io.helsy.emireminder.ui.screens.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun InterestTypeSelectorScreen(
    principal: Double,
    rate: Double,
    tenureMonths: Int,
    currentType: String,
    onBack: () -> Unit,
) {
    var selected by remember { mutableStateOf(currentType) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interest Type", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Select Interest Calculation Method", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            listOf("REDUCING" to "Reducing Balance", "FLAT" to "Flat Rate").forEach { (key, label) ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = selected == key, onClick = { selected = key })
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Apply")
            }
        }
    }
}
