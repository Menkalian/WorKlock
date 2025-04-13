package de.menkalian.worklock.controller

import androidx.compose.runtime.*
import java.time.Instant
import java.time.LocalDate

class DummyLogicController : LogicController {
    override val allowUndo: State<Boolean> = mutableStateOf(false)
    override val isStarted: State<Boolean> = mutableStateOf(false)
    override val isPaused: State<Boolean> = mutableStateOf(false)
    override val currentTimeForDayMinutes: State<Int> = mutableIntStateOf(63)
    override val currentDayHasError: State<Boolean> = mutableStateOf(false)

    override suspend fun start() {}

    override suspend fun end() {    }

    override suspend fun togglePause() {}

    override suspend fun undoLastAction() {}

    override suspend fun getAllRecordsForDay(date: LocalDate, detailed: Boolean): List<Record> {
        if (date.dayOfMonth < 10 || date == LocalDate.now()) {
            return emptyList()
        }
        if (date.dayOfMonth % 5 == 3) {
            return listOf(
                // Error
                Record(0, Instant.now().minusSeconds(60 * 85), RecordType.Start, null, false, false),
                Record(1, Instant.now().minusSeconds(60 * 80), RecordType.Start, null, false, false),
                Record(6, Instant.now().plusSeconds(60 * 180), RecordType.End, null, false, false),
            )
        }
        return listOf(
            Record(1, Instant.now().minusSeconds(60 * 80), RecordType.Start, null, false, false),
            Record(21, Instant.now().minusSeconds(60 * 70), RecordType.End, null, false, true),
            Record(2, Instant.now().minusSeconds(60 * 50), RecordType.Pause, null, false, false),
            Record(90, Instant.now().plusSeconds(60 * 60), RecordType.End, null, false, false),
            Record(3, Instant.now().minusSeconds(60 * 40), RecordType.Unpause, null, false, false),
            Record(4, Instant.now().plusSeconds(60 * 40), RecordType.End, 90, false, false),
            Record(5, Instant.now().plusSeconds(60 * 80), RecordType.Start, null, true, false),
            Record(6, Instant.now().plusSeconds(60 * 180), RecordType.End, null, true, false),
        )
    }

    override suspend fun getAllowedEditTimeRangeFor(record: Record): Pair<Instant, Instant> {
        return Instant.now().minusSeconds(600) to Instant.now().plusSeconds(600)
    }

    override suspend fun getAllowedManualWorkPeriodEndTimestamp(start: Instant): Instant? {
        return null
    }

    override suspend fun getAllowedManualPauseEndTimestamp(start: Instant): Instant? {
        return null
    }

    override suspend fun addManualWorkPeriod(from: Instant, until: Instant) {
    }

    override suspend fun addManualPause(from: Instant, until: Instant) {
    }

    override suspend fun updateRecordTime(originalId: Long, recordTime: Instant) {
    }

    override suspend fun deleteRecord(id: Long) {
    }
}