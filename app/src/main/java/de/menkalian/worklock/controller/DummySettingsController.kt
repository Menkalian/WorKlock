package de.menkalian.worklock.controller

class DummySettingsController : SettingsController {
    private var exportIncludeManualFlag = false
    private var exportIncludeErrorsFlag = false
    private var exportIncludePause = false
    private var allowPotentiallyDangerousEdits = false
    private val personalData = mutableListOf(
        PersonalDataEntry(1, true, "ID-Number", "123456"),
        PersonalDataEntry(2, false, "Company", "Example Company Inc."),
    )

    override suspend fun isExportIncludeManualFlag(): Boolean {
        return exportIncludeManualFlag
    }

    override suspend fun setExportIncludeManualFlag(value: Boolean) {
        exportIncludeManualFlag = value
    }

    override suspend fun isExportIncludeErrorsFlag(): Boolean {
        return exportIncludeErrorsFlag
    }

    override suspend fun setExportIncludeErrorsFlag(value: Boolean) {
        exportIncludeErrorsFlag = value
    }

    override suspend fun isExportIncludePause(): Boolean {
        return exportIncludePause
    }

    override suspend fun setExportIncludePause(value: Boolean) {
        exportIncludePause = value
    }

    override suspend fun isAllowPotentiallyDangerousEdits(): Boolean {
        return allowPotentiallyDangerousEdits
    }

    override suspend fun setAllowPotentiallyDangerousEdits(value: Boolean) {
        allowPotentiallyDangerousEdits = value
    }

    override suspend fun getAllPersonalData(): List<PersonalDataEntry> {
        return personalData.toList()
    }

    override suspend fun setAllPersonalData(data: List<PersonalDataEntry>) {
        personalData.clear()
        personalData.addAll(data)
    }
}