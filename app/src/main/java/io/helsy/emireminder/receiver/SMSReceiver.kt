package io.helsy.emireminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint
import io.helsy.emireminder.data.db.entity.SMSImport
import io.helsy.emireminder.data.repository.ReminderRepository
import io.helsy.emireminder.sms.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SMSReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderRepository: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                messages.forEach { sms ->
                    val result = SmsParser.parse(sms.originatingAddress ?: "", sms.messageBody ?: "")
                    if (result.confidence >= 0.4f) {
                        reminderRepository.insertSmsImport(
                            SMSImport(
                                senderAddress     = sms.originatingAddress ?: "",
                                rawBody           = sms.messageBody ?: "",
                                detectedLoanName  = result.bank.name,
                                detectedEmiAmount = result.emiAmount,
                                detectedDueDate   = "",
                            ),
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
