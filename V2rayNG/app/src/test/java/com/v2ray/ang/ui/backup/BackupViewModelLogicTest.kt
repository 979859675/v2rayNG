package com.v2ray.ang.ui.backup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BackupViewModelLogicTest {

    @Test
    fun `valid backup count and successful zip produce the export path`() {
        assertEquals(
            Pair(true, "/cache/backup.zip"),
            backupFileResult(backupCount = 5, zipCreated = true, zipPath = "/cache/backup.zip")
        )
    }

    @Test
    fun `empty backup count is a failure with no export path`() {
        assertEquals(
            Pair(false, ""),
            backupFileResult(backupCount = 0, zipCreated = true, zipPath = "/cache/backup.zip")
        )
    }

    @Test
    fun `negative backup count is an invalid result`() {
        assertEquals(
            Pair(false, ""),
            backupFileResult(backupCount = -1, zipCreated = true, zipPath = "/cache/backup.zip")
        )
    }

    @Test
    fun `zip failure is reported without a partial export path`() {
        assertEquals(
            Pair(false, ""),
            backupFileResult(backupCount = 5, zipCreated = false, zipPath = "/cache/backup.zip")
        )
    }
}
