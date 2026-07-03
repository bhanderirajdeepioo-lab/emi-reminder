package com.emireminder.app.ui.screens.calculator

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val CibilRed = Color(0xFFDC2626)
private val CibilRed50 = Color(0xFFFEF2F2)

@Composable
fun CIBILScoreScreen(
    onBack: () -> Unit,
    viewModel: CIBILScoreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CIBIL Score", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CibilRed,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = CibilRed50,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // No history chip
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setNoHistory(!state.noHistory) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.noHistory,
                        onCheckedChange = { viewModel.setNoHistory(it) },
                        colors = CheckboxDefaults.colors(checkedColor = CibilRed),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("I have no credit history yet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                        Text("NH / first-time credit user", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
            }

            if (!state.noHistory) {
                // Score input
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Enter Your CIBIL Score (300–900)", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = state.scoreText,
                            onValueChange = viewModel::setScore,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                                color = state.band?.let { Color(it.colorHex) } ?: Color(0xFF1E293B),
                                textAlign = TextAlign.Center,
                            ),
                            placeholder = {
                                Text(
                                    "e.g. 750", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFCBD5E1), textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = state.scoreError != null,
                            supportingText = state.scoreError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CibilRed,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                            ),
                        )
                    }
                }
            }

            // Score band result (shown when we have a valid score or no-history mode)
            val band = state.band
            if (band != null || state.noHistory) {
                val bandColor = band?.let { Color(it.colorHex) } ?: Color(0xFF64748B)

                // Gauge
                if (band != null && state.score != null) {
                    val progress by animateFloatAsState(
                        targetValue = ((state.score!! - 300f) / 600f).coerceIn(0f, 1f),
                        animationSpec = tween(800),
                        label = "gauge",
                    )
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(140.dp),
                                    strokeWidth = 12.dp,
                                    color = bandColor,
                                    trackColor = Color(0xFFE2E8F0),
                                    strokeCap = StrokeCap.Round,
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${state.score}",
                                        fontSize = 36.sp, fontWeight = FontWeight.ExtraBold,
                                        color = bandColor,
                                    )
                                    Text(band.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = bandColor)
                                    Text(band.range, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }
                }

                // Description card
                if (state.description.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                if (state.noHistory) "No Credit History" else "What this means",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(state.description, fontSize = 13.sp, color = Color(0xFF475569), lineHeight = 20.sp)
                        }
                    }
                }

                // Tips
                if (state.tips.isNotEmpty()) {
                    Text(
                        "IMPROVEMENT TIPS",
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.tips.forEachIndexed { idx, tip ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.CheckCircle, contentDescription = null,
                                        tint = bandColor, modifier = Modifier.size(18.dp).padding(top = 1.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(tip, fontSize = 13.sp, color = Color(0xFF475569), lineHeight = 20.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Factors card (always shown)
            Text(
                "FACTORS AFFECTING YOUR SCORE",
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp),
            )
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("Payment History", "35%", "Pay all EMIs & bills on time"),
                        Triple("Credit Utilization", "30%", "Keep card usage below 30%"),
                        Triple("Credit History Length", "15%", "Don't close old accounts"),
                        Triple("Credit Mix", "10%", "Mix of secured & unsecured loans"),
                        Triple("New Enquiries", "10%", "Avoid multiple loan applications"),
                    ).forEach { (factor, weight, practice) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(factor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                                Text(practice, fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CibilRed50)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(weight, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CibilRed)
                            }
                        }
                        if (factor != "New Enquiries") HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }

            // Official CIBIL link
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Score entered manually. For your official credit score, visit CIBIL.com.",
                        fontSize = 12.sp, color = Color(0xFF92400E),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cibil.com")),
                            )
                        },
                        border = ButtonDefaults.outlinedButtonBorder,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Check official CIBIL score", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
