package com.v2ray.ang.handler

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

enum class AppNotificationType {
    NORMAL, SUCCESS, ERROR, INFO
}

/**
 * Calculates display duration based on text length.
 * Base 1500ms + 30ms per character, min 2000ms (2.0s), max 5000ms (5.0s).
 */
fun calculateNotificationDuration(message: CharSequence): Long {
    val length = message.length
    val calculated = 1500L + (length * 30L)
    return calculated.coerceIn(2000L, 5000L)
}

data class AppNotificationData(
    val message: CharSequence,
    val type: AppNotificationType = AppNotificationType.NORMAL,
    val durationMs: Long = calculateNotificationDuration(message)
)

private const val MaxQueuedNotifications = 16

/**
 * Per-instance notification queue. Production uses the [AppNotificationManager] singleton;
 * JVM tests create a separate queue so queued events never touch the global UI stream.
 *
 * Android activity-result callbacks such as Storage Access Framework can run while an activity
 * is STARTED, before the global notification bridge resumes collection. The buffered Channel
 * prevents a success toast emitted in that gap from being dropped, without replaying an
 * already-consumed message to a later activity. Remove the buffering only if the bridge begins
 * collecting at STARTED and can safely render the popup before RESUMED.
 */
open class AppNotificationQueue {
    private val _notifications = Channel<AppNotificationData>(
        capacity = MaxQueuedNotifications,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val notifications = _notifications.receiveAsFlow()

    fun show(
        message: CharSequence,
        type: AppNotificationType = AppNotificationType.NORMAL,
        durationMs: Long = calculateNotificationDuration(message)
    ) {
        _notifications.trySend(
            AppNotificationData(
                message = message,
                type = type,
                durationMs = durationMs
            )
        )
    }
}

object AppNotificationManager : AppNotificationQueue()
