package com.emireminder.app.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import com.emireminder.app.data.repository.SmsFinanceRepository
import com.emireminder.app.service.SmsMonitorController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SMSReceiver : BroadcastReceiver() {

    @Inject lateinit var smsFinanceRepository: SmsFinanceRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        if (!hasReceiveSmsPermission(context)) {
            SmsMonitorController.stop(context)
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                messages.forEach { sms ->
                    smsFinanceRepository.processIncomingSms(
                        senderAddress = sms.originatingAddress ?: return@forEach,
                        body          = sms.messageBody ?: return@forEach,
                        receivedAtMs  = sms.timestampMillis,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun hasReceiveSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
}
