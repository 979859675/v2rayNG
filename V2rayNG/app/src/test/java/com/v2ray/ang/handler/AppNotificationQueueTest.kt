package com.v2ray.ang.handler

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppNotificationQueueTest {

    @Test
    fun `message emitted before the resumed UI collector starts is buffered`() = runBlocking {
        val queue = AppNotificationQueue()
        val expected = AppNotificationData(
            message = "Backup exported successfully",
            type = AppNotificationType.SUCCESS,
            durationMs = 2500L,
        )
        queue.show(expected.message, expected.type, expected.durationMs)

        val received = withTimeout(1_000) { queue.notifications.first() }
        assertEquals(expected, received)
    }

    @Test
    fun `queued messages keep their order until the collector drains them`() = runBlocking {
        val queue = AppNotificationQueue()
        repeat(3) { index ->
            queue.show("message-$index", AppNotificationType.NORMAL, 2000L)
        }
        val received = mutableListOf<AppNotificationData>()
        val job = launch {
            queue.notifications.take(3).collect { received += it }
        }

        withTimeout(1_000) { job.join() }
        assertEquals(listOf("message-0", "message-1", "message-2"), received.map { it.message.toString() })
    }

    @Test
    fun `a full queue drops the oldest message and keeps the newest`() = runBlocking {
        val queue = AppNotificationQueue()
        repeat(20) { index ->
            queue.show("message-$index", AppNotificationType.NORMAL, 2000L)
        }
        val received = mutableListOf<AppNotificationData>()
        val job = launch {
            queue.notifications.take(16).collect { received += it }
        }

        withTimeout(1_000) { job.join() }
        assertEquals(16, received.size)
        assertEquals("message-4", received.first().message.toString())
        assertEquals("message-19", received.last().message.toString())
    }
}
