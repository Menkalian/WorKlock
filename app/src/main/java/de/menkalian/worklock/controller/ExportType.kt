package de.menkalian.worklock.controller

enum class ExportType(val supportRange: Boolean) {
    CSV_RECORDS(false),
    CSV_AGGREGATE(false),
    EXCEL_RECORDS(true),
    EXCEL_AGGREGATE(true),
}