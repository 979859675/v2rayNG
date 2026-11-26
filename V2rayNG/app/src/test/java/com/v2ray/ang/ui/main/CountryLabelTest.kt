package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Locale

class CountryLabelTest {

    @Test
    fun `ISO code is localized to the app locale`() {
        assertEquals("Japan", formatTestCountry("JP", Locale.ENGLISH))
        assertEquals("日本", formatTestCountry("jp", Locale.CHINESE))
    }

    @Test
    fun `unknown ISO code falls back to the probed code`() {
        assertEquals("XX", formatTestCountry("XX", Locale.ENGLISH))
    }

    @Test
    fun `a full country name reported by the IP API is kept as-is`() {
        assertEquals("Japan", formatTestCountry("Japan", Locale.ENGLISH))
        assertEquals("Hong Kong", formatTestCountry(" Hong Kong ", Locale.ENGLISH))
    }

    @Test
    fun `a missing probe result has no country label`() {
        assertNull(formatTestCountry(null, Locale.ENGLISH))
        assertNull(formatTestCountry("", Locale.ENGLISH))
        assertNull(formatTestCountry("   ", Locale.ENGLISH))
    }
}
