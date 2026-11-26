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
        groupId.isNotEmpty() && searchQuery.isEmpty() && !isFiltered

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

/**
 * Turns the raw country observed by the egress-IP probe into a label for the app locale.
 * A two-letter ISO code is localized (JP -> "Japan" / "日本"); any other non-empty value is
 * already a real name reported by the IP API and is kept as-is. Unknown codes fall back to the code.
 */
internal fun formatTestCountry(raw: String?, locale: Locale): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (value.length != 2 || !value.all { it.isLetter() }) {
        return value
    }
    val code = value.uppercase(Locale.ROOT)
    val display = Locale("", code).getDisplayCountry(locale)
    return display.takeIf { it.isNotEmpty() && !it.equals(code, ignoreCase = true) } ?: code
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
