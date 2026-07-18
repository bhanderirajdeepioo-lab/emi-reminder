package com.emireminder.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

// Test interstitial ad unit ID — replace with production ID before release
private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
private const val FREQUENCY_CAP_MS = 3 * 60 * 1_000L // 3 minutes
private const val TAG = "InterstitialAdManager"

object InterstitialAdManager {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShowTimeMs = 0L

    fun preload(context: Context) {
        if (isLoading || interstitialAd != null) return
        isLoading = true
        InterstitialAd.load(
            context.applicationContext,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Interstitial loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.d(TAG, "Interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Show the interstitial if loaded and the 3-minute frequency cap allows.
     * Always calls [onComplete], whether or not the ad was shown.
     */
    fun showIfAvailable(activity: Activity, onComplete: () -> Unit = {}) {
        val ad = interstitialAd
        val capElapsed = System.currentTimeMillis() - lastShowTimeMs >= FREQUENCY_CAP_MS
        if (ad == null || !capElapsed) {
            onComplete()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastShowTimeMs = System.currentTimeMillis()
                interstitialAd = null
            }

            override fun onAdDismissedFullScreenContent() {
                onComplete()
                preload(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                onComplete()
            }
        }

        ad.show(activity)
    }
}
