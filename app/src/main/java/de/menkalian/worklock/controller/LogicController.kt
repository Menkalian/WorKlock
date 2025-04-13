package de.menkalian.worklock.controller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.LocalDate

interface LogicController {
    val allowUndo: State<Boolean>
    val isStarted: State<Boolean>
    val isPaused: State<Boolean>
    val currentTimeForDayMinutes: State<Int>
    val currentDayHasError: State<Boolean>

    suspend fun start()
    suspend fun end()
    suspend fun togglePause()

    suspend fun undoLastAction()

    suspend fun getAllRecordsForDay(date: LocalDate, detailed: Boolean = false): List<Record>
    suspend fun getAllowedEditTimeRangeFor(record: Record): Pair<Instant, Instant>

    suspend fun getAllowedManualWorkPeriodEndTimestamp(start: Instant): Instant?
    suspend fun getAllowedManualPauseEndTimestamp(start: Instant): Instant?
    suspend fun addManualWorkPeriod(from: Instant, until: Instant)
    suspend fun addManualPause(from: Instant, until: Instant)
    suspend fun updateRecordTime(originalId: Long, recordTime: Instant)
    suspend fun deleteRecord(id: Long)

    companion object {
        @Composable
        fun get(): LogicController = hiltViewModel<LogicControllerImpl>()
    }
}
