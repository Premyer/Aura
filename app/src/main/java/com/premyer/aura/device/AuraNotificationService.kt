package com.premyer.aura.device

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** No history, disk writes or automatic upload; snapshots are requested explicitly by the user. */
class AuraNotificationService : NotificationListenerService() {
    override fun onListenerConnected() { instance = this }
    override fun onListenerDisconnected() { if (instance === this) instance = null }
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = Unit

    fun snapshot(): String = activeNotifications?.take(30)?.joinToString("\n\n") { sbn ->
        val extras = sbn.notification.extras
        "${sbn.packageName}\n${extras.getCharSequence("android.title")?.toString().orEmpty()}\n" +
            extras.getCharSequence("android.text")?.toString().orEmpty().take(1000)
    }.orEmpty().ifBlank { "Keine aktiven Benachrichtigungen." }

    companion object {
        var instance: AuraNotificationService? = null
            private set
    }
}
