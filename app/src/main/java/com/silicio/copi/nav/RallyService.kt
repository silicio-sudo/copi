package com.silicio.copi.nav

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.car.app.notification.CarAppExtender
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.silicio.copi.R
import com.silicio.copi.core.formatDistanceShort
import com.silicio.copi.core.formatDuration
import com.silicio.copi.core.formatSpeed
import com.silicio.copi.core.speedUnit
import com.silicio.copi.data.shortEs
import com.silicio.copi.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RallyService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastNotificationKey = ""
    private var lastNotificationAt = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundCompat(buildNotification(Copi.engine.state.value))
        scope.launch {
            Copi.engine.state.collect { state ->
                if (!state.running && !state.paused) {
                    stopSelf()
                    return@collect
                }
                val now = System.currentTimeMillis()
                val key = buildString {
                    append(state.roadbook?.id).append('|')
                    append(state.running).append('|')
                    append(state.paused).append('|')
                    append(state.nextIndex).append('|')
                    append((state.distanceToNextM ?: 0.0).toInt() / 10).append('|')
                    append((state.progress * 100).toInt()).append('|')
                    append(state.elapsedMs / 5_000)
                }
                if (key != lastNotificationKey || now - lastNotificationAt > 5_000) {
                    lastNotificationKey = key
                    lastNotificationAt = now
                    updateNotification(state)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> Copi.pauseNavigation()
            ACTION_RESUME -> Copi.resumeNavigation()
            ACTION_PREV -> Copi.previousNote()
            ACTION_NEXT -> Copi.skipNext()
            ACTION_MUTE -> Copi.toggleMute()
            ACTION_STOP -> {
                Copi.resetNavigation()
                stopSelf()
            }
            ACTION_OPEN -> {
                val open = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(open)
            }
        }
        if (engineReady) {
            updateNotification(Copi.engine.state.value)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private val engineReady: Boolean
        get() = runCatching { Copi.engine }.isSuccess

    private fun startForegroundCompat(notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }.onFailure {
            stopSelf()
        }
    }

    private fun updateNotification(state: RallyState) {
        if (!engineReady) return
        val notification = buildNotification(state)
        runCatching { startForegroundCompat(notification) }
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            runCatching { NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification) }
        }
    }

    private fun buildNotification(state: RallyState): Notification {
        val settings = Copi.settings.current
        val note = state.nextNote
        val distanceToNext = state.distanceToNextM
        val title = state.roadbook?.name ?: "Copi"
        val text = when {
            state.finished -> "Tramo completado"
            state.paused -> "En pausa · ${formatDuration(state.elapsedMs)}"
            note != null && distanceToNext != null -> buildString {
                append(note.shortEs())
                append(" · ")
                append(formatDistanceShort(distanceToNext, settings.units))
                append(" · ")
                append(formatSpeed(state.speedKmh, settings.units))
                append(' ')
                append(speedUnit(settings.units))
            }
            else -> "Copi listo"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_NAV)
            .setSmallIcon(R.drawable.ic_stat_copi)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(
                buildString {
                    append(if (state.demo) "DEMO · " else "")
                    append(formatDuration(state.elapsedMs))
                    append(" · ")
                    append(if (state.running && !state.paused) "en tramo" else "detenido")
                }
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(1000, (state.progress * 1000).toInt().coerceIn(0, 1000), false)
            .setContentIntent(activityIntent())

        if (state.paused) {
            builder.addAction(android.R.drawable.ic_media_play, "Reanudar", serviceIntent(ACTION_RESUME, 1))
        } else {
            builder.addAction(android.R.drawable.ic_media_pause, "Pausa", serviceIntent(ACTION_PAUSE, 2))
        }
        builder.addAction(android.R.drawable.ic_media_previous, "Anterior", serviceIntent(ACTION_PREV, 3))
        builder.addAction(android.R.drawable.ic_media_next, "Siguiente", serviceIntent(ACTION_NEXT, 4))
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Terminar", serviceIntent(ACTION_STOP, 5))

        runCatching {
            builder.extend(
                CarAppExtender.Builder()
                    .setContentTitle(title)
                    .setContentText(text)
                    .setImportance(NotificationManagerCompat.IMPORTANCE_LOW)
                    .build()
            )
        }
        return builder.build()
    }

    private fun activityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, RallyService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_NAV,
            "Navegación del tramo",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Estado del tramo, siguiente nota y controles del copiloto"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_SYNC = "com.silicio.copi.action.SYNC"
        const val ACTION_PAUSE = "com.silicio.copi.action.PAUSE"
        const val ACTION_RESUME = "com.silicio.copi.action.RESUME"
        const val ACTION_PREV = "com.silicio.copi.action.PREV"
        const val ACTION_NEXT = "com.silicio.copi.action.NEXT"
        const val ACTION_MUTE = "com.silicio.copi.action.MUTE"
        const val ACTION_STOP = "com.silicio.copi.action.STOP"
        const val ACTION_OPEN = "com.silicio.copi.action.OPEN"
        const val NOTIFICATION_ID = 42
        const val CHANNEL_NAV = "copi_navigation"
    }
}
