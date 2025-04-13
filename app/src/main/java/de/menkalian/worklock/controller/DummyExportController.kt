package de.menkalian.worklock.controller

import java.io.File
import java.time.YearMonth

class DummyExportController : ExportController {
    override suspend fun createExport(month: YearMonth, type: ExportType): File {
        throw UnsupportedOperationException()
    }

    override suspend fun createRangeExport(start: YearMonth, until: YearMonth, type: ExportType): File {
        throw UnsupportedOperationException()
    }

    override fun getMimeType(exportType: ExportType): String {
        throw UnsupportedOperationException()
    }
}