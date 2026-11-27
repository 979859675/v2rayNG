package com.v2ray.ang.dto.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FavoriteFolderTest {

    @Test
    fun `blank and malformed persisted payloads start with an empty folder list`() {
        assertTrue(decodeFavoriteFolders(null).isEmpty())
        assertTrue(decodeFavoriteFolders("").isEmpty())
        assertTrue(decodeFavoriteFolders("   ").isEmpty())
        assertTrue(decodeFavoriteFolders("not json").isEmpty())
        assertTrue(decodeFavoriteFolders("{}").isEmpty())
    }

    @Test
    fun `legacy records without membership or name decode with safe defaults`() {
        val json = """
            [
              {"id":"work","name":" Work ","serverGuids":["a","b","a"]},
              {"id":"empty"},
              {"name":"No ID"},
              {"id":"bad","serverGuids":"not an array"}
            ]
        """.trimIndent()
        val folders = decodeFavoriteFolders(json)
        assertEquals(3, folders.size)
        assertEquals(FavoriteFolder("work", "Work", listOf("a", "b")), folders[0])
        assertEquals(FavoriteFolder("empty", "", emptyList()), folders[1])
        assertEquals(FavoriteFolder("bad", "", emptyList()), folders[2])
    }

    @Test
    fun `create rejects blank names duplicate IDs and trims the user name`() {
        val folders = listOf(FavoriteFolder("one", "One"))
        assertEquals(folders, createFavoriteFolder(folders, "", "Blank ID"))
        assertEquals(folders, createFavoriteFolder(folders, "new", "   "))
        assertEquals(folders, createFavoriteFolder(folders, "one", "Duplicate"))
        assertEquals(
            folders + FavoriteFolder("new", "Travel"),
            createFavoriteFolder(folders, "new", "  Travel  ")
        )
    }

    @Test
    fun `rename and delete resolve by folder ID`() {
        val folders = listOf(FavoriteFolder("one", "One"), FavoriteFolder("two", "Two"))
        assertEquals(folders, renameFavoriteFolder(folders, "one", "  "))
        assertEquals(folders, renameFavoriteFolder(folders, "missing", "Other"))
        assertEquals(
            listOf(FavoriteFolder("one", "Renamed"), FavoriteFolder("two", "Two")),
            renameFavoriteFolder(folders, "one", " Renamed ")
        )
        assertEquals(listOf(FavoriteFolder("two", "Two")), deleteFavoriteFolder(folders, "one"))
        assertEquals(folders, deleteFavoriteFolder(folders, "missing"))
    }

    @Test
    fun `membership replacement uses GUIDs and exact folder IDs`() {
        val folders = listOf(
            FavoriteFolder("one", "One", listOf("a", "c")),
            FavoriteFolder("two", "Two", listOf("b")),
        )
        val moved = setServerFavoriteFolders(folders, "a", setOf("two", "missing"))
        assertEquals(listOf("c"), moved[0].serverGuids)
        assertEquals(listOf("b", "a"), moved[1].serverGuids)
        assertEquals(setOf("two"), folderIdsContaining(moved, "a"))
        assertEquals(folders, setServerFavoriteFolders(folders, "", setOf("two")))

        val cleared = setServerFavoriteFolders(moved, "a", emptySet())
        assertEquals(listOf("c"), cleared[0].serverGuids)
        assertEquals(listOf("b"), cleared[1].serverGuids)
        assertTrue(folderIdsContaining(cleared, "a").isEmpty())
    }

    @Test
    fun `clearing membership and pruning drop only GUIDs no longer in folders`() {
        val folders = listOf(
            FavoriteFolder("one", "One", listOf("a", "b")),
            FavoriteFolder("two", "Two", listOf("a")),
        )
        val cleared = setServerFavoriteFolders(folders, "a", setOf("two"))
        assertEquals(listOf("b"), cleared[0].serverGuids)
        assertEquals(listOf("a"), cleared[1].serverGuids)
        assertEquals(
            listOf(FavoriteFolder("one", "One", listOf("b")), FavoriteFolder("two", "Two", emptyList())),
            pruneFavoriteFolders(folders, setOf("b"))
        )
    }
}
