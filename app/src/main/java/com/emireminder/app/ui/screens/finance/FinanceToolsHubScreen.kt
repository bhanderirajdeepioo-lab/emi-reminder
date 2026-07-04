package com.emireminder.app.ui.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emireminder.app.ui.theme.*
import kotlinx.coroutines.launch

private data class LoanTool(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val label: String,
    val subtitle: String,
    val isNew: Boolean = false,
    val onClick: () -> Unit,
)

private data class InvestTool(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val accentColor: Color,
    val label: String,
    val sublabel: String,
    val onClick: () -> Unit,
)

private data class MoreTool(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun FinanceToolsHubScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
    onNavigateToEmiCalculator: () -> Unit,
    onNavigateToComparison: () -> Unit,
    onNavigateToPrepayment: () -> Unit,
    onNavigateToFdRd: () -> Unit,
    onNavigateToSip: () -> Unit,
    onNavigateToPpf: () -> Unit,
    onNavigateToGst: () -> Unit,
    onNavigateToIncomeTax: () -> Unit,
    onNavigateToInflation: () -> Unit,
    onNavigateToHra: () -> Unit,
    onNavigateToCibil: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val loanTools = remember(onNavigateToEmiCalculator, onNavigateToComparison, onNavigateToPrepayment) {
        listOf(
            LoanTool(
                icon = Icons.Default.Calculate,
                iconBg = Indigo50, iconTint = Indigo600,
                label = "EMI Calculator",
                subtitle = "Reducing balance & flat rate",
                onClick = onNavigateToEmiCalculator,
            ),
            LoanTool(
                icon = Icons.Default.CompareArrows,
                iconBg = Color(0xFFF3E8FF), iconTint = Violet600,
                label = "Loan Comparison",
                subtitle = "Compare 2 loan offers side by side",
                onClick = onNavigateToComparison,
            ),
            LoanTool(
                icon = Icons.Default.Payments,
                iconBg = Color(0xFFF0FDF4), iconTint = SafeGreen,
                label = "Prepayment / Foreclosure",
                subtitle = "Save interest with early payoff",
                onClick = onNavigateToPrepayment,
            ),
            LoanTool(
                icon = Icons.Default.TrendingUp,
                iconBg = Color(0xFFFFF7ED), iconTint = Color(0xFFD97706),
                label = "Step-up EMI",
                subtitle = "Graduated EMI with annual increment",
                isNew = true,
                onClick = { scope.launch { snackbarHostState.showSnackbar("Coming soon") } },
            ),
            LoanTool(
                icon = Icons.Default.SwapHoriz,
                iconBg = Color(0xFFFDF2F8), iconTint = Color(0xFFEC4899),
                label = "Balance Transfer",
                subtitle = "Switch loan to a lower rate bank",
                isNew = true,
                onClick = { scope.launch { snackbarHostState.showSnackbar("Coming soon") } },
            ),
        )
    }

    val investTools = remember(onNavigateToSip, onNavigateToFdRd, onNavigateToPpf, onNavigateToGst) {
        listOf(
            InvestTool(
                icon = Icons.Default.BarChart,
                iconBg = Color(0xFFF0FDFA), iconTint = Color(0xFF0D9488),
                accentColor = Color(0xFF0D9488),
                label = "SIP Calculator",
                sublabel = "Wealth projection & step-up SIP",
                onClick = onNavigateToSip,
            ),
            InvestTool(
                icon = Icons.Default.AccountBalance,
                iconBg = Color(0xFFFFFBEB), iconTint = WarnOrange,
                accentColor = WarnOrange,
                label = "FD / RD Calc",
                sublabel = "Fixed & recurring deposit returns",
                onClick = onNavigateToFdRd,
            ),
            InvestTool(
                icon = Icons.Default.Lock,
                iconBg = Color(0xFFF0FDF4), iconTint = Color(0xFF16A34A),
                accentColor = Color(0xFF16A34A),
                label = "PPF Calculator",
                sublabel = "15-yr compounding at 7.1% p.a.",
                onClick = onNavigateToPpf,
            ),
            InvestTool(
                icon = Icons.Default.Receipt,
                iconBg = Color(0xFFEFF6FF), iconTint = Color(0xFF2563EB),
                accentColor = Color(0xFF2563EB),
                label = "GST Calculator",
                sublabel = "Inclusive & exclusive tax calc",
                onClick = onNavigateToGst,
            ),
        )
    }

    val moreTools = remember(onNavigateToIncomeTax, onNavigateToInflation, onNavigateToHra, onNavigateToCibil) {
        listOf(
            MoreTool(
                icon = Icons.Default.Calculate,
                iconBg = Color(0xFFF3E8FF), iconTint = Violet600,
                label = "Income Tax",
                onClick = onNavigateToIncomeTax,
            ),
            MoreTool(
                icon = Icons.Default.TrendingDown,
                iconBg = Color(0xFFFFFBEB), iconTint = WarnOrange,
                label = "Inflation Calc",
                onClick = onNavigateToInflation,
            ),
            MoreTool(
                icon = Icons.Default.Home,
                iconBg = Color(0xFFEFF6FF), iconTint = Color(0xFF2563EB),
                label = "HRA Calc",
                onClick = onNavigateToHra,
            ),
            MoreTool(
                icon = Icons.Default.CreditScore,
                iconBg = Color(0xFFF0FDF4), iconTint = Color(0xFF16A34A),
                label = "CIBIL Score",
                onClick = onNavigateToCibil,
            ),
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredLoanTools by remember { derivedStateOf {
        if (searchQuery.isBlank()) loanTools
        else loanTools.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    } }
    val filteredInvestTools by remember { derivedStateOf {
        if (searchQuery.isBlank()) investTools
        else investTools.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                it.sublabel.contains(searchQuery, ignoreCase = true)
        }
    } }
    val filteredMoreTools by remember { derivedStateOf {
        if (searchQuery.isBlank()) moreTools
        else moreTools.filter { it.label.contains(searchQuery, ignoreCase = true) }
    } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Indigo600, Violet600)))
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(
                        "Finance Tools",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                }
            }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = {
                        Text("Search tools…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = if (searchQuery.isNotBlank()) Indigo600
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Indigo600,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }

            if (searchQuery.isNotBlank()) {
                // Search results
                val totalResults = filteredLoanTools.size + filteredInvestTools.size + filteredMoreTools.size
                item {
                    Text(
                        "$totalResults result${if (totalResults != 1) "s" else ""} for \"$searchQuery\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                    )
                }
                items(filteredLoanTools) { tool ->
                    LoanToolRow(
                        tool = tool,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }
                items(filteredInvestTools) { tool ->
                    InvestToolRow(
                        tool = tool,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }
                items(filteredMoreTools) { tool ->
                    MoreToolRow(
                        tool = tool,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }
                if (totalResults == 0) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Try \"EMI\", \"SIP\", \"PPF\", \"tax\"…",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                // Default: Loan Calculators section
                item {
                    Text(
                        "LOAN CALCULATORS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Indigo600,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp),
                    )
                }
                items(loanTools) { tool ->
                    LoanToolRow(
                        tool = tool,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    )
                }

                // Investment Tools 2×2 grid
                item {
                    Text(
                        "INVESTMENT TOOLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Indigo600,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 6.dp),
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        investTools.chunked(2).forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                row.forEach { tool ->
                                    InvestmentCard(tool = tool, modifier = Modifier.weight(1f))
                                }
                                if (row.size < 2) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 4 More Tools strip
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "4 MORE TOOLS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                moreTools.forEach { tool ->
                                    MoreToolChip(tool = tool)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanToolRow(tool: LoanTool, modifier: Modifier = Modifier) {
    Card(
        onClick = tool.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tool.iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tool.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (tool.isNew) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Indigo600,
                        ) {
                            Text(
                                "NEW",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    tool.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun InvestmentCard(tool: InvestTool, modifier: Modifier = Modifier) {
    Card(
        onClick = tool.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Decorative tint blob top-right (partially clipped by card shape)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 16.dp, y = (-16).dp)
                    .clip(CircleShape)
                    .background(tool.accentColor.copy(alpha = 0.08f)),
            )
            Column(modifier = Modifier.padding(16.dp, 16.dp, 14.dp, 16.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tool.iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(tool.icon, contentDescription = null, tint = tool.iconTint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(tool.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(tool.sublabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun MoreToolChip(tool: MoreTool) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = tool.onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tool.iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.iconTint, modifier = Modifier.size(24.dp))
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB45309)),
                contentAlignment = Alignment.Center,
            ) {
                Text("!", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tool.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
            maxLines = 2,
        )
    }
}

// Search-results rows for InvestTool and MoreTool — reuse the LoanToolRow visual pattern

@Composable
private fun InvestToolRow(tool: InvestTool, modifier: Modifier = Modifier) {
    Card(
        onClick = tool.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tool.iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tool.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text("${tool.sublabel} · Investment Tools", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun MoreToolRow(tool: MoreTool, modifier: Modifier = Modifier) {
    Card(
        onClick = tool.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tool.iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tool.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text("Finance Tools", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}
