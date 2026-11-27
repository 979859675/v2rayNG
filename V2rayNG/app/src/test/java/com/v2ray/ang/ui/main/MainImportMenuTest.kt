package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainImportMenuTest {

    @Test
    fun tvMoreMenuLeavesRestartToTheToolbarAndOmitsDeleteAll() {
        assertEquals(listOf(MainMoreMenuAction.RestartService, MainMoreMenuAction.DeleteAll),
            MainMoreMenuAction.entries - tvMoreMenuActions.toSet())
    }

    @Test
    fun regularShareMenuIncludesShareAndFavoriteActions() {
        val expected = listOf(
            ServerMenuAction.ShareQRCode,
            ServerMenuAction.ShareClipboard,
            ServerMenuAction.ShareFullContent,
            ServerMenuAction.Favorite,
        )
        assertEquals(expected, serverMenuActions(isComplexProfile = false, includeManagementActions = false))
    }

    @Test
    fun regularMoreMenuContainsEveryActionInDisplayOrder() {
        assertEquals(
            ServerMenuAction.entries,
            serverMenuActions(isComplexProfile = false, includeManagementActions = true),
        )
    }

    @Test
    fun complexShareMenuKeepsFullContentAndFavorite() {
        assertEquals(
            listOf(ServerMenuAction.ShareFullContent, ServerMenuAction.Favorite),
            serverMenuActions(isComplexProfile = true, includeManagementActions = false),
        )
    }

    @Test
    fun complexMoreMenuRetainsManagementActions() {
        val expected = listOf(
            ServerMenuAction.ShareFullContent,
            ServerMenuAction.Favorite,
            ServerMenuAction.Edit,
            ServerMenuAction.Delete,
        )
        assertEquals(expected, serverMenuActions(isComplexProfile = true, includeManagementActions = true))
    }
}
