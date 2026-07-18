package com.emireminder.app.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Thin helper for starting and stopping [SmsMonitorService].
 *
 * Always call [start] after the user grants RECEIVE_SMS permission.
 * Always call [stop] if the user explicitly disables SMS monitoring or revokes permission.
 */
object SmsMonitorController {

    fun start(context: Context) {
        if (!hasReceiveSmsPermission(context)) return
        val intent = Intent(context, SmsMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, SmsMonitorService::class.java))
    }

    fun hasReceiveSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
}
