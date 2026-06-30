package com.emireminder.app.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    HOME("Home Loan"),
    CAR("Car Loan"),
    PERSONAL("Personal"),
}

private data class LoanTabConfig(
    val defaultPrincipal: Double,
    val minPrincipal: Double,
    val maxPrincipal: Double,
    val minLabel: String,
    val maxLabel: String,
    val defaultRate: Double,
)

private val loanTabConfigs = mapOf(
    LoanTab.HOME     to LoanTabConfig(5_000_000.0,  100_000.0,  20_000_000.0, "₹1L",  "₹2Cr",  8.5),
    LoanTab.CAR      to LoanTabConfig(  800_000.0,   50_000.0,   3_000_000.0, "₹50K", "₹30L",  9.0),
    LoanTab.PERSONAL to LoanTabConfig(  200_000.0,   10_000.0,   1_000_000.0, "₹10K", "₹10L", 13.0),
)

private enum class TenureUnit(val label: String) {
    YEARS("Yr"),
    MONTHS("Mo"),
}

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
    var rate by remember { mutableStateOf(config.defaultRate) }
    var tenure by remember { mutableStateOf(60) }
    var tenureUnit by remember { mutableStateOf(TenureUnit.YEARS) }
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    LaunchedEffect(selectedTab) {
        val cfg = loanTabConfigs[selectedTab]!!
        principal = cfg.defaultPrincipal
        rate = cfg.defaultRate
    }

    val emi = remember(principal, rate, tenure) {
        viewModel.calculateEmi(principal, rate, tenure)
    }

    val tenureDisplay = when (tenureUnit) {
        TenureUnit.YEARS -> "${tenure / 12} yr ${if (tenure % 12 > 0) "${tenure % 12} mo" else ""}".trim()
        TenureUnit.MONTHS -> "$tenure mo"
    }
    val tenureSliderValue = when (tenureUnit) {
        TenureUnit.YEARS -> ((tenure / 12 - 1).coerceAtLeast(0) / 29f).coerceIn(0f, 1f)
        TenureUnit.MONTHS -> ((tenure - 1) / 359f).coerceIn(0f, 1f)
    }
    val onTenureSliderChange: (Float) -> Unit = when (tenureUnit) {
        TenureUnit.YEARS -> { v -> tenure = (((v * 29) + 1).roundToInt().coerceIn(1, 30)) * 12 }
        TenureUnit.MONTHS -> { v -> tenure = ((v * 359) + 1).roundToInt().coerceIn(1, 360) }
    }
    val tenureMinLabel = if (tenureUnit == TenureUnit.YEARS) "1 yr" else "1 mo"
    val tenureMaxLabel = if (tenureUnit == TenureUnit.YEARS) "30 yr" else "360 mo"

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

            // Loan-type tabs in dark pill container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF312E81)),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    LoanTab.entries.forEach { tab ->
                        val isSelected = tab == selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                tab.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Indigo600 else Color(0xFFA5B4FC),
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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

                // Interest Rate stepper — +/- buttons per wireframe
                StepperField(
                    label = "Annual Interest Rate (%)",
                    displayValue = "%.2f%%".format(rate),
                    onDecrement = { rate = (rate - 0.1).coerceAtLeast(1.0).let { "%.1f".format(it).toDouble() } },
                    onIncrement = { rate = (rate + 0.1).coerceAtMost(30.0).let { "%.1f".format(it).toDouble() } },
                    accentColor = Color(0xFF0891B2),
                )

                // Tenure slider with Yr/Mo toggle
                SliderField(
                    label = "Loan Tenure",
                    displayValue = tenureDisplay,
                    sliderValue = tenureSliderValue,
                    onSliderChange = onTenureSliderChange,
                    inputValue = if (tenureUnit == TenureUnit.YEARS) (tenure / 12).toString() else tenure.toString(),
                    onInputChange = { v ->
                        v.toIntOrNull()?.let { input ->
                            when (tenureUnit) {
                                TenureUnit.YEARS -> { if (input in 1..30) tenure = input * 12 }
                                TenureUnit.MONTHS -> { if (input in 1..360) tenure = input }
                            }
                        }
                    },
                    valueRange = 0f..1f,
                    minLabel = tenureMinLabel,
                    maxLabel = tenureMaxLabel,
                    accentColor = SafeGreen,
                    headerTrailing = {
                        TenureUnitToggle(
                            selected = tenureUnit,
                            onSelect = { tenureUnit = it },
                        )
                    },
                )

                // Quick tenure chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(12, 24, 36, 60, 84, 120, 240).forEach { months ->
                        val selected = tenure == months
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Indigo600 else Indigo50),
                        ) {
                            TextButton(
                                onClick = { tenure = months },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "${months / 12}Y",
                                    fontSize = 11.sp,
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
private fun TenureUnitToggle(
    selected: TenureUnit,
    onSelect: (TenureUnit) -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEEF2FF)),
    ) {
        Row {
            TenureUnit.entries.forEach { unit ->
                val isActive = unit == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isActive) Indigo600 else Color.Transparent)
                        .clickable { onSelect(unit) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        unit.label,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) Color.White else Color(0xFF64748B),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperField(
    label: String,
    displayValue: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    accentColor: Color,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Decrement button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEF2FF))
                        .clickable(onClick = onDecrement),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("−", fontSize = 20.sp, color = accentColor, fontWeight = FontWeight.Bold)
                }

                // Value display
                Text(
                    displayValue,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Increment button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEF2FF))
                        .clickable(onClick = onIncrement),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", fontSize = 20.sp, color = accentColor, fontWeight = FontWeight.Bold)
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
    headerTrailing: @Composable (() -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (headerTrailing != null) headerTrailing()
                }
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
}
