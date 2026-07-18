package com.emireminder.app.ui.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

private const val TAG = "ConsentHelper"

object ConsentHelper {

    fun requestConsent(activity: Activity, onConsentGathered: (canRequestAds: Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError: FormError? ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.errorCode} ${formError.message}")
                    }
                    onConsentGathered(consentInfo.canRequestAds())
                }
            },
            { requestError: FormError ->
                Log.w(TAG, "Consent request error: ${requestError.errorCode} ${requestError.message}")
                // Allow ads if consent info update fails (non-EEA region likely)
                onConsentGathered(true)
            },
        )
    }

    fun canRequestAds(activity: Activity): Boolean =
        UserMessagingPlatform.getConsentInformation(activity).canRequestAds()
}
