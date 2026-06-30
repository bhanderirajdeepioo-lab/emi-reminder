package io.helsy.emireminder.ui.screens.reminders

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AddReminderScreen(
    loanId: Int,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Reminder", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text("Create a new EMI reminder.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Text("Loan ID: $loanId", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
