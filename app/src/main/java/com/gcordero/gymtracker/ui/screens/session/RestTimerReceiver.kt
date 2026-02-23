package com.gcordero.gymtracker.ui.screens.session

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.gcordero.gymtracker.R

class RestTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Fired by AlarmManager only in deep Doze / process killed scenarios
        vibrate(context)
        showNotification(context)
    }

    companion object {
        const val CHANNEL_ID = "rest_timer_channel"
        const val NOTIFICATION_ID = 1001

        fun vibrate(context: Context) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    manager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                val timings = longArrayOf(0, 700, 200, 700, 200, 700, 200, 700, 200, 700)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } catch (_: Exception) {}
        }

        fun showNotification(context: Context) {
            try {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Temporizador de descanso",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificación cuando termina el descanso entre series"
                    setSound(alarmUri, audioAttributes)
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("¡Tiempo de descanso terminado!")
                    .setContentText("¡Vuelve a entrenar! 💪")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setSound(alarmUri)
                    .build()

                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (_: Exception) {}
        }
    }
}
