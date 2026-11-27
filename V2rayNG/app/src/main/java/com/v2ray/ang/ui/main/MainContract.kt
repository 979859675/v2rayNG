package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.ConnectionTestResult
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.LocateTarget
import com.v2ray.ang.dto.entities.FavoriteFolder
import com.v2ray.ang.dto.entities.createFavoriteFolder as createFavoriteFolderInList
import com.v2ray.ang.dto.entities.deleteFavoriteFolder as deleteFavoriteFolderFromList
import com.v2ray.ang.dto.entities.renameFavoriteFolder as renameFavoriteFolderInList
import com.v2ray.ang.dto.entities.setServerFavoriteFolders as setServerFavoriteFoldersInList

/** Favorite folders are virtual server groups; their IDs are never subscription IDs. */
internal const val FAVORITE_GROUP_PREFIX = "favorite-folder:"
internal fun isFavoriteGroupId(groupId: String): Boolean = groupId.startsWith(FAVORITE_GROUP_PREFIX)
internal fun favoriteGroupId(folderId: String): String = "$FAVORITE_GROUP_PREFIX$folderId"
internal fun favoriteFolderId(groupId: String): String? =
    groupId.removePrefix(FAVORITE_GROUP_PREFIX).takeIf { isFavoriteGroupId(groupId) && it.isNotEmpty() }

/** Profile writes must not use a virtual favorite-group ID as a subscription ID. */
internal fun storageGroupId(groupId: String): String = if (isFavoriteGroupId(groupId)) "" else groupId

/** Locale-neutral state formatted only when it reaches the main UI. */
sealed interface MainStatus {
    data object Disconnected : MainStatus
    data object Connected : MainStatus
    data object Testing : MainStatus
    data class TestProgress(val progress: String) : MainStatus
    data class ConnectionTest(val result: ConnectionTestResult) : MainStatus
}

/**
 * Main UI state
 */
data class MainUiState(
    val groups: List<GroupMapItem> = emptyList(),
    val selectedGroupId: String = "",
    val selectedGuid: String? = null,
    val isRunning: Boolean = false,
    val isTesting: Boolean = false,
    val status: MainStatus = MainStatus.Disconnected,
    val searchQuery: String = "",
    val locateTarget: LocateTarget? = null,
    val confirmRemove: Boolean = false,
    val doubleColumnDisplay: Boolean = false,
    val shareQRCodeBitmap: android.graphics.Bitmap? = null,
    val favoriteFolders: List<FavoriteFolder> = emptyList()
)

/**
 * All possible user interaction intents
 */
sealed interface MainAction {
    data object Initialize : MainAction
    data object RefreshGroups : MainAction
    data object ToggleService : MainAction
    data object TestCurrentServer : MainAction
    data object TestAllServers : MainAction
    data object TestRealAllServers : MainAction
    data object CancelTesting : MainAction
    data object RemoveAllServers : MainAction
    data object RemoveDuplicateServers : MainAction
    data object RemoveInvalidServers : MainAction
    data object SortByTestResults : MainAction
    data object UpdateSubscriptions : MainAction
    data class UpdateGroupSubscription(val groupId: String) : MainAction
    data object ExportAll : MainAction

    data object ImportQRcode : MainAction
    data object ImportClipboard : MainAction
    data object ImportConfigLocal : MainAction
    data class ImportManually(val type: Int) : MainAction
    data object RestartService : MainAction
    data object LocateSelectedServer : MainAction

    data class SelectGroup(val groupId: String) : MainAction
    data class SelectServer(val guid: String) : MainAction
    data class RemoveServer(val guid: String) : MainAction
    data class EditServer(val guid: String, val profile: com.v2ray.ang.dto.entities.ProfileItem) : MainAction
    data class Search(val query: String) : MainAction
    data class ShareQRCode(val guid: String) : MainAction
    data class ShareClipboard(val guid: String) : MainAction
    data class ShareFullContent(val guid: String) : MainAction
    data object DismissQRCodeDialog : MainAction

    data class ImportBatchConfig(val configText: String) : MainAction

    data class LocateHandled(val target: LocateTarget) : MainAction
    data class MoveServer(val groupId: String, val fromGuid: String, val toGuid: String) : MainAction

    data class CreateFavoriteFolder(val id: String, val name: String) : MainAction
    data class RenameFavoriteFolder(val id: String, val name: String) : MainAction
    data class DeleteFavoriteFolder(val id: String) : MainAction
    data class SetServerFavoriteFolders(val guid: String, val folderIds: Set<String>) : MainAction

    data object Exit : MainAction
}

/** Pure, JVM-testable mapping from favorite-folder actions to the next durable folder list. */
internal fun applyFavoriteFolderAction(
    folders: List<FavoriteFolder>,
    action: MainAction,
): List<FavoriteFolder> = when (action) {
    is MainAction.CreateFavoriteFolder -> createFavoriteFolderInList(folders, action.id, action.name)
    is MainAction.RenameFavoriteFolder -> renameFavoriteFolderInList(folders, action.id, action.name)
    is MainAction.DeleteFavoriteFolder -> deleteFavoriteFolderFromList(folders, action.id)
    is MainAction.SetServerFavoriteFolders -> setServerFavoriteFoldersInList(folders, action.guid, action.folderIds)
    else -> folders
}

/** A no-op action must not create a write or replace the currently published state. */
internal fun shouldPersistFavoriteFolders(
    current: List<FavoriteFolder>,
    next: List<FavoriteFolder>,
): Boolean = current != next

/** A failed MMKV write must leave the previously loaded folder list unchanged. */
internal fun favoriteFoldersAfterSave(
    current: List<FavoriteFolder>,
    next: List<FavoriteFolder>,
    saved: Boolean,
): List<FavoriteFolder> = if (saved) next else current
