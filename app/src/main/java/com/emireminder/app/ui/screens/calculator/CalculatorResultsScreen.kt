package com.emireminder.app.ui.screens.calculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CalculatorResultsScreen(
    principal: Double,
    rate: Double,
    tenureMonths: Int,
    onBack: () -> Unit,
    onViewAmortization: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    val emi = remember(principal, rate, tenureMonths) { viewModel.calculateEmi(principal, rate, tenureMonths) }
    val totalInterest = remember(emi, tenureMonths, principal) { viewModel.calculateTotalInterest(emi, tenureMonths, principal) }
    val totalPayment = principal + totalInterest
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Indigo600, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Hero EMI card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Indigo600, Violet600)))
                    .padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Monthly EMI", fontSize = 14.sp, color = Indigo100)
                    Spacer(Modifier.height(4.dp))
                    Text(fmt.format(emi), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MiniStat("${tenureMonths} mo", "Tenure")
                        MiniStat("%.1f%%".format(rate), "Rate")
                        MiniStat(fmt.format(principal).replace(",00,000", "L"), "Principal")
                    }
                }
            }

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Donut chart with legend
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DonutPieChart(
                            principal = principal,
                            interest = totalInterest,
                            modifier = Modifier.size(130.dp),
                        )
                        Spacer(Modifier.width(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LegendRow("Principal", fmt.format(principal), Indigo600, principal / totalPayment)
                            LegendRow("Interest", fmt.format(totalInterest), WarnOrange, totalInterest / totalPayment)
                            Divider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(fmt.format(totalPayment), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Indigo600)
                            }
                        }
                    }
                }

                // Breakdown table
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Payment Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        ResultRow("Loan Amount", fmt.format(principal))
                        ResultRow("Annual Rate", "%.2f%%".format(rate))
                        ResultRow("Loan Tenure", "$tenureMonths months (${tenureMonths / 12}y ${tenureMonths % 12}m)")
                        Divider()
                        ResultRow("Monthly EMI", fmt.format(emi), bold = true, color = Indigo600)
                        ResultRow("Total Interest", fmt.format(totalInterest), color = WarnOrange)
                        ResultRow("Total Payment", fmt.format(totalPayment), bold = true)
                    }
                }

                // View amortization button
                Button(
                    onClick = onViewAmortization,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                ) {
                    Icon(Icons.Default.TableChart, null)
                    Spacer(Modifier.width(8.dp))
                    Text("View Amortization Schedule", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DonutPieChart(principal: Double, interest: Double, modifier: Modifier) {
    val total = principal + interest
    val principalSweep = if (total > 0) (principal / total * 300f).toFloat() else 150f
    val interestSweep = 300f - principalSweep

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = size.minDimension * 0.16f, cap = StrokeCap.Round)
            val inset = stroke.width / 2 + 6f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(Color(0xFFEEF2FF), -210f, 300f, false, topLeft, arcSize, style = stroke)
            drawArc(Color(0xFF4F46E5), -210f, principalSweep, false, topLeft, arcSize, style = stroke)
            drawArc(Color(0xFFD97706), -210f + principalSweep, interestSweep, false, topLeft, arcSize, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%.0f%%".format(if (total > 0) interest / total * 100 else 0.0), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = WarnOrange)
            Text("interest", fontSize = 10.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun LegendRow(label: String, value: String, color: Color, ratio: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("%.0f%%".format(ratio * 100), fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ResultRow(label: String, value: String, bold: Boolean = false, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium, color = color)
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 10.sp, color = Indigo100)
    }
}
