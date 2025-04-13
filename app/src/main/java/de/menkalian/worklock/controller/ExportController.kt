package de.menkalian.worklock.controller

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import java.time.YearMonth

interface ExportController {
    suspend fun createExport(month: YearMonth, type: ExportType): File
    suspend fun createRangeExport(start: YearMonth, until: YearMonth, type: ExportType): File

    fun getMimeType(exportType: ExportType): String

    companion object {
        @Composable
        fun get(): ExportController = hiltViewModel<ExportControllerImpl>()
    }
}