package io.helsy.emireminder.ui.screens.reminders

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RemindersScreen(
    onAddReminder: () -> Unit,
    onReminderClick: (Int) -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Reminders", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddReminder, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Reminders", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Your EMI reminders will appear here.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
