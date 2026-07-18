package com.emireminder.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object ConsentManager {

    fun requestConsent(activity: Activity, onConsentGathered: () -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Info updated — now load and show form if required
                if (consentInfo.isConsentFormAvailable) {
                    loadAndShowForm(activity, consentInfo, onConsentGathered)
                } else {
                    onConsentGathered()
                }
            },
            { _ ->
                // Consent info update failed — proceed without consent (best-effort)
                onConsentGathered()
            }
        )
    }

    private fun loadAndShowForm(
        activity: Activity,
        consentInfo: ConsentInformation,
        onConsentGathered: () -> Unit,
    ) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
            // formError non-null means the form failed to show; we proceed regardless
            onConsentGathered()
        }
    }

    fun canRequestAds(context: Context): Boolean {
        val status = UserMessagingPlatform.getConsentInformation(context).consentStatus
        return status == ConsentInformation.ConsentStatus.OBTAINED ||
            status == ConsentInformation.ConsentStatus.NOT_REQUIRED
    }
}
