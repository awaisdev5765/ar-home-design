package com.arhomedesign.homedesign.retention

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arhomedesign.MainActivity
import com.arhomedesign.homedesign.brand.samplePaintBrands
import java.util.Calendar

class DailyColorWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val flat = samplePaintBrands.flatMap { b -> b.colors.map { b.name to it } }
        val idx = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % flat.size
        val (brand, color) = flat[idx]
        val title = "Today's color: ${color.name}"
        val body = "See how ${color.name} looks in your home."
        maybeCreateChannel()
        val deepIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("arhome://arpreview?color=${color.hexColor}&brand=$brand&name=${color.name}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            1201,
            deepIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(9981, n)
        return Result.success()
    }

    private fun maybeCreateChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = applicationContext.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL_ID, "Daily Inspiration", NotificationManager.IMPORTANCE_DEFAULT)
        mgr.createNotificationChannel(ch)
    }

    companion object {
        const val CHANNEL_ID = "daily_color"
    }
}
