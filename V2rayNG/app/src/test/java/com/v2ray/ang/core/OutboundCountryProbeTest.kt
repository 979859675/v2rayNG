package com.v2ray.ang.core

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OutboundCountryProbeTest {

    private val speedtestConfig = """
        {"log":{"loglevel":"warning"},
         "inbounds":[{"tag":"socks","port":10808,"protocol":"socks"}],
         "outbounds":[{"tag":"proxy","protocol":"vless","settings":{"vnext":[{"address":"example.com","port":443}]}}],
         "routing":{"rules":[]}}
    """.trimIndent()

    @Test
    fun `probe config replaces inbounds with a single loopback HTTP inbound`() {
        val json = JsonParser.parseString(
            requireNotNull(OutboundCountryProbe.buildProbeConfig(speedtestConfig, 20812))
        ).asJsonObject

        val inbounds = json.getAsJsonArray("inbounds")
        assertEquals(1, inbounds.size())
        val inbound = inbounds[0].asJsonObject
        assertEquals("real-ping-country-probe", inbound["tag"].asString)
        assertEquals("http", inbound["protocol"].asString)
        assertEquals("127.0.0.1", inbound["listen"].asString)
        assertEquals(20812, inbound["port"].asInt)

        // Only the inbound changes: the probe must still use this config's real outbound/routing.
        assertEquals("proxy", json.getAsJsonArray("outbounds")[0].asJsonObject["tag"].asString)
        assertEquals(0, json.getAsJsonObject("routing").getAsJsonArray("rules").size())
    }

    @Test
    fun `probe config rejects a JSON array`() {
        assertNull(OutboundCountryProbe.buildProbeConfig("[1,2]", 20813))
    }

    @Test
    fun `probe config rejects malformed JSON`() {
        assertNull(OutboundCountryProbe.buildProbeConfig("{oops", 20814))
    }
}
