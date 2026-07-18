package com.emireminder.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.emireminder.app.ui.ads.AdConstants

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConstants.BANNER_AD_UNIT_ID,
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.White),
        factory = {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            adView.loadAd(AdRequest.Builder().build())
        },
    )
}
