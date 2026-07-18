package com.emireminder.app.ui.components

import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

// Test native ad unit ID — replace with production ID before release
private const val NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

@Composable
fun NativeAdCardView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    // Capture theme colors in composable scope for use inside AndroidView factory
    val colorScheme = MaterialTheme.colorScheme
    val onSurfaceArgb = colorScheme.onSurface.toArgb()
    val onSurfaceVariantArgb = colorScheme.onSurfaceVariant.toArgb()
    val primaryArgb = colorScheme.primary.toArgb()
    val onPrimaryArgb = colorScheme.onPrimary.toArgb()

    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context.applicationContext, NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                if (nativeAd == null) nativeAd = ad else ad.destroy()
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadFailed = true
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
        onDispose { nativeAd?.destroy() }
    }

    if (!loadFailed) {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "Sponsored",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, bottom = 2.dp),
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                val ad = nativeAd
                if (ad != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx ->
                            val dp = ctx.resources.displayMetrics.density
                            val pad = (12 * dp).toInt()
                            val iconSz = (40 * dp).toInt()
                            val gap = (8 * dp).toInt()
                            val ctaH = (48 * dp).toInt()

                            val iconImg = ImageView(ctx).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                layoutParams = LinearLayout.LayoutParams(iconSz, iconSz).also {
                                    it.marginEnd = gap
                                }
                            }
                            val headlineView = TextView(ctx).apply {
                                setTextColor(onSurfaceArgb)
                                textSize = 14f
                                setTypeface(null, Typeface.BOLD)
                                maxLines = 1
                            }
                            val bodyView = TextView(ctx).apply {
                                setTextColor(onSurfaceVariantArgb)
                                textSize = 12f
                                maxLines = 2
                                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                                    it.topMargin = (4 * dp).toInt()
                                }
                            }
                            val textCol = LinearLayout(ctx).apply {
                                orientation = LinearLayout.VERTICAL
                                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                                addView(headlineView)
                                addView(bodyView)
                            }
                            val topRow = LinearLayout(ctx).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER_VERTICAL
                                addView(iconImg)
                                addView(textCol)
                            }
                            val ctaBtn = android.widget.Button(ctx).apply {
                                setTextColor(onPrimaryArgb)
                                textSize = 11f
                                setBackgroundColor(primaryArgb)
                                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, ctaH).also {
                                    it.topMargin = gap
                                }
                            }
                            val inner = LinearLayout(ctx).apply {
                                orientation = LinearLayout.VERTICAL
                                setPadding(pad, pad, pad, pad)
                                addView(topRow)
                                addView(ctaBtn)
                            }

                            headlineView.text = ad.headline ?: ""
                            bodyView.text = ad.body ?: ""
                            ctaBtn.text = ad.callToAction ?: "Learn More"
                            ad.icon?.drawable?.let { iconImg.setImageDrawable(it) }

                            NativeAdView(ctx).apply {
                                addView(inner, android.view.ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                                this.headlineView = headlineView
                                this.bodyView = bodyView
                                this.callToActionView = ctaBtn
                                this.iconView = iconImg
                                setNativeAd(ad)
                            }
                        },
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .background(colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = colorScheme.onSurfaceVariant,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }
    }
}
