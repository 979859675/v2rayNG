package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.enums.EConfigType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class TestResultUpdateTest {

    private val profile = ProfileItem(configType = EConfigType.VLESS)
    private val servers = listOf("a", "b").map { ServersCache(it, profile) }
    private val rows = listOf("a", "b").map { ServerRowUiModel(it, profile, "", "", "", 0L, "") }

    @Test
    fun `initial state has neither delay nor country`() {
        servers.forEach {
            assertEquals(0L, it.testDelayMillis)
            assertNull(it.testCountry)
        }
        rows.forEach {
            assertEquals(0L, it.testDelayMillis)
            assertNull(it.testCountry)
        }
    }

    @Test
    fun `empty update leaves both collections unchanged`() {
        val updatedServers = applyTestDelayResults(servers, emptyMap())
        val updatedRows = applyTestDelayResultsToRows(rows, emptyMap())
        assertEquals(servers, updatedServers)
        assertEquals(rows, updatedRows)
        assertSame(servers[0], updatedServers[0])
    }

    @Test
    fun `successful real ping applies delay and probed country by GUID`() {
        val updates = mapOf("a" to TestResultUpdate(123L, "JP"))
        val updatedServers = applyTestDelayResults(servers, updates)
        val updatedRows = applyTestDelayResultsToRows(rows, updates)

        assertEquals(123L, updatedServers[0].testDelayMillis)
        assertEquals("JP", updatedServers[0].testCountry)
        assertEquals(123L, updatedRows[0].testDelayMillis)
        assertEquals("JP", updatedRows[0].testCountry)
        assertEquals(0L, updatedServers[1].testDelayMillis)
        assertNull(updatedServers[1].testCountry)
    }

    @Test
    fun `failed real ping or TCPing clears a stale country`() {
        val withCountry = listOf(
            servers[0].copy(testDelayMillis = 88L, testCountry = "US"),
            servers[1]
        )
        val rowsWithCountry = listOf(
            rows[0].copy(testDelayMillis = 88L, testCountry = "US"),
            rows[1]
        )
        val updates = mapOf("a" to TestResultUpdate(-1L, null))

        val updatedServers = applyTestDelayResults(withCountry, updates)
        val updatedRows = applyTestDelayResultsToRows(rowsWithCountry, updates)
        assertEquals(-1L, updatedServers[0].testDelayMillis)
        assertNull(updatedServers[0].testCountry)
        assertEquals(-1L, updatedRows[0].testDelayMillis)
        assertNull(updatedRows[0].testCountry)
    }

    @Test
    fun `a new country is applied even when the delay is unchanged`() {
        val withCountry = listOf(servers[0].copy(testDelayMillis = 123L, testCountry = "JP"), servers[1])
        val rowsWithCountry = listOf(rows[0].copy(testDelayMillis = 123L, testCountry = "JP"), rows[1])
        val updates = mapOf("a" to TestResultUpdate(123L, "SG"))

        assertEquals("SG", applyTestDelayResults(withCountry, updates)[0].testCountry)
        assertEquals("SG", applyTestDelayResultsToRows(rowsWithCountry, updates)[0].testCountry)
    }

    @Test
    fun `an update for an unknown GUID is ignored`() {
        val updates = mapOf("missing" to TestResultUpdate(50L, "DE"))
        assertEquals(servers, applyTestDelayResults(servers, updates))
        assertEquals(rows, applyTestDelayResultsToRows(rows, updates))
    }
}
