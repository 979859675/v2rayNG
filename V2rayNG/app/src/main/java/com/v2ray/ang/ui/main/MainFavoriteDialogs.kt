package com.v2ray.ang.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.FavoriteFolder
import com.v2ray.ang.dto.entities.folderIdsContaining
import com.v2ray.ang.ui.compose.ConfirmDialog
import java.util.UUID

private val FavoriteFolderIdListSaver = listSaver<List<String>, String>(
    save = { ids -> ids.toList() },
    restore = { ids -> ids.toList() }
)

/** Saveable target for the add-to-folder dialog. Origin fields are used only by the TV focus path. */
data class FavoriteFolderTarget(
    val guid: String,
    val name: String,
    val originGuid: String? = null,
    val originAction: String? = null,
) {
    companion object {
        val Saver = listSaver<FavoriteFolderTarget?, String>(
            save = { target ->
                target?.let { listOf(it.guid, it.name, it.originGuid.orEmpty(), it.originAction.orEmpty()) }.orEmpty()
            },
            restore = { values ->
                values.takeIf { it.size >= 2 }?.let {
                    FavoriteFolderTarget(
                        guid = it[0],
                        name = it[1],
                        originGuid = it.getOrNull(2)?.takeIf(String::isNotEmpty),
                        originAction = it.getOrNull(3)?.takeIf(String::isNotEmpty),
                    )
                }
            }
        )
    }
}

internal fun FavoriteFolderTarget.tvOrigin(): TvMainFocusTarget.Profile? = originGuid?.let { guid ->
    val action = originAction?.let { name -> runCatching { ProfileRowAction.valueOf(name) }.getOrNull() }
    TvMainFocusTarget.Profile(guid, action)
}

@Composable
fun FavoriteFolderManagerDialog(
    folders: List<FavoriteFolder>,
    onAction: (MainAction) -> Unit,
    onDismiss: () -> Unit,
) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var renameTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    val renameTarget = folders.firstOrNull { it.id == renameTargetId }
    val deleteTarget = folders.firstOrNull { it.id == deleteTargetId }

    if (showCreate) {
        FavoriteFolderInputDialog(
            title = stringResource(R.string.favorite_folder_create),
            initialValue = "",
            onConfirm = { name ->
                onAction(MainAction.CreateFavoriteFolder(UUID.randomUUID().toString(), name))
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }
    if (renameTarget != null) {
        FavoriteFolderInputDialog(
            title = stringResource(R.string.favorite_folder_rename),
            initialValue = renameTarget.name,
            onConfirm = { name ->
                onAction(MainAction.RenameFavoriteFolder(renameTarget.id, name))
                renameTargetId = null
            },
            onDismiss = { renameTargetId = null }
        )
    }
    if (deleteTarget != null) {
        ConfirmDialog(
            message = stringResource(R.string.favorite_folder_delete_confirm, deleteTarget.name),
            confirmText = stringResource(R.string.action_delete),
            onConfirm = {
                onAction(MainAction.DeleteFavoriteFolder(deleteTarget.id))
                deleteTargetId = null
            },
            onDismiss = { deleteTargetId = null }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorite_folders)) },
        text = {
            if (folders.isEmpty()) {
                Text(
                    text = stringResource(R.string.favorite_folder_empty),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(folders, key = { it.id }) { folder ->
                        FavoriteFolderManagerRow(
                            folder = folder,
                            onRename = { renameTargetId = folder.id },
                            onDelete = { deleteTargetId = folder.id }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showCreate = true }) {
                Text(stringResource(R.string.favorite_folder_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun FavoriteFolderManagerRow(
    folder: FavoriteFolder,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = folder.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onRename) {
            Icon(
                painter = painterResource(R.drawable.ic_edit_24dp),
                contentDescription = stringResource(R.string.favorite_folder_rename)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete_24dp),
                contentDescription = stringResource(R.string.favorite_folder_delete)
            )
        }
    }
}

@Composable
fun FavoriteFolderPickerDialog(
    guid: String,
    profileName: String,
    folders: List<FavoriteFolder>,
    onAction: (MainAction) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedFolderIds by rememberSaveable(guid, stateSaver = FavoriteFolderIdListSaver) {
        mutableStateOf(folderIdsContaining(folders, guid).toList())
    }
    var showCreate by rememberSaveable(guid) { mutableStateOf(false) }
    val selected = selectedFolderIds.toSet()

    if (showCreate) {
        FavoriteFolderInputDialog(
            title = stringResource(R.string.favorite_folder_create),
            initialValue = "",
            onConfirm = { name ->
                val folderId = UUID.randomUUID().toString()
                onAction(MainAction.CreateFavoriteFolder(folderId, name))
                selectedFolderIds = selectedFolderIds + folderId
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorite_add_to_folder)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (profileName.isNotBlank()) {
                    Text(
                        text = profileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (folders.isEmpty()) {
                    Text(
                        text = stringResource(R.string.favorite_folder_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(folders, key = { it.id }) { folder ->
                            val checked = folder.id in selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = checked,
                                        role = Role.Checkbox,
                                        onClick = {
                                            selectedFolderIds = if (checked) {
                                                selectedFolderIds - folder.id
                                            } else {
                                                selectedFolderIds + folder.id
                                            }
                                        }
                                    )
                                    .semantics(mergeDescendants = true) {}
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                    modifier = Modifier.clearAndSetSemantics {}
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { showCreate = true }) {
                    Text(stringResource(R.string.favorite_folder_create))
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = {
                    onAction(MainAction.SetServerFavoriteFolders(guid, selectedFolderIds.toSet()))
                    onDismiss()
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * Dialogs are outside Scaffold: consume the navigation-bar inset once and apply imePadding so
 * the folder-name field and primary action remain reachable while the keyboard is visible.
 */
@Composable
private fun FavoriteFolderInputDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(title, initialValue) { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(title, initialValue) {
        focusRequester.requestFocus()
    }
    fun confirm() {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) onConfirm(trimmed)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .consumeWindowInsets(WindowInsets.navigationBars)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.favorite_folder_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { confirm() }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
