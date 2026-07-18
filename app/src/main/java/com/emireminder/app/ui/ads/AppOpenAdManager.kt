package com.emireminder.app.ui.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

private const val TAG = "AppOpenAdManager"

object AppOpenAdManager : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var loadTime = 0L
    private var isShowingAd = false
    // Prevents showing ad during splash/onboarding
    private var isAppReady = false
    private var wasInBackground = false

    private var currentActivity: Activity? = null

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        loadAd(application)
    }

    /** Call once the user has passed splash/onboarding and ads are appropriate. */
    fun markAppReady() {
        isAppReady = true
    }

    // ── ActivityLifecycleCallbacks ──────────────────────────────────────────────

    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        if (!isShowingAd) currentActivity = activity
        if (wasInBackground && isAppReady) {
            wasInBackground = false
            showAdIfAvailable(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        wasInBackground = true
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    // ── Ad loading ─────────────────────────────────────────────────────────────

    fun loadAd(context: Context) {
        if (isLoading || isAdAvailable()) return
        isLoading = true
        AppOpenAd.load(
            context,
            AdConstants.APP_OPEN_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "Ad loaded")
                    appOpenAd = ad
                    loadTime = Date().time
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Ad failed to load: ${error.message}")
                    isLoading = false
                }
            },
        )
    }

    fun showAdIfAvailable(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (isShowingAd) return
        if (!isAdAvailable()) {
            loadAd(activity)
            onAdDismissed()
            return
        }
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                loadAd(activity)
                onAdDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                onAdDismissed()
            }
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }
        isShowingAd = true
        appOpenAd?.show(activity)
    }

    private fun isAdAvailable(): Boolean =
        appOpenAd != null && wasLoadedWithinFourHours()

    private fun wasLoadedWithinFourHours(): Boolean {
        val millisPerHour = 3_600_000L
        return (Date().time - loadTime) < (AdConstants.APP_OPEN_FREQUENCY_CAP_HOURS * millisPerHour)
    }
}
