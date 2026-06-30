package io.helsy.emireminder.ui.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

private data class ToolCard(
    val label: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
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
    val tools = listOf(
        ToolCard("EMI Calculator",      Icons.Default.Calculate,         Indigo100,             Indigo600,            onNavigateToEmiCalculator),
        ToolCard("FD / RD Calculator",  Icons.Default.AccountBalance,    Color(0xFFECFDF5),     SafeGreen,            onNavigateToFdRd),
        ToolCard("SIP Calculator",      Icons.Default.TrendingUp,        Color(0xFFF3E8FF),     Violet600,            onNavigateToSip),
        ToolCard("Loan Comparison",     Icons.Default.CompareArrows,     Color(0xFFFFF7ED),     WarnOrange,           onNavigateToComparison),
        ToolCard("Prepayment Calc",     Icons.Default.Payment,           Color(0xFFEFF6FF),     HomeLoanColor,        onNavigateToPrepayment),
        ToolCard("Loan Categories",     Icons.Default.Category,          Color(0xFFFFF1F2),     UrgentRed,            onNavigateToLoanCategories),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance Tools", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(tools, key = { it.label }) { tool ->
                FinanceToolCard(tool)
            }
        }
    }
}

@Composable
private fun FinanceToolCard(tool: ToolCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = tool.onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tool.iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    tool.icon,
                    contentDescription = tool.label,
                    tint = tool.iconTint,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                tool.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
