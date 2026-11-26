package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.ServerAffiliationInfo
import com.v2ray.ang.util.JsonUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ServerAffiliationInfoTest {

    @Test
    fun `preceding stored format without a country still decodes`() {
        val stored = """{"testDelayMillis":123}"""
        val aff = requireNotNull(JsonUtil.fromJsonSafe(stored, ServerAffiliationInfo::class.java))

        assertEquals(123L, aff.testDelayMillis)
        assertNull(aff.testCountry)
    }

    @Test
    fun `probed country round-trips through the stored JSON`() {
        val aff = ServerAffiliationInfo(testDelayMillis = 45L, testCountry = "JP")
        val decoded = requireNotNull(
            JsonUtil.fromJsonSafe(JsonUtil.toJson(aff), ServerAffiliationInfo::class.java)
        )

        assertEquals(45L, decoded.testDelayMillis)
        assertEquals("JP", decoded.testCountry)
    }

    @Test
    fun `an empty persisted affiliation has no country`() {
        assertNull(ServerAffiliationInfo().testCountry)
    }
}
