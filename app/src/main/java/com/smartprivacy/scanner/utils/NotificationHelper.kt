package com.smartprivacy.scanner.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartprivacy.scanner.R
import com.smartprivacy.scanner.ui.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "risk_alerts"
    const val CHANNEL_NAME = "Privacy & Security Alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val descriptionText = "Notifications for app scanning and risk alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showScanningNotification(context: Context, packageName: String, appName: String): Int {
        val notificationId = packageName.hashCode()
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Scanning $appName")
            .setContentText("Analyzing privacy risks and malware...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setProgress(0, 0, true) // Indeterminate progress
            .setOngoing(true)
            .setAutoCancel(false)

        notify(context, notificationId, builder)
        return notificationId
    }

    fun showRiskNotification(context: Context, packageName: String, appName: String, riskScore: Int, reason: String = "High Privacy Risk Detected!") {
        val notificationId = packageName.hashCode()
        
        // Create an explicit intent for an Activity in your app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_package", packageName) // To open specific app details if implemented
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reason)
            .setContentText("$appName has a risk score of $riskScore. Tap to review.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$appName has a risk score of $riskScore.\nReason: $reason"))

        notify(context, notificationId, builder)
    }
    
    fun showSafeNotification(context: Context, packageName: String, appName: String) {
        val notificationId = packageName.hashCode()
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Scan Complete")
            .setContentText("$appName is safe.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(3000) // Auto dismiss after 3 seconds

        notify(context, notificationId, builder)
    }

    fun cancelNotification(context: Context, packageName: String) {
        val notificationId = packageName.hashCode()
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun notify(context: Context, id: Int, builder: NotificationCompat.Builder) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(id, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
