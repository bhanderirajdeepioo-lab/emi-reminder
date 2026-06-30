package io.helsy.emireminder.ui.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.helsy.emireminder.ui.theme.*

private data class ListTool(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val label: String,
    val subtitle: String,
    val isNew: Boolean = false,
    val onClick: () -> Unit,
)

private data class GridTool(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val label: String,
    val sublabel: String,
    val isComingSoon: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun FinanceToolsHubScreen(
    onNavigateToEmiCalculator: () -> Unit,
    onNavigateToComparison: () -> Unit,
    onNavigateToPrepayment: () -> Unit,
    onNavigateToFdRd: () -> Unit,
    onNavigateToSip: () -> Unit,
    onNavigateToLoanCategories: () -> Unit,
) {
    val loanTools = listOf(
        ListTool(
            icon = Icons.Default.Calculate,
            iconBg = Indigo50, iconTint = Indigo600,
            label = "EMI Calculator",
            subtitle = "Reducing balance & flat rate",
            onClick = onNavigateToEmiCalculator,
        ),
        ListTool(
            icon = Icons.Default.CompareArrows,
            iconBg = Color(0xFFF3E8FF), iconTint = Violet600,
            label = "Loan Comparison",
            subtitle = "Compare 2 loan offers side by side",
            onClick = onNavigateToComparison,
        ),
        ListTool(
            icon = Icons.Default.Payments,
            iconBg = Color(0xFFF0FDF4), iconTint = SafeGreen,
            label = "Prepayment / Foreclosure",
            subtitle = "Save interest with part-payment",
            onClick = onNavigateToPrepayment,
        ),
        ListTool(
            icon = Icons.Default.TrendingUp,
            iconBg = Color(0xFFEFF6FF), iconTint = HomeLoanColor,
            label = "Step-up EMI",
            subtitle = "Increase EMI with salary growth",
            isNew = true,
            onClick = { /* coming soon */ },
        ),
        ListTool(
            icon = Icons.Default.SwapHoriz,
            iconBg = Color(0xFFF5F3FF), iconTint = Violet600,
            label = "Balance Transfer",
            subtitle = "Check savings on switching lender",
            isNew = true,
            onClick = { /* coming soon */ },
        ),
    )

    val investmentTools = listOf(
        GridTool(
            icon = Icons.Default.BarChart,
            iconBg = Indigo50, iconTint = Indigo600,
            label = "SIP",
            sublabel = "Mutual Fund\nCalculator",
            onClick = onNavigateToSip,
        ),
        GridTool(
            icon = Icons.Default.AccountBalance,
            iconBg = Color(0xFFFFFBEB), iconTint = WarnOrange,
            label = "FD / RD",
            sublabel = "Fixed & Recurring\nDeposit",
            onClick = onNavigateToFdRd,
        ),
        GridTool(
            icon = Icons.Default.Lock,
            iconBg = Color(0xFFF3E8FF), iconTint = Violet600,
            label = "PPF",
            sublabel = "Public Provident\nFund",
            isComingSoon = true,
            onClick = { },
        ),
        GridTool(
            icon = Icons.Default.Receipt,
            iconBg = Color(0xFFECFEFF), iconTint = CarLoanColor,
            label = "GST",
            sublabel = "Goods & Services\nTax",
            isComingSoon = true,
            onClick = { },
        ),
    )

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance Tools", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo600,
                    titleContentColor = Color.White,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = {
                        Text("Search tools…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Indigo600,
                    ),
                )
            }

            // LOAN CALCULATORS section
            item {
                Text(
                    "LOAN CALCULATORS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Indigo600, fontSize = 11.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column {
                        val filtered = loanTools.filter {
                            searchQuery.isBlank() ||
                                it.label.contains(searchQuery, ignoreCase = true) ||
                                it.subtitle.contains(searchQuery, ignoreCase = true)
                        }
                        filtered.forEachIndexed { idx, tool ->
                            ToolListRow(tool = tool)
                            if (idx < filtered.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
                            }
                        }
                    }
                }
            }

            // INVESTMENT TOOLS section
            item {
                Text(
                    "INVESTMENT TOOLS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SafeGreen, fontSize = 11.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val filtered = investmentTools.filter {
                        searchQuery.isBlank() ||
                            it.label.contains(searchQuery, ignoreCase = true) ||
                            it.sublabel.contains(searchQuery, ignoreCase = true)
                    }
                    val rows = filtered.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                        rows.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { tool ->
                                    InvestmentCard(tool = tool, modifier = Modifier.weight(1f))
                                }
                                if (row.size < 2) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // MORE TOOLS strip
            item {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Indigo600)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "+ 4 more: Income Tax · Inflation · HRA · CIBIL →",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolListRow(tool: ListTool) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = tool.onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tool.iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tool.icon, contentDescription = tool.label,
                tint = tool.iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tool.label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(tool.subtitle, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (tool.isNew) {
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = Indigo50) {
                Text("NEW ✦", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Indigo600,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InvestmentCard(tool: GridTool, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !tool.isComingSoon, onClick = tool.onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tool.iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tool.icon, contentDescription = tool.label,
                    tint = if (tool.isComingSoon) tool.iconTint.copy(alpha = 0.4f) else tool.iconTint,
                    modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                tool.label, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (tool.isComingSoon) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                tool.sublabel, fontSize = 11.sp,
                color = if (tool.isComingSoon) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tool.isComingSoon) {
                Spacer(Modifier.height(4.dp))
                Text("Coming soon", fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
        }
    }
}
