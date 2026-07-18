package com.emireminder.app.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

// Test App Open ad unit ID — replace with production ID before release
private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
private const val TAG = "AppOpenAdManager"

object AppOpenAdManager : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var currentActivity: Activity? = null
    private var adShownOnce = false
    // Set when showAdIfAvailable() is called before the ad has loaded.
    // The load callback checks this and shows immediately on completion.
    private var pendingShow = false

    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        loadAd(application)
    }

    private fun loadAd(context: android.content.Context) {
        if (isLoadingAd || appOpenAd != null) return
        isLoadingAd = true
        AppOpenAd.load(
            context,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    Log.d(TAG, "App open ad loaded")
                    // If show was requested while we were still loading, show now
                    if (pendingShow) {
                        pendingShow = false
                        currentActivity?.let { showAdIfAvailable(it) }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                    pendingShow = false
                    Log.d(TAG, "App open ad failed to load: ${error.message}")
                }
            }
        )
    }

    /** Show the cold-launch App Open ad. If the ad isn't ready yet, queues it to show on load. */
    fun showAdIfAvailable(activity: Activity, onComplete: () -> Unit = {}) {
        if (adShownOnce) { onComplete(); return }
        if (isShowingAd) { onComplete(); return }
        val ad = appOpenAd ?: run {
            // Ad not loaded yet — mark pending so it shows as soon as it loads
            pendingShow = true
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                adShownOnce = true
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                onComplete()
                currentActivity?.application?.let { loadAd(it) }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                onComplete()
            }
        }

        ad.show(activity)
    }

    // ActivityLifecycleCallbacks — track the foreground activity
    override fun onActivityStarted(activity: Activity) { currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }
}
