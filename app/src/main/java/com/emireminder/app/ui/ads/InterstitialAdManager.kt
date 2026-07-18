package com.emireminder.app.ui.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

private const val TAG = "InterstitialAdManager"
private const val FREQUENCY_CAP_MS = 3 * 60 * 1_000L // 3 minutes shared across all triggers

object InterstitialAdManager {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShownAt = 0L

    /** Call on screen entry to ensure the ad is ready when needed. */
    fun preload(context: Context) {
        if (isLoading || interstitialAd != null) return
        isLoading = true
        InterstitialAd.load(
            context,
            AdConstants.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial loaded")
                    interstitialAd = ad
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    isLoading = false
                }
            },
        )
    }

    /**
     * Show the interstitial if an ad is loaded and the 3-minute frequency cap has elapsed.
     * [onAdFinished] is always called — after the ad is dismissed, fails to show, or is skipped.
     */
    fun showIfAvailable(activity: Activity, onAdFinished: () -> Unit) {
        val now = System.currentTimeMillis()
        val ad = interstitialAd
        if (ad == null || (now - lastShownAt) < FREQUENCY_CAP_MS) {
            if (ad == null) preload(activity)
            onAdFinished()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastShownAt = System.currentTimeMillis()
                interstitialAd = null
            }
            override fun onAdDismissedFullScreenContent() {
                preload(activity)
                onAdFinished()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
                interstitialAd = null
                preload(activity)
                onAdFinished()
            }
        }
        ad.show(activity)
    }
}
