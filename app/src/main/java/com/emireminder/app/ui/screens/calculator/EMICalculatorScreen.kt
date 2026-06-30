package com.emireminder.app.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emireminder.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private enum class LoanTab(val label: String) {
    HOME("Home"),
    CAR("Car"),
    PERSONAL("Personal"),
}

private data class LoanTabConfig(
    val defaultPrincipal: Double,
    val minPrincipal: Double,
    val maxPrincipal: Double,
    val minLabel: String,
    val maxLabel: String,
)

private val loanTabConfigs = mapOf(
    LoanTab.HOME     to LoanTabConfig(5_000_000.0,  100_000.0,  20_000_000.0, "₹1L",  "₹2Cr"),
    LoanTab.CAR      to LoanTabConfig(  800_000.0,   50_000.0,   3_000_000.0, "₹50K", "₹30L"),
    LoanTab.PERSONAL to LoanTabConfig(  200_000.0,   10_000.0,   1_000_000.0, "₹10K", "₹10L"),
)

@Composable
fun EMICalculatorScreen(
    onBack: () -> Unit,
    onShowResults: (Double, Double, Int) -> Unit,
    onInterestTypeSelector: (Double, Double, Int, String) -> Unit,
    showBackButton: Boolean = true,
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableStateOf(LoanTab.HOME) }
    val config = loanTabConfigs[selectedTab]!!

    var principal by remember { mutableStateOf(config.defaultPrincipal) }
    var rate by remember { mutableStateOf(10.0) }
    var tenure by remember { mutableStateOf(60) }
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    // Reset principal when tab changes
    LaunchedEffect(selectedTab) {
        principal = loanTabConfigs[selectedTab]!!.defaultPrincipal
    }

    val emi = remember(principal, rate, tenure) {
        viewModel.calculateEmi(principal, rate, tenure)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMI Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Indigo600,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
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
            // Live EMI banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Indigo600, Violet600)))
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Monthly EMI", fontSize = 13.sp, color = Indigo100)
                    Text(
                        fmt.format(emi),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        "Total: ${fmt.format(emi * tenure)}  •  Interest: ${fmt.format((emi * tenure) - principal)}",
                        fontSize = 12.sp,
                        color = Indigo100.copy(alpha = 0.8f),
                    )
                }
            }

            // Loan-type segmented tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LoanTab.values().forEach { tab ->
                    val isSelected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Indigo600 else Indigo50),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(
                            onClick = { selectedTab = tab },
                            contentPadding = PaddingValues(vertical = 8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                tab.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Indigo600,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Loan Amount slider — range adapts to selected loan type
                val principalSlider = ((principal - config.minPrincipal) / (config.maxPrincipal - config.minPrincipal)).toFloat().coerceIn(0f, 1f)
                SliderField(
                    label = "Loan Amount",
                    displayValue = fmt.format(principal),
                    sliderValue = principalSlider,
                    onSliderChange = {
                        principal = (it * (config.maxPrincipal - config.minPrincipal) + config.minPrincipal)
                            .roundToInt().toDouble().coerceIn(config.minPrincipal, config.maxPrincipal)
                    },
                    inputValue = if (principal == principal.toLong().toDouble()) principal.toLong().toString() else principal.toString(),
                    onInputChange = { v ->
                        v.toDoubleOrNull()?.let {
                            if (it in config.minPrincipal..config.maxPrincipal) principal = it
                        }
                    },
                    valueRange = 0f..1f,
                    minLabel = config.minLabel,
                    maxLabel = config.maxLabel,
                    accentColor = Indigo600,
                )

                // Interest Rate slider
                SliderField(
                    label = "Annual Interest Rate",
                    displayValue = "%.1f%%".format(rate),
                    sliderValue = ((rate - 1) / 29f).toFloat(),
                    onSliderChange = { rate = ((it * 29) + 1).roundToInt().toDouble().coerceIn(1.0, 30.0) },
                    inputValue = "%.1f".format(rate),
                    onInputChange = { v -> v.toDoubleOrNull()?.let { if (it in 0.1..50.0) rate = it } },
                    valueRange = 0f..1f,
                    minLabel = "1%",
                    maxLabel = "30%",
                    accentColor = Color(0xFF0891B2),
                )

                // Tenure slider
                SliderField(
                    label = "Loan Tenure",
                    displayValue = "${tenure} months (${tenure / 12}y ${tenure % 12}m)",
                    sliderValue = ((tenure - 1) / 359f).toFloat(),
                    onSliderChange = { tenure = ((it * 359) + 1).roundToInt().coerceIn(1, 360) },
                    inputValue = tenure.toString(),
                    onInputChange = { v -> v.toIntOrNull()?.let { if (it in 1..360) tenure = it } },
                    valueRange = 0f..1f,
                    minLabel = "1 mo",
                    maxLabel = "30 yr",
                    accentColor = SafeGreen,
                )

                // Quick tenure chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(12, 24, 36, 60, 84, 120, 240).forEach { months ->
                        val selected = tenure == months
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Indigo600 else Indigo50),
                        ) {
                            TextButton(
                                onClick = { tenure = months },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "${months / 12}Y",
                                    fontSize = 12.sp,
                                    color = if (selected) Color.White else Indigo600,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }

                // Buttons
                Button(
                    onClick = { onShowResults(principal, rate, tenure) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                ) {
                    Icon(Icons.Default.Calculate, null)
                    Spacer(Modifier.width(8.dp))
                    Text("View Detailed Results", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { onInterestTypeSelector(principal, rate, tenure, "REDUCING") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Change Interest Type (Flat / Reducing)", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun SliderField(
    label: String,
    displayValue: String,
    sliderValue: Float,
    onSliderChange: (Float) -> Unit,
    inputValue: String,
    onInputChange: (String) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    minLabel: String,
    maxLabel: String,
    accentColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(displayValue, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
        }
        Slider(
            value = sliderValue,
            onValueChange = onSliderChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = accentColor.copy(alpha = 0.2f),
            ),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(minLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(maxLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
