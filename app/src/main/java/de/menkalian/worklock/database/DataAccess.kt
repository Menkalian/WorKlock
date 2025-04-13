package de.menkalian.worklock.database

import de.menkalian.worklock.controller.PersonalDataEntry
import de.menkalian.worklock.controller.Record
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

interface DataAccess {
    /**
     * Adds a start record at the current time.
     */
    fun addStartRecord(): Record

    /**
     * Adds an end record at the current time.
     */
    fun addEndRecord(): Record

    /**
     * Adds a pause record at the current time.
     */
    fun addPauseRecord(): Record

    /**
     * Adds an unpause record at the current time
     */
    fun addUnpauseRecord(): Record

    /**
     * Adds a work period in the given range.
     */
    fun addWorkPeriod(from: Instant, until: Instant)

    /**
     * Adds a pause period in the given range.
     */
    fun addPause(from: Instant, until: Instant)

    /**
     * Corrects the record time for the given record (this actually creates a new record).
     */
    fun setTimeCorrection(originalRecordId: Long, correctedTime: Instant): Boolean

    /**
     * Marks the given record as deleted in the database.
     */
    fun deleteRecord(id: Long): Boolean

    /**
     * Reads the entered personal data from the database.
     */
    fun getPersonalData(): List<PersonalDataEntry>

    /**
     * Updates the personal data stored in the database.
     */
    fun updatePersonalData(data: List<PersonalDataEntry>)

    /**
     * Reads the record which was last recorded before the given record.
     */
    fun getPreviousRecordFor(record: Record): Record?

    /**
     * Reads the record which was first recorded after the given record.
     */
    fun getNextRecordFor(record: Record): Record?

    /**
     * Reads the Record which was last recorded before the given record, that has the type "Start"
     */
    fun getStartRecordFor(record: Record): Record

    /**
     * Reads the Record which was last recorded before the given record, that has the type "End"
     */
    fun getEndRecordFor(record: Record): Record

    /**
     * Reads the last record from the database with type "Start" or "End"
     */
    fun getLastStartOrEndRecord(): Record?

    /**
     * Reads the last record from the database with type "Pause" or "Unpause"
     */
    fun getLastPauseOrUnpauseRecord(): Record?

    /**
     * Reads all records for the given month.
     *
     * @param month The month to read from the database
     * @param includeCorrected Whether to include the original record for a correction
     * @param includeDeleted Whether to include deleted records.
     */
    fun getRecordsForMonth(month: YearMonth, includeCorrected: Boolean = true, includeDeleted: Boolean = false): List<Record>

    /**
     * Reads all records for the given date.
     *
     * @param date The date to read from the database
     * @param includeCorrected Whether to include the original record for a correction
     * @param includeDeleted Whether to include deleted records.
     */
    fun getRecordsForDate(date: LocalDate, includeCorrected: Boolean = true, includeDeleted: Boolean = false): List<Record>

    /**
     * Cleans up the database by deleting entries that are older than 5 years while keeping the last 10_000 records.
     * Also deletes all AdditionalData and VariableData which are not attached to a Record.
     * Additionally, the previous years are compressed and archived for 15 years.
     */
    fun cleanupDatabase(): Boolean
}