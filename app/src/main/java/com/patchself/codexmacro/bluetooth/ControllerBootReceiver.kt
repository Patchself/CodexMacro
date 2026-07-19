package com.patchself.codexmacro.bluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class ControllerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED || !CodexMicroService.shouldAutoResume(context)) return
        ContextCompat.startForegroundService(
            context,
            Intent(context, CodexMicroService::class.java).setAction(CodexMicroService.actionStart),
        )
    }
}
