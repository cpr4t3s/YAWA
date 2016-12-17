package com.isel.pdm.yawa

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationGenerator : BroadcastReceiver() {
    var NOTIFICATION_ID =  "notification-id"
    val NOTIFICATION =  "notification"

    override fun onReceive(context: Context, intent: Intent?) {
        if(intent != null) {
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification: Notification? = intent.getParcelableExtra(NOTIFICATION)
            if(notification != null) {
                val id: Int? = intent.getIntExtra(NOTIFICATION_ID, 0)
                id?.let { notificationManager.notify(it, notification) }
            }
        }
    }
}