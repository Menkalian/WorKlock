package de.menkalian.worklock.controller

import WorKlock.Settings
import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.menkalian.worklock.database.DataAccess
import de.menkalian.worklock.database.settingsStore
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SettingsControllerImpl
@Inject constructor(
    private val application: Application,
    private val dataAccess: DataAccess,
) : AndroidViewModel(application), SettingsController {
    private val exportIncludeManualKey = booleanPreferencesKey(Settings.Export.IncludeManualMarker)
    private val exportIncludeErrorsKey = booleanPreferencesKey(Settings.Export.IncludeErrorMarker)
    private val exportIncludePauseKey = booleanPreferencesKey(Settings.Export.IncludePause)
    private val allowPotentiallyDangerousEditsKey = booleanPreferencesKey(Settings.App.AllowDangerousEdits)

    override suspend fun isExportIncludeManualFlag(): Boolean {
        return application.settingsStore.data.map {
            it[exportIncludeManualKey] ?: false
        }.firstOrNull() ?: false
    }

    override suspend fun setExportIncludeManualFlag(value: Boolean) {
        application.settingsStore.edit { settings ->
            settings[exportIncludeManualKey] = value
        }
    }

    override suspend fun isExportIncludeErrorsFlag(): Boolean {
        return application.settingsStore.data.map {
            it[exportIncludeErrorsKey] ?: false
        }.firstOrNull() ?: false
    }

    override suspend fun setExportIncludeErrorsFlag(value: Boolean) {
        application.settingsStore.edit { settings ->
            settings[exportIncludeErrorsKey] = value
        }
    }

    override suspend fun isExportIncludePause(): Boolean {
        return application.settingsStore.data.map {
            it[exportIncludePauseKey] ?: false
        }.firstOrNull() ?: false
    }

    override suspend fun setExportIncludePause(value: Boolean) {
        application.settingsStore.edit { settings ->
            settings[exportIncludePauseKey] = value
        }
    }

    override suspend fun isAllowPotentiallyDangerousEdits(): Boolean {
        return application.settingsStore.data.map {
            it[allowPotentiallyDangerousEditsKey] ?: false
        }.firstOrNull() ?: false
    }

    override suspend fun setAllowPotentiallyDangerousEdits(value: Boolean) {
        application.settingsStore.edit { settings ->
            settings[allowPotentiallyDangerousEditsKey] = value
        }
    }

    override suspend fun getAllPersonalData(): List<PersonalDataEntry> {
        return dataAccess.getPersonalData()
    }

    override suspend fun setAllPersonalData(data: List<PersonalDataEntry>) {
        dataAccess.updatePersonalData(data)
    }
}