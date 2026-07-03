package com.emireminder.app.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

private val GstCyan     = Color(0xFF0891B2)
private val GstCyanDark = Color(0xFF0E7490)
private val GstCyan50   = Color(0xFFECFEFF)
private val GstCyanLight = Color(0xFFCFFAFE)

private val _gstFmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 2 }
private fun fmtGst(v: Double) = "₹${_gstFmt.format(v)}"

@Composable
fun GSTCalculatorScreen(
    onBack: () -> Unit,
    viewModel: GSTCalculatorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Interstitial ad: show once per session, 3s after first non-zero result
    var hasShownAdThisSession by remember { mutableStateOf(false) }
    var showInterstitialAd by remember { mutableStateOf(false) }
    val hasResult = state.gstAmount > 0.0

    LaunchedEffect(hasResult) {
        if (hasResult && !hasShownAdThisSession) {
            delay(3_000L)
            showInterstitialAd = true
            hasShownAdThisSession = true
        }
    }

    if (showInterstitialAd) {
        InterstitialAdOverlay(onDismiss = { showInterstitialAd = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GST Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GstCyan,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = GstCyan50,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Add / Remove GST toggle
            SegmentedToggle(
                options = listOf(GstMode.ADD to "Add GST", GstMode.REMOVE to "Remove GST"),
                selected = state.mode,
                onSelect = viewModel::setMode,
                containerColor = GstCyanDark,
                selectedTextColor = GstCyan,
                unselectedTextColor = GstCyanLight,
            )

            // Amount card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (state.mode == GstMode.ADD) "Original Amount (excl. GST)"
                        else "Total Amount (incl. GST)",
                        fontSize = 12.sp, color = Color(0xFF64748B),
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.amountText,
                        onValueChange = viewModel::setAmountText,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B),
                        ),
                        prefix = { Text("₹ ", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GstCyan,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                    )
                }
            }

            // GST Rate chips (0 / 5 / 12 / 18 / 28%)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("GST Rate", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        state.gstRates.forEach { rate ->
                            val selected = state.selectedRate == rate
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) GstCyan else GstCyanLight)
                                    .clickable { viewModel.setRate(rate) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$rate%", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else GstCyanDark,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            // Intra-state / Inter-state toggle
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Transaction Type", fontSize = 12.sp, color = Color(0xFF64748B))
                    Spacer(Modifier.height(8.dp))
                    SegmentedToggle(
                        options = listOf(
                            GstTransactionType.INTRA to "Intra-State",
                            GstTransactionType.INTER to "Inter-State",
                        ),
                        selected = state.transactionType,
                        onSelect = viewModel::setTransactionType,
                        containerColor = Color(0xFFF1F5F9),
                        selectedTextColor = Color.White,
                        unselectedTextColor = GstCyanDark,
                        selectedBg = GstCyan,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.transactionType == GstTransactionType.INTRA)
                            "Within the same state — CGST + SGST applies"
                        else
                            "Between different states — IGST applies",
                        fontSize = 11.sp, color = Color(0xFF64748B),
                    )
                }
            }

            // Section label
            Text(
                "GST BREAKDOWN",
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp),
            )

            // Hero result card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (state.mode == GstMode.ADD) "Total (incl. GST)" else "GST Included in Amount",
                        fontSize = 13.sp, color = Color(0xFF94A3B8),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fmtGst(state.total),
                        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = GstCyan,
                    )
                }
            }

            // Breakdown cards — 3-up for intra-state, 2-up for inter-state
            if (state.transactionType == GstTransactionType.INTRA) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "GST Amount" to fmtGst(state.gstAmount),
                        "CGST (50%)" to fmtGst(state.cgst),
                        "SGST (50%)" to fmtGst(state.sgst),
                    ).forEach { (label, value) ->
                        BreakdownCard(Modifier.weight(1f), label, value)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BreakdownCard(Modifier.weight(1f), "GST Amount", fmtGst(state.gstAmount))
                    BreakdownCard(Modifier.weight(1f), "IGST (100%)", fmtGst(state.igst))
                }
            }

            // Cess disclaimer for 28% rate
            if (state.selectedRate == 28) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text("⚠️", fontSize = 14.sp)
                        Text(
                            "28% GST applies to luxury & sin goods. Additional cess " +
                                "(e.g. 1–22%) may apply on certain items. Verify the exact cess " +
                                "for your product category before filing.",
                            fontSize = 11.sp, color = Color(0xFF92400E), lineHeight = 16.sp,
                        )
                    }
                }
            }

            // CTA
            Button(
                onClick = { /* results update live */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GstCyan),
            ) {
                Text("Calculate GST →", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun <T> SegmentedToggle(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    containerColor: Color,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    selectedBg: Color = Color.White,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .padding(4.dp),
    ) {
        Row {
            options.forEach { (value, label) ->
                val isSelected = selected == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) selectedBg else Color.Transparent)
                        .clickable { onSelect(value) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) selectedTextColor else unselectedTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
    }
}

@Composable
private fun InterstitialAdOverlay(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GstCyan50),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Track all your taxes in one place",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B), textAlign = TextAlign.Center,
                        )
                        Text(
                            "EMI Reminder Pro — upgrade today",
                            fontSize = 12.sp, color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Skip", color = Color(0xFF64748B))
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GstCyan),
                    ) {
                        Text("Learn More")
                    }
                }
            }
        }
    }
}
