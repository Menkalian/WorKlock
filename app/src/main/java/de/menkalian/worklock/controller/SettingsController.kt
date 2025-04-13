package de.menkalian.worklock.controller

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

interface SettingsController {
    suspend fun isExportIncludeManualFlag(): Boolean
    suspend fun setExportIncludeManualFlag(value: Boolean)

    suspend fun isExportIncludeErrorsFlag(): Boolean
    suspend fun setExportIncludeErrorsFlag(value: Boolean)

    suspend fun isExportIncludePause(): Boolean
    suspend fun setExportIncludePause(value: Boolean)

    suspend fun isAllowPotentiallyDangerousEdits(): Boolean
    suspend fun setAllowPotentiallyDangerousEdits(value: Boolean)

    suspend fun getAllPersonalData() : List<PersonalDataEntry>
    suspend fun setAllPersonalData(data: List<PersonalDataEntry>)

    companion object {
        @Composable
        fun get(): SettingsController = hiltViewModel<SettingsControllerImpl>()
    }
}
