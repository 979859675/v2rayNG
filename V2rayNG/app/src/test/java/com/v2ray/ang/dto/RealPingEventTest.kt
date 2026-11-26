package com.v2ray.ang.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RealPingEventTest {

    @Test
    fun `TCPing and failed probes carry no country`() {
        val event = RealPingEvent.Result("guid-1", 42L)
        assertEquals("guid-1", event.guid)
        assertEquals(42L, event.delayMillis)
        assertNull(event.country)
    }

    @Test
    fun `a successful real-ping probe carries its observed country`() {
        val event = RealPingEvent.Result("guid-1", 42L, "JP")
        assertEquals("JP", event.country)
        assertEquals(event, RealPingEvent.Result("guid-1", 42L, "JP"))
    }
}
