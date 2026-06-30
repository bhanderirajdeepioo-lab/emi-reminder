package io.helsy.emireminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SMSReceiver : BroadcastReceiver() {

    private val emiKeywords = listOf("emi", "loan", "payment due", "debit", "instalment", "installment")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { sms ->
            val body = sms.messageBody.lowercase()
            if (emiKeywords.any { it in body }) {
                // TODO: persist via WorkManager / coroutine outside broadcast receiver
            }
        }
    }
}
