package com.v2ray.ang.dto.entities

import com.google.gson.JsonParser

/**
 * A user-named folder of server GUIDs. The folder owns the membership, so deleting a folder
 * never touches the profiles themselves. Folder and server identities are stable IDs, never
 * list positions.
 */
data class FavoriteFolder(
    val id: String,
    val name: String,
    val serverGuids: List<String> = emptyList(),
)

/**
 * Reads the persisted folder list. Gson allocates data classes without calling their
 * constructors, so missing/legacy fields are normalized here instead of relying on defaults.
 * A missing, blank, malformed, or pre-feature payload yields an empty list.
 */
internal fun decodeFavoriteFolders(json: String?): List<FavoriteFolder> {
    if (json.isNullOrBlank()) return emptyList()
    val array = runCatching { JsonParser.parseString(json).asJsonArray }.getOrNull() ?: return emptyList()
    return array.mapNotNull { element ->
        val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
        val id = obj.get("id")?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        val name = obj.get("name")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
        val guids = obj.get("serverGuids")
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?.mapNotNull { guid -> guid.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() } }
            .orEmpty()
            .distinct()
        FavoriteFolder(id = id, name = name, serverGuids = guids)
    }
}

internal fun createFavoriteFolder(folders: List<FavoriteFolder>, id: String, name: String): List<FavoriteFolder> {
    val trimmed = name.trim()
    if (id.isBlank() || trimmed.isEmpty() || folders.any { it.id == id }) return folders
    return folders + FavoriteFolder(id = id, name = trimmed)
}

internal fun renameFavoriteFolder(folders: List<FavoriteFolder>, id: String, name: String): List<FavoriteFolder> {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return folders
    return folders.map { folder -> if (folder.id == id) folder.copy(name = trimmed) else folder }
}

internal fun deleteFavoriteFolder(folders: List<FavoriteFolder>, id: String): List<FavoriteFolder> =
    folders.filterNot { it.id == id }

/** Replaces one server's membership across all folders with exactly `folderIds`. */
internal fun setServerFavoriteFolders(
    folders: List<FavoriteFolder>,
    guid: String,
    folderIds: Set<String>,
): List<FavoriteFolder> {
    if (guid.isBlank()) return folders
    return folders.map { folder ->
        val guids = folder.serverGuids.filterNot { it == guid }.toMutableList()
        if (folder.id in folderIds) guids.add(guid)
        folder.copy(serverGuids = guids.distinct())
    }
}

internal fun folderIdsContaining(folders: List<FavoriteFolder>, guid: String): Set<String> =
    folders.filter { guid in it.serverGuids }.map { it.id }.toSet()

/** Drops GUIDs of profiles that no longer exist, so deleted servers cannot linger in folders. */
internal fun pruneFavoriteFolders(folders: List<FavoriteFolder>, validGuids: Set<String>): List<FavoriteFolder> =
    folders.map { folder -> folder.copy(serverGuids = folder.serverGuids.filter { it in validGuids }) }
