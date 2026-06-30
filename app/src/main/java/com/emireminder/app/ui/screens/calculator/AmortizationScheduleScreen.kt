package com.emireminder.app.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

private data class AmortRow(val month: Int, val emi: Double, val principal: Double, val interest: Double, val balance: Double)

@Composable
fun AmortizationScheduleScreen(
    principal: Double,
    rate: Double,
    tenureMonths: Int,
    onBack: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    val emi = remember(principal, rate, tenureMonths) { viewModel.calculateEmi(principal, rate, tenureMonths) }
    val totalInterest = remember(emi, tenureMonths, principal) { viewModel.calculateTotalInterest(emi, tenureMonths, principal) }
    val numFmt = NumberFormat.getNumberInstance(Locale("en", "IN"))
    fun fmt(v: Double) = "₹${numFmt.format(v.toLong())}"

    val rows: List<AmortRow> = remember(principal, rate, tenureMonths) {
        val monthlyRate = rate / (12 * 100)
        var balance = principal
        (1..tenureMonths).map { month ->
            val interest = balance * monthlyRate
            val principalPaid = emi - interest
            balance -= principalPaid
            AmortRow(month, emi, principalPaid, interest, maxOf(0.0, balance))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amortization Schedule", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo600,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            // Summary banner
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Indigo600, Violet600))).padding(16.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SummaryCol("Monthly EMI", fmt(emi))
                        SummaryCol("Total Interest", fmt(totalInterest))
                        SummaryCol("Total Amount", fmt(principal + totalInterest))
                    }
                }
            }
            // Header row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    TableHeaderCell("Mo", Modifier.width(40.dp))
                    TableHeaderCell("Principal", Modifier.weight(1f))
                    TableHeaderCell("Interest", Modifier.weight(1f))
                    TableHeaderCell("Balance", Modifier.weight(1.3f))
                }
            }
            // Data rows
            itemsIndexed(rows) { idx, row ->
                val bgColor = if (idx % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Month indicator — show year badge at start of each year
                    Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                        if (row.month % 12 == 1) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Indigo600)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                            ) {
                                Text("Y${(row.month - 1) / 12 + 1}", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                "${row.month}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    TableCell(fmt(row.principal), Modifier.weight(1f), Indigo600)
                    TableCell(fmt(row.interest), Modifier.weight(1f), WarnOrange)
                    TableCell(fmt(row.balance), Modifier.weight(1.3f))
                }
            }
        }
    }
}

@Composable
private fun SummaryCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Indigo100)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun TableCell(text: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(text, modifier = modifier, fontSize = 11.sp, color = color, textAlign = TextAlign.End, maxLines = 1)
}
