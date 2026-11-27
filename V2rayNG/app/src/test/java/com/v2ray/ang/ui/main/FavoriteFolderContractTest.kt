package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.FavoriteFolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FavoriteFolderContractTest {
    private val folders = listOf(
        FavoriteFolder("one", "One", listOf("a")),
        FavoriteFolder("two", "Two", listOf("b")),
    )

    @Test
    fun `virtual group IDs never become subscription or profile storage IDs`() {
        val groupId = favoriteGroupId("folder-id")
        assertEquals("favorite-folder:folder-id", groupId)
        assertTrue(isFavoriteGroupId(groupId))
        assertEquals("folder-id", favoriteFolderId(groupId))
        assertNull(favoriteFolderId("subscription-id"))
        assertNull(favoriteFolderId(FAVORITE_GROUP_PREFIX))
        assertEquals("", storageGroupId(groupId))
        assertEquals("", storageGroupId(""))
        assertEquals("subscription-id", storageGroupId("subscription-id"))
    }

    @Test
    fun `empty initial state and invalid create inputs remain unchanged`() {
        val empty = emptyList<FavoriteFolder>()
        assertSame(empty, applyFavoriteFolderAction(empty, MainAction.CreateFavoriteFolder("", "Blank ID")))
        assertSame(empty, applyFavoriteFolderAction(empty, MainAction.CreateFavoriteFolder("id", "  ")))
        assertEquals(
            listOf(FavoriteFolder("id", "Work")),
            applyFavoriteFolderAction(empty, MainAction.CreateFavoriteFolder("id", " Work "))
        )
        assertFalse(shouldPersistFavoriteFolders(empty, empty))
    }

    @Test
    fun `each folder action maps to its durable list by domain ID`() {
        val created = applyFavoriteFolderAction(folders, MainAction.CreateFavoriteFolder("three", " Three "))
        assertEquals(folders + FavoriteFolder("three", "Three"), created)

        val renamed = applyFavoriteFolderAction(folders, MainAction.RenameFavoriteFolder("one", " Renamed "))
        assertEquals(FavoriteFolder("one", "Renamed", listOf("a")), renamed[0])
        assertEquals(folders, applyFavoriteFolderAction(folders, MainAction.RenameFavoriteFolder("missing", "Other")))
        assertSame(folders, applyFavoriteFolderAction(folders, MainAction.RenameFavoriteFolder("one", " ")))

        val deleted = applyFavoriteFolderAction(folders, MainAction.DeleteFavoriteFolder("one"))
        assertEquals(listOf(FavoriteFolder("two", "Two", listOf("b"))), deleted)
        assertEquals(folders, applyFavoriteFolderAction(folders, MainAction.DeleteFavoriteFolder("missing")))

        val assigned = applyFavoriteFolderAction(
            folders,
            MainAction.SetServerFavoriteFolders("a", setOf("two", "missing"))
        )
        assertEquals(emptyList<String>(), assigned[0].serverGuids)
        assertEquals(listOf("b", "a"), assigned[1].serverGuids)
        assertSame(folders, applyFavoriteFolderAction(folders, MainAction.SetServerFavoriteFolders("", setOf("two"))))

        val cleared = applyFavoriteFolderAction(assigned, MainAction.SetServerFavoriteFolders("a", emptySet()))
        assertEquals(emptyList<String>(), cleared[0].serverGuids)
        assertEquals(listOf("b"), cleared[1].serverGuids)
    }

    @Test
    fun `non-folder actions and no-op mutations do not request a write`() {
        assertSame(folders, applyFavoriteFolderAction(folders, MainAction.Exit))
        assertFalse(shouldPersistFavoriteFolders(folders, folders))
        assertTrue(shouldPersistFavoriteFolders(folders, folders + FavoriteFolder("new", "New")))
    }

    @Test
    fun `failed storage write keeps the previously loaded folder list`() {
        val next = folders + FavoriteFolder("three", "Three")
        assertEquals(next, favoriteFoldersAfterSave(folders, next, saved = true))
        assertEquals(folders, favoriteFoldersAfterSave(folders, next, saved = false))
    }
}
