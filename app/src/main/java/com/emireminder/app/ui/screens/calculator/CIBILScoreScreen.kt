package com.emireminder.app.ui.screens.calculator

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val CibilRed = Color(0xFFDC2626)
private val CibilRed50 = Color(0xFFFEF2F2)

private val BandColors = listOf(
    Color(0xFFDC2626), // Poor
    Color(0xFFEA580C), // Fair
    Color(0xFFD97706), // Average
    Color(0xFF65A30D), // Good
    Color(0xFF059669), // Very Good
    Color(0xFF047857), // Excellent
)

@Composable
fun CIBILScoreScreen(
    onBack: () -> Unit,
    viewModel: CIBILScoreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (state.triggerRewardedAd) {
        CibilRewardedAdOverlay(onDismiss = viewModel::onRewardedAdConsumed)
    }

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

                // Semi-circle 6-band gauge
                if (band != null && state.score != null) {
                    val needleProgress by animateFloatAsState(
                        targetValue = ((state.score!! - 300f) / 600f).coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
                        label = "needle",
                    )
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CibilSemiCircleGauge(
                                progress = needleProgress,
                                bandColor = bandColor,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${state.score}",
                                fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                                color = bandColor,
                            )
                            Text(
                                band.label,
                                fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                color = bandColor,
                            )
                            Text(
                                band.range,
                                fontSize = 12.sp, color = Color(0xFF94A3B8),
                            )
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

                // Tips + rewarded ad CTA
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
                            state.tips.forEachIndexed { _, tip ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.CheckCircle, contentDescription = null,
                                        tint = bandColor, modifier = Modifier.size(18.dp).padding(top = 1.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(tip, fontSize = 13.sp, color = Color(0xFF475569), lineHeight = 20.sp)
                                }
                            }
                            if (!state.adShownThisSession) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                OutlinedButton(
                                    onClick = viewModel::onRewardedAdTriggered,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = bandColor),
                                    border = ButtonDefaults.outlinedButtonBorder,
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Unlock personalised tips", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Factors card with progress bars (always shown)
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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    listOf(
                        Triple("Payment History", 0.35f, "Pay all EMIs & bills on time"),
                        Triple("Credit Utilization", 0.30f, "Keep card usage below 30%"),
                        Triple("Credit History Length", 0.15f, "Don't close old accounts"),
                        Triple("Credit Mix", 0.10f, "Mix of secured & unsecured loans"),
                        Triple("New Enquiries", 0.10f, "Avoid multiple loan applications"),
                    ).forEach { (factor, weight, practice) ->
                        CibilFactorRow(factor = factor, weight = weight, practice = practice)
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

@Composable
private fun CibilSemiCircleGauge(progress: Float, bandColor: Color) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val strokeDp = 22.dp
        val radiusDp = (maxWidth - strokeDp) / 2
        val canvasHeightDp = radiusDp + strokeDp

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeightDp),
        ) {
            val sw = strokeDp.toPx()
            val d = size.width - sw
            val topLeft = Offset(sw / 2f, sw / 2f)
            val arcSize = Size(d, d)
            val gap = 2f

            // Draw 6 equal band segments
            BandColors.forEachIndexed { i, color ->
                val segmentSweep = 180f / BandColors.size
                val start = 180f + i * segmentSweep + gap / 2f
                val sweep = segmentSweep - gap
                drawArc(
                    color = color.copy(alpha = 0.25f),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Butt),
                )
            }

            // Draw filled colored arc up to current position
            val filledSweep = 180f * progress
            if (filledSweep > 0f) {
                drawArc(
                    color = bandColor,
                    startAngle = 180f,
                    sweepAngle = filledSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                )
            }

            // Draw needle circle at current position
            val needleAngleDeg = 180f + filledSweep
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            val cx = size.width / 2f
            val cy = sw / 2f + d / 2f
            val r = d / 2f
            val nx = cx + r * cos(needleAngleRad).toFloat()
            val ny = cy + r * sin(needleAngleRad).toFloat()

            drawCircle(
                color = Color.White,
                radius = sw / 2f,
                center = Offset(nx, ny),
            )
            drawCircle(
                color = bandColor,
                radius = sw / 2f - 4f,
                center = Offset(nx, ny),
            )
        }
    }
}

@Composable
private fun CibilFactorRow(factor: String, weight: Float, practice: String) {
    val animatedProgress by animateFloatAsState(
        targetValue = weight,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "factor_$factor",
    )
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(factor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
            Text(
                "${(weight * 100).toInt()}%",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CibilRed,
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = CibilRed,
            trackColor = Color(0xFFFECACA),
            strokeCap = StrokeCap.Round,
        )
        Text(practice, fontSize = 11.sp, color = Color(0xFF64748B))
    }
}

@Composable
private fun CibilRewardedAdOverlay(onDismiss: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1_000L)
            secondsLeft--
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(CibilRed50, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = CibilRed, modifier = Modifier.size(28.dp))
                    }
                    Text("Personalised Credit Tips", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), textAlign = TextAlign.Center)
                    Text(
                        "Watch a short video to unlock 3 personalised improvement tips based on your score band.",
                        fontSize = 13.sp, color = Color(0xFF475569), textAlign = TextAlign.Center, lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ad closes in $secondsLeft s",
                        fontSize = 12.sp, color = Color(0xFF94A3B8),
                    )
                    LinearProgressIndicator(
                        progress = { 1f - secondsLeft / 5f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = CibilRed,
                        trackColor = Color(0xFFFECACA),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
