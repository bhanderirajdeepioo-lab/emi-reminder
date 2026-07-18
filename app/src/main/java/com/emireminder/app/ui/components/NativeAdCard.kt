package com.emireminder.app.ui.components

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.google.android.gms.ads.nativead.NativeAdView
import com.emireminder.app.R
import com.emireminder.app.ui.ads.AdConstants

@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        val loader = AdLoader.Builder(context, AdConstants.NATIVE_AD_UNIT_ID)
            .forNativeAd { ad -> nativeAd = ad }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // ad slot stays hidden on failure
                }
            })
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose { nativeAd?.destroy() }
    }

    val ad = nativeAd ?: return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Sponsored",
            fontSize = 10.sp,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    LayoutInflater.from(ctx)
                        .inflate(R.layout.native_ad_card, null, false) as NativeAdView
                },
                update = { adView ->
                    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
                    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
                    val iconView = adView.findViewById<ImageView>(R.id.ad_icon)
                    val ctaButton = adView.findViewById<Button>(R.id.ad_call_to_action)

                    adView.headlineView = headlineView
                    adView.bodyView = bodyView
                    adView.iconView = iconView
                    adView.callToActionView = ctaButton

                    headlineView.text = ad.headline
                    bodyView.text = ad.body ?: ""
                    ad.icon?.drawable?.let { iconView.setImageDrawable(it) }
                        ?: run { iconView.visibility = android.view.View.GONE }
                    if (ad.callToAction != null) {
                        ctaButton.text = ad.callToAction
                        ctaButton.visibility = android.view.View.VISIBLE
                    } else {
                        ctaButton.visibility = android.view.View.GONE
                    }

                    adView.setNativeAd(ad)
                },
            )
        }
    }
}
