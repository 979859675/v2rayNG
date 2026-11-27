package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.moveItem
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.handler.AngConfigManager
import java.util.Locale

internal data class ServerRowUiModel(
    val guid: String,
    val profile: ProfileItem,
    val remarks: String,
    val statistics: String,
    val typeDescription: String,
    val testDelayMillis: Long,
    val subscriptionBadge: String,
    val testCountry: String? = null,
)

internal data class ServerGroupUiState(
    val servers: List<ServersCache> = emptyList(),
    val rows: List<ServerRowUiModel> = emptyList(),
    val isFiltered: Boolean = false,
) {
    fun canReorder(groupId: String, searchQuery: String): Boolean =
        groupId.isNotEmpty() && !isFavoriteGroupId(groupId) && searchQuery.isEmpty() && !isFiltered

    fun moveServer(fromGuid: String, toGuid: String): ServerGroupUiState? {
        if (isFiltered) return null
        val from = servers.indexOfFirst { it.guid == fromGuid }
        val to = servers.indexOfFirst { it.guid == toGuid }
        if (from == to || from < 0 || to < 0) return null
        return ServerGroupUiState(
            servers = servers.toMutableList().apply { moveItem(from, to) },
            rows = rows.toMutableList().apply { moveItem(from, to) }
        )
    }
}

internal fun buildServerRowUiModel(
    server: ServersCache,
    subscriptionRemarks: String,
): ServerRowUiModel {
    val profile = server.profile
    return ServerRowUiModel(
        guid = server.guid,
        profile = profile,
        remarks = profile.remarks,
        statistics = profile.description.nullIfBlank()
            ?: AngConfigManager.generateDescription(profile),
        typeDescription = serverProtocolDescription(profile),
        testDelayMillis = server.testDelayMillis,
        subscriptionBadge = subscriptionRemarks.firstOrNull()?.toString().orEmpty(),
        testCountry = server.testCountry,
    )
}

private val isoCountryCodes by lazy { Locale.getISOCountries().toSet() }
private val whitespacePattern = Regex("\\s+")

private fun countryNameKey(value: String): String {
    val normalized = buildString(value.length) {
        value.trim().lowercase(Locale.ROOT).forEach { letter ->
            append(if (letter.isLetterOrDigit()) letter else ' ')
        }
    }.trim()
    return whitespacePattern.replace(normalized, " ")
}

private val englishCountryNameToCode by lazy {
    Locale.getISOCountries().associate { code ->
        countryNameKey(Locale("", code).getDisplayCountry(Locale.ENGLISH)) to code
    }.filterKeys { it.isNotEmpty() }
}

private val commonCountryNameAliases = mapOf(
    "uk" to "GB",
    "united kingdom of great britain and northern ireland" to "GB",
    "united states of america" to "US",
    "usa" to "US",
    "south korea" to "KR",
    "republic of korea" to "KR",
    "north korea" to "KP",
    "russia" to "RU",
    "russian federation" to "RU",
    "vietnam" to "VN",
    "viet nam" to "VN",
    "iran" to "IR",
    "taiwan" to "TW",
    "hong kong" to "HK",
    "macao" to "MO",
    "macau" to "MO",
    "czech republic" to "CZ",
    "the netherlands" to "NL",
    "holland" to "NL",
    "laos" to "LA",
    "syria" to "SY",
    "moldova" to "MD",
    "tanzania" to "TZ",
    "bolivia" to "BO",
    "venezuela" to "VE",
    "brunei" to "BN",
    "cape verde" to "CV",
    "swaziland" to "SZ",
    "macedonia" to "MK",
).mapKeys { (key, _) -> countryNameKey(key) }

/**
 * Resolves a real country name or ISO code reported by the egress-IP API to an ISO country code.
 * Localized names from the app locale are supported (中国 -> CN), but an unrecognized value
 * never gets a guessed flag.
 */
internal fun countryCodeForLabel(raw: String?, locale: Locale): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val key = countryNameKey(value)
    Locale.getISOCountries().firstOrNull { code ->
        val display = Locale("", code).getDisplayCountry(locale)
        countryNameKey(display) == key && !display.equals(code, ignoreCase = true)
    }?.let { return it }
    commonCountryNameAliases[key]?.let { return it }
    englishCountryNameToCode[key]?.let { return it }
    if (value.length == 2 && value.all { it in 'A'..'Z' || it in 'a'..'z' }) {
        return value.uppercase(Locale.ROOT)
    }
    return null
}

/**
 * Turns the raw country observed by the egress-IP probe into "flag + localized name".
 * A known ISO code or country name becomes its regional-indicator flag plus a localized name
 * (JP -> "🇯🇵 Japan" / "🇯🇵 日本"). Unrecognized values are kept as-is without a flag:
 * the flag is never guessed from a server address.
 */
internal fun formatTestCountry(raw: String?, locale: Locale): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val code = countryCodeForLabel(value, locale) ?: return value
    if (code !in isoCountryCodes) return value
    val display = Locale("", code).getDisplayCountry(locale)
    val name = display.takeIf { it.isNotEmpty() && !it.equals(code, ignoreCase = true) } ?: return value
    val flag = countryFlagEmoji(code) ?: return name
    return "$flag $name"
}

/** Regional-indicator pair for an ISO 3166-1 alpha-2 code, e.g. JP -> 🇯🇵. */
internal fun countryFlagEmoji(code: String): String? {
    if (code.length != 2 || !code.all { it in 'A'..'Z' }) return null
    return buildString {
        code.forEach { letter ->
            append(Character.toChars(0x1F1E6 + (letter - 'A')))
        }
    }
}

private fun serverProtocolDescription(profile: ProfileItem): String {
    if (profile.configType.isComplexType()) return profile.configType.name
    val parts = mutableListOf(profile.configType.name)
    profile.network?.let { network ->
        if (network.isNotBlank() && !network.equals("tcp", ignoreCase = true)) {
            parts.add(network)
        }
    }
    profile.security?.let { security ->
        if (security.isNotBlank()) {
            parts.add(
                if (profile.insecure == true && security.equals("tls", ignoreCase = true)) {
                    "$security insecure"
                } else {
                    security
                }
            )
        }
    }
    return parts.joinToString(" / ")
}
