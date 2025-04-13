package de.menkalian.worklock.controller

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.menkalian.worklock.BuildConfig
import de.menkalian.worklock.database.DataAccess
import kotlinx.coroutines.*
import java.time.*
import javax.inject.Inject

@HiltViewModel
class LogicControllerImpl @Inject constructor(application: Application, private val dataAccess: DataAccess) : AndroidViewModel(application), LogicController {
    override val allowUndo = mutableStateOf(false)
    override val isStarted = mutableStateOf(false)
    override val isPaused = mutableStateOf(false)
    override val currentTimeForDayMinutes = mutableIntStateOf(0)
    override val currentDayHasError = mutableStateOf(false)

    private var started by isStarted
    private var paused by isPaused
    private var minutesInCurrentDay by currentTimeForDayMinutes
    private var errorInCurrentDay by currentDayHasError

    private var lastInsert: Record? = null
        set(value) {
            allowUndo.value = value != null
            field = value
        }

    init {
        resetStates()
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO)
        private var updateJob: Job? = null
    }

    override suspend fun start() {
        if (!started) {
            lastInsert = dataAccess.addStartRecord()
            started = true
            paused = false
        }
        recalculateTime()
    }

    override suspend fun end() {
        if (paused) {
            dataAccess.addUnpauseRecord()
            paused = false
        }
        lastInsert = dataAccess.addEndRecord()
        started = false
    }

    override suspend fun togglePause() {
        lastInsert = if (paused) {
            dataAccess.addUnpauseRecord()
        } else {
            dataAccess.addPauseRecord()
        }
        paused = !paused
        recalculateTime()
    }

    override suspend fun undoLastAction() {
        if (lastInsert != null) {
            dataAccess.deleteRecord(lastInsert!!.id)
            lastInsert = null
        }
        resetStates()
    }

    override suspend fun getAllRecordsForDay(date: LocalDate, detailed: Boolean): List<Record> {
        return dataAccess.getRecordsForDate(
            date,
            includeCorrected = detailed,
            includeDeleted = detailed
        )
    }

    override suspend fun getAllowedEditTimeRangeFor(record: Record): Pair<Instant, Instant> {
        val start = dataAccess.getPreviousRecordFor(record)?.timestamp ?: Instant.EPOCH
        val end = dataAccess.getNextRecordFor(record)?.timestamp
            ?: record.timestamp.plusSeconds((LocalTime.MAX.toSecondOfDay() - record.timestamp.getTime().toSecondOfDay()).toLong())
        return start to end
    }

    override suspend fun getAllowedManualWorkPeriodEndTimestamp(start: Instant): Instant? {
        val records = getAllRecordsForDay(start.getDate())
        return records
            .firstOrNull { it.timestamp > start }
            ?.timestamp
            ?: start.plusSeconds((LocalTime.MAX.toSecondOfDay() - start.getTime().toSecondOfDay()).toLong())
    }

    override suspend fun getAllowedManualPauseEndTimestamp(start: Instant): Instant? {
        val records = getAllRecordsForDay(start.getDate())
        return records
            .firstOrNull { it.timestamp > start }
            ?.timestamp
            ?: start.plusSeconds((LocalTime.MAX.toSecondOfDay() - start.getTime().toSecondOfDay()).toLong())
    }

    override suspend fun addManualWorkPeriod(from: Instant, until: Instant) {
        if (from.getDate() != until.getDate() && BuildConfig.DEBUG) {
            throw IllegalArgumentException("Manual work period may only be on one day.")
        }
        val records = getAllRecordsForDay(from.getDate()).toMutableList()
        val (errorBefore, _) = getFullTimeForDayInMinutes(records)
        if (errorBefore && BuildConfig.DEBUG) {
            throw UnsupportedOperationException("May not add manual work period while day has errors.")
        }

        // Add fake records for checking errors.
        records += Record(-1, from, RecordType.Start, null, manual = true, deleted = false)
        records += Record(-1, until, RecordType.End, null, manual = true, deleted = false)

        val (errorAfter, _) = getFullTimeForDayInMinutes(records)
        if (errorAfter && BuildConfig.DEBUG) {
            throw UnsupportedOperationException("Adding this work period would cause an error. This is not permitted.")
        }

        dataAccess.addWorkPeriod(from, until)
        lastInsert = null // Reset undo to prevent funny errors.
    }

    override suspend fun addManualPause(from: Instant, until: Instant) {
        if (from.getDate() != until.getDate() && BuildConfig.DEBUG) {
            throw IllegalArgumentException("Manual pause may only be on one day.")
        }
        val records = getAllRecordsForDay(from.getDate()).toMutableList()
        val (errorBefore, _) = getFullTimeForDayInMinutes(records)
        if (errorBefore && BuildConfig.DEBUG) {
            throw UnsupportedOperationException("May not add manual pause while day has errors.")
        }

        // Add fake records for checking errors.
        records += Record(-1, from, RecordType.Pause, null, manual = true, deleted = false)
        records += Record(-1, until, RecordType.Unpause, null, manual = true, deleted = false)

        val (errorAfter, _) = getFullTimeForDayInMinutes(records)
        if (errorAfter && BuildConfig.DEBUG) {
            throw UnsupportedOperationException("Adding this pause would cause an error. This is not permitted.")
        }

        dataAccess.addWorkPeriod(from, until)
        lastInsert = null // Reset undo to prevent funny errors.
    }

    override suspend fun updateRecordTime(originalId: Long, recordTime: Instant) {
        // TODO: Validate time?
        dataAccess.setTimeCorrection(originalId, recordTime)
        lastInsert = null
    }

    override suspend fun deleteRecord(id: Long) {
        dataAccess.deleteRecord(id)
    }

    private fun resetStates() {
        var started by isStarted
        var paused by isPaused

        started = dataAccess.getLastStartOrEndRecord()?.type == RecordType.Start
        paused = started && dataAccess.getLastPauseOrUnpauseRecord()?.type == RecordType.Pause
        recalculateTime()
    }

    private fun recalculateTime() {
        updateJob?.cancel()
        scope.launch {
            delay(500)
            while (true) {
                val records = dataAccess.getRecordsForDate(LocalDate.now(), includeCorrected = false, includeDeleted = false)
                val (error, minutes) = getFullTimeForDayInMinutes(records, endOfDay = LocalTime.now())
                errorInCurrentDay = error
                minutesInCurrentDay = minutes

                delay(30_000) // Recalculate every 30 seconds in background.
            }
        }
    }
}