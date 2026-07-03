package com.emireminder.app.ui.screens.financeaccounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emireminder.app.data.db.entity.BankAccount
import com.emireminder.app.ui.theme.Indigo50
import com.emireminder.app.ui.theme.Indigo600
import com.emireminder.app.ui.theme.Slate800

@Composable
fun FinanceAccountsScreen(
    onBack: () -> Unit,
    viewModel: FinanceAccountsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var renameTarget by remember { mutableStateOf<BankAccount?>(null) }

    renameTarget?.let { account ->
        RenameAccountDialog(
            account = account,
            onDismiss = { renameTarget = null },
            onConfirm = { label ->
                viewModel.renameAccount(account.id, label)
                renameTarget = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance Accounts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo600,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = Color(0xFFF8FAFC),
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        when (val state = uiState) {
            is FinanceAccountsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Indigo600)
                }
            }

            is FinanceAccountsUiState.Empty -> {
                EmptyAccountsContent(modifier = Modifier.fillMaxSize().padding(padding))
            }

            is FinanceAccountsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            "Detected bank accounts from your SMS messages. Tap any row to rename.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    items(items = state.accounts, key = { it.id }) { account ->
                        AccountRow(
                            account = account,
                            onRename = { renameTarget = account },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(account: BankAccount, onRename: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRename),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Indigo600.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Indigo600, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.bankName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate800,
                )
                Text(
                    text = "Account ·· ${account.accountLast4}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                )
                if (account.accountLabel != null) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Indigo50,
                    ) {
                        Text(
                            text = account.accountLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Indigo600,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                } else {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Unnamed — tap to label",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                    )
                }
            }

            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RenameAccountDialog(
    account: BankAccount,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var label by remember { mutableStateOf(account.accountLabel ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Label Account")
        },
        text = {
            Column {
                Text(
                    "${account.bankName} ·· ${account.accountLast4}",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Account label") },
                    placeholder = { Text("e.g. Salary Account") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(label) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EmptyAccountsContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Indigo50),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Indigo600, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("No accounts detected", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate800, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Bank accounts will appear here after SMS Intelligence detects your first financial transaction.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}
