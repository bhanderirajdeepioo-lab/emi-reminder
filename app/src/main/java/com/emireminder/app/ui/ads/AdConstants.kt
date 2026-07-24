package com.emireminder.app.ui.ads

import com.emireminder.app.BuildConfig

object AdConstants {
    val APP_OPEN_AD_UNIT_ID = if (BuildConfig.DEBUG)
        "ca-app-pub-3940256099942544/9257395921"
    else
        "<YOUR_PRODUCTION_APP_OPEN_AD_UNIT_ID>"

    val BANNER_AD_UNIT_ID = if (BuildConfig.DEBUG)
        "ca-app-pub-3940256099942544/6300978111"
    else
        "<YOUR_PRODUCTION_BANNER_AD_UNIT_ID>"

    val INTERSTITIAL_AD_UNIT_ID = if (BuildConfig.DEBUG)
        "ca-app-pub-3940256099942544/1033173712"
    else
        "<YOUR_PRODUCTION_INTERSTITIAL_AD_UNIT_ID>"

    val NATIVE_AD_UNIT_ID = if (BuildConfig.DEBUG)
        "ca-app-pub-3940256099942544/2247696110"
    else
        "<YOUR_PRODUCTION_NATIVE_AD_UNIT_ID>"

    const val APP_OPEN_FREQUENCY_CAP_HOURS = 4L
}
