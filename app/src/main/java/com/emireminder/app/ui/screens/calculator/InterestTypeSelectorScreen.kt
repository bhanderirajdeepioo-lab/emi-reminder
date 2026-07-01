package com.emireminder.app.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

@Composable
fun InterestTypeSelectorScreen(
    principal: Double,
    rate: Double,
    tenureMonths: Int,
    currentType: String,
    onBack: () -> Unit,
    onApply: (String) -> Unit = { onBack() },
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    var selected by remember { mutableStateOf(currentType) }
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    // Reducing balance EMI
    val reducingEmi = remember(principal, rate, tenureMonths) {
        viewModel.calculateEmi(principal, rate, tenureMonths)
    }
    val reducingTotal = reducingEmi * tenureMonths
    val reducingInterest = reducingTotal - principal

    // Flat rate EMI: EMI = (P + P*R*T) / (T*12)
    val flatMonthlyRate = rate / 100
    val flatEmi = remember(principal, rate, tenureMonths) {
        (principal + (principal * flatMonthlyRate * (tenureMonths / 12.0))) / tenureMonths
    }
    val flatTotal = flatEmi * tenureMonths
    val flatInterest = flatTotal - principal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interest Type", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Choose how interest is calculated on your loan.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )

            // Reducing Balance card
            InterestTypeCard(
                title = "Reducing Balance",
                subtitle = "Interest charged on outstanding principal",
                tag = "Most Common",
                tagColor = SafeGreen,
                isSelected = selected == "REDUCING",
                emi = reducingEmi,
                totalInterest = reducingInterest,
                totalPayment = reducingTotal,
                fmt = fmt,
                onClick = { selected = "REDUCING" },
                description = "Each month your interest is calculated only on the remaining loan balance. As you pay EMIs, the principal reduces and so does the interest component.",
            )

            // Flat Rate card
            InterestTypeCard(
                title = "Flat Rate",
                subtitle = "Interest charged on original principal",
                tag = "Higher Cost",
                tagColor = WarnOrange,
                isSelected = selected == "FLAT",
                emi = flatEmi,
                totalInterest = flatInterest,
                totalPayment = flatTotal,
                fmt = fmt,
                onClick = { selected = "FLAT" },
                description = "Interest is calculated on the full original loan amount throughout the entire tenure, regardless of how much you've repaid.",
            )

            // Comparison summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Indigo50),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Savings with Reducing Balance", fontWeight = FontWeight.Bold, color = Indigo600, fontSize = 13.sp)
                    val savingsEmi = flatEmi - reducingEmi
                    val savingsTotal = flatInterest - reducingInterest
                    CompareRow("Lower monthly EMI by", fmt.format(savingsEmi), SafeGreen)
                    CompareRow("Total interest savings", fmt.format(savingsTotal), SafeGreen)
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { onApply(selected) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
            ) {
                Text("Apply ${if (selected == "REDUCING") "Reducing Balance" else "Flat Rate"}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InterestTypeCard(
    title: String,
    subtitle: String,
    tag: String,
    tagColor: Color,
    isSelected: Boolean,
    emi: Double,
    totalInterest: Double,
    totalPayment: Double,
    fmt: NumberFormat,
    onClick: () -> Unit,
    description: String,
) {
    val borderColor = if (isSelected) Indigo600 else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val bgColor = if (isSelected) Indigo50 else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isSelected) Indigo600 else MaterialTheme.colorScheme.onSurface)
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(tagColor.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(tag, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tagColor)
                        }
                    }
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, null, tint = Indigo600, modifier = Modifier.size(24.dp))
                }
            }

            // Numbers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("Monthly EMI", fmt.format(emi), modifier = Modifier.weight(1f))
                MetricPill("Total Interest", fmt.format(totalInterest), modifier = Modifier.weight(1f))
            }

            // Description
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.background).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CompareRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
