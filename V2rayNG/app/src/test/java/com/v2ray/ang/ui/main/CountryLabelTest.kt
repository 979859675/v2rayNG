package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Locale

class CountryLabelTest {
    private val jpFlag = "🇯🇵"

    @Test
    fun `ISO code is localized to the app locale and gets its real flag`() {
        assertEquals("$jpFlag Japan", formatTestCountry("JP", Locale.ENGLISH))
        assertEquals("$jpFlag 日本", formatTestCountry("jp", Locale.CHINESE))
    }

    @Test
    fun `unknown ISO code falls back to the probed code without a flag`() {
        assertEquals("XX", formatTestCountry("XX", Locale.ENGLISH))
        assertEquals("XX", countryCodeForLabel("XX", Locale.ENGLISH))
        assertFalse("XX" in Locale.getISOCountries().toSet())
    }

    @Test
    fun `recognized country names and localized names resolve to flags`() {
        assertEquals("$jpFlag Japan", formatTestCountry("Japan", Locale.ENGLISH))
        assertEquals("$jpFlag 日本", formatTestCountry("日本", Locale.CHINESE))
        assertEquals("🇬🇧 United Kingdom", formatTestCountry("UK", Locale.ENGLISH))
        assertEquals("GB", countryCodeForLabel("UK", Locale.ENGLISH))
    }

    @Test
    fun `an unrecognized country name is kept as-is without a flag`() {
        assertEquals("Atlantis", formatTestCountry("Atlantis", Locale.ENGLISH))
        assertEquals("Atlantis", formatTestCountry(" Atlantis ", Locale.ENGLISH))
        assertNull(countryCodeForLabel("Atlantis", Locale.ENGLISH))
    }

    @Test
    fun `a missing probe result has no country label`() {
        assertNull(formatTestCountry(null, Locale.ENGLISH))
        assertNull(formatTestCountry("", Locale.ENGLISH))
        assertNull(formatTestCountry("   ", Locale.ENGLISH))
    }

    @Test
    fun `flag emoji is built only from ISO alpha-2 letters`() {
        assertEquals(jpFlag, countryFlagEmoji("JP"))
        assertNull(countryFlagEmoji("jp"))
        assertNull(countryFlagEmoji("J"))
        assertNull(countryFlagEmoji("JPN"))
    }
}
