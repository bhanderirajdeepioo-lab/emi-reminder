package com.emireminder.app.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emireminder.app.ui.theme.DarkBg
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LogoCircle    = Color(0xFF312E81)
private val RingColor     = Color(0xFF818CF8)
private val GlowColor     = Color(0xFF4F46E5)
private val BrandText     = Color(0xFFE0E7FF)
private val SubtitleColor = Color(0xFF818CF8)
private val PillText      = Color(0xFFa5b4fc)
private val DotDim        = Color(0xFF6366F1)
private val BlobTR        = Color(0xFF312E81)
private val BlobBL        = Color(0xFF3B0764)

@Composable
fun SplashScreen(
    isFirstLaunch: Boolean,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    var activeDot by remember { mutableStateOf(1) }
    // Read the live value at navigation time so the async IO read in AppNavGraph is visible here.
    val currentIsFirstLaunch by rememberUpdatedState(isFirstLaunch)

    LaunchedEffect(Unit) {
        launch { logoScale.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing)) }
        launch { logoAlpha.animateTo(1f, animationSpec = tween(350)) }
        delay(150)
        textAlpha.animateTo(1f, animationSpec = tween(300, easing = EaseInOut))
        delay(400)
        if (currentIsFirstLaunch) onNavigateToOnboarding() else onNavigateToHome()
    }

    LaunchedEffect("dots") {
        while (true) {
            delay(400)
            activeDot = (activeDot + 1) % 3
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
    ) {
        // Ambient blob — top right
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = 80.dp, y = (-60).dp)
                .clip(CircleShape)
                .background(BlobTR.copy(alpha = 0.55f))
                .align(Alignment.TopEnd),
        )
        // Ambient blob — bottom left
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-80).dp, y = 60.dp)
                .clip(CircleShape)
                .background(BlobBL.copy(alpha = 0.40f))
                .align(Alignment.BottomStart),
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo: outer glow → ring stroke → filled circle → ₹
            Box(
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .size(170.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(GlowColor.copy(alpha = 0.12f)),
                )
                Canvas(modifier = Modifier.size(128.dp)) {
                    drawCircle(
                        color = RingColor,
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(LogoCircle),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("₹", fontSize = 42.sp, color = BrandText, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier.alpha(textAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "EMI Reminder &",
                    color = BrandText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = "Calculator App",
                    color = BrandText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Your Smart Loan Manager",
                    color = SubtitleColor,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaglinePill("Calculate")
                    TaglinePill("Track")
                }
            }
        }

        // Animated loading dots — walking highlight cycles left → center → right
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .alpha(textAlpha.value),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (i == activeDot) BrandText else DotDim.copy(alpha = 0.5f)),
                )
            }
        }
    }
}

@Composable
private fun TaglinePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF312E81))
            .padding(horizontal = 16.dp, vertical = 5.dp),
    ) {
        Text(text, fontSize = 11.sp, color = PillText)
    }
}
