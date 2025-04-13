package de.menkalian.worklock.database

import android.content.Context
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.menkalian.worklock.BuildConfig
import de.menkalian.worklock.controller.PersonalDataEntry
import de.menkalian.worklock.controller.Record
import de.menkalian.worklock.database.dao.*
import de.menkalian.worklock.database.dao.PersonalData.convertToPersonalDataEntry
import de.menkalian.worklock.database.dao.RecordData.convertToRecordObj
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInSubQuery
import org.jetbrains.exposed.sql.javatime.*
import org.jetbrains.exposed.sql.javatime.Month
import org.jetbrains.exposed.sql.javatime.Year
import org.jetbrains.exposed.sql.transactions.transaction
import org.sqlite.SQLiteConfig
import java.time.*
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
class DataAccessImpl(dbPath: String) : DataAccess {
    @Inject
    constructor(@ApplicationContext context: Context)
            : this(context.getDatabasePath("Worklock_common.DB3").absolutePath)

    private val dbConnection: Database

    init {
        dbConnection = Database.connect(
            "jdbc:sqlite:$dbPath",
            setupConnection = {
                SQLiteConfig().apply {
                    setJournalMode(SQLiteConfig.JournalMode.TRUNCATE)
                    setSynchronous(SQLiteConfig.SynchronousMode.FULL)
                    apply(it)
                }
            }
        )
        transaction(dbConnection) {
            SchemaUtils.create(
                AdditionalData,
                ArchiveData,
                MetaData,
                PersonalData,
                RecordData,
                VariableData
            )

            val existingRecordVersion = MetaData
                .select(MetaData.schemaVersion)
                .where { MetaData.schemaVersion eq Constants.SCHEMA_VERSION }
                .firstOrNull()
                ?.get(MetaData.schemaVersion)

            if (existingRecordVersion != null) {
                MetaData.update(
                    where = { MetaData.schemaVersion eq Constants.SCHEMA_VERSION }
                ) {
                    it[softwareVersion] = BuildConfig.VERSION_NAME
                    it[lastOpenTimestamp] = Instant.now()
                }
            } else {
                MetaData.insert {
                    it[schemaVersion] = Constants.SCHEMA_VERSION
                    it[schemaTimestamp] = Instant.parse(Constants.SCHEMA_TIMESTAMP)
                    it[softwareVersion] = BuildConfig.VERSION_NAME
                    it[lastOpenTimestamp] = Instant.now()
                }
            }
        }

        cleanupDatabase()
    }

    override fun addStartRecord() = addRecord(RecordType.Start)

    override fun addEndRecord() = addRecord(RecordType.End)

    override fun addPauseRecord() = addRecord(RecordType.Pause)

    override fun addUnpauseRecord() = addRecord(RecordType.Unpause)

    override fun addWorkPeriod(from: Instant, until: Instant) {
        // We do not validate in the data layer. The logic is obligated to do this.
        addRecord(RecordType.Start, recordedTime = from)
        addRecord(RecordType.End, recordedTime = until)
    }

    override fun addPause(from: Instant, until: Instant) {
        addRecord(RecordType.Pause, recordedTime = from)
        addRecord(RecordType.Unpause, recordedTime = until)
    }

    override fun setTimeCorrection(originalRecordId: Long, correctedTime: Instant): Boolean {
        transaction(dbConnection) {
            val currentTimestamp = Instant.now()
            RecordData.insert {
                it[creationTime] = currentTimestamp
                it[changeTime] = currentTimestamp
                it[recordTime] = correctedTime
                it[type] = select(type).where { id eq originalRecordId }.first()[type]
                it[correctionFor] = originalRecordId
                it[additionalData] = AdditionalData.insertAndGetId { }
            }
        }
        return true
    }

    override fun deleteRecord(id: Long): Boolean {
        val updateCount = transaction(dbConnection) {
            val currentTimestamp = Instant.now()
            RecordData.update(
                where = { RecordData.id eq id },
            ) {
                it[changeTime] = currentTimestamp
                it[deleted] = true
            }
        }
        return updateCount != 0
    }

    override fun getPersonalData(): List<PersonalDataEntry> {
        return transaction(dbConnection) {
            PersonalData.selectAll()
                .map { it.convertToPersonalDataEntry() }
        }
    }

    override fun updatePersonalData(data: List<PersonalDataEntry>) {
        transaction(dbConnection) {
            for (personalDataEntry in data) {
                val exists = PersonalData
                    .update(
                        where = { PersonalData.name eq personalDataEntry.name }
                    ) {
                        it[value] = personalDataEntry.value
                        it[enabled] = personalDataEntry.enabled
                    } > 0
                if (exists.not()) {
                    PersonalData.insert {
                        it[name] = personalDataEntry.name
                        it[value] = personalDataEntry.value
                        it[enabled] = personalDataEntry.enabled
                    }
                }
            }
            val names = data.map { it.name }.toSet()
            PersonalData.deleteWhere {
                name notInList names
            }
        }
    }

    override fun getPreviousRecordFor(record: Record): Record? {
        return transaction(dbConnection) {
            return@transaction RecordData
                .selectAll()
                .where(
                    RecordData.recordTime less record.timestamp
                            and not(RecordData.deleted)
                            and (not(RecordData.id inSubQuery (RecordData.select(RecordData.correctionFor).where { RecordData.correctionFor.isNotNull() })))
                )
                .orderBy(RecordData.recordTime, SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.convertToRecordObj()
        }
    }

    override fun getNextRecordFor(record: Record): Record? {
        return transaction(dbConnection) {
            return@transaction RecordData
                .selectAll()
                .where(
                    RecordData.recordTime greater record.timestamp
                            and not(RecordData.deleted)
                            and (not(RecordData.id inSubQuery (RecordData.select(RecordData.correctionFor).where { RecordData.correctionFor.isNotNull() })))
                )
                .orderBy(RecordData.recordTime, SortOrder.ASC)
                .limit(1)
                .firstOrNull()
                ?.convertToRecordObj()
        }
    }

    override fun getStartRecordFor(record: Record): Record {
        return transaction(dbConnection) {
            return@transaction RecordData
                .selectAll()
                .where(
                    (RecordData.recordTime less record.timestamp) and (RecordData.type eq RecordType.Start)
                            and not(RecordData.deleted)
                            and (not(RecordData.id inSubQuery (RecordData.select(RecordData.correctionFor).where { RecordData.correctionFor.isNotNull() })))
                )
                .orderBy(RecordData.recordTime, SortOrder.DESC)
                .limit(1)
                .first()
                .convertToRecordObj()
        }
    }

    override fun getEndRecordFor(record: Record): Record {
        return transaction(dbConnection) {
            return@transaction RecordData
                .selectAll()
                .where(
                    (RecordData.recordTime greater record.timestamp) and (RecordData.type eq RecordType.End)
                            and not(RecordData.deleted)
                            and (not(RecordData.id inSubQuery (RecordData.select(RecordData.correctionFor).where { RecordData.correctionFor.isNotNull() })))
                )
                .orderBy(RecordData.recordTime, SortOrder.ASC)
                .limit(1)
                .first()
                .convertToRecordObj()
        }
    }

    override fun getLastStartOrEndRecord(): Record? {
        return transaction(dbConnection) {
            return@transaction RecordData
                .selectAll()
                .where(
                    (RecordData.type inList listOf(RecordType.Start, RecordType.End))
                            and not(RecordData.deleted)
                            and (not(RecordData.id inSubQuery (RecordData.select(RecordData.correctionFor).where { RecordData.correctionFor.isNotNull() })))
                )
                .orderBy(RecordData.recordTime, SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.convertToRecordObj()
        }
    }

    override fun getLastPauseOrUnpauseRecord(): Record? {
        return transaction(dbConnection) {
            return@transaction RecordData
                .selectAll()
                .where(
                    (RecordData.type inList listOf(RecordType.Pause, RecordType.Unpause))
                            and not(RecordData.deleted)
                            and (not(RecordData.id inSubQuery (RecordData.select(RecordData.correctionFor).where { RecordData.correctionFor.isNotNull() })))
                )
                .orderBy(RecordData.recordTime, SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.convertToRecordObj()
        }
    }

    override fun getRecordsForMonth(month: YearMonth, includeCorrected: Boolean, includeDeleted: Boolean): List<Record> {
        return transaction(dbConnection) {
            var condition = (Month(RecordData.recordTime) eq month.monthValue) and (Year(RecordData.recordTime) eq month.year)
            if (!includeDeleted) {
                condition = condition and (not(RecordData.deleted))
            }
            if (!includeCorrected) {
                condition =
                    condition and (not(RecordData.id inSubQuery (RecordData.select(RecordData.correctionFor).where { RecordData.correctionFor.isNotNull() })))
            }
            return@transaction RecordData
                .selectAll()
                .where(condition)
                .orderBy(RecordData.recordTime, SortOrder.ASC)
                .map { it.convertToRecordObj() }
        }
    }

    override fun getRecordsForDate(date: LocalDate, includeCorrected: Boolean, includeDeleted: Boolean): List<Record> {
        return transaction(dbConnection) {
            var condition = Date(RecordData.recordTime) eq date
            if (!includeDeleted) {
                condition = condition and (not(RecordData.deleted))
            }
            if (!includeCorrected) {
                condition =
                    condition and (not(RecordData.id inSubQuery (RecordData.select(RecordData.correctionFor).where { RecordData.correctionFor.isNotNull() })))
            }
            return@transaction RecordData
                .selectAll()
                .where(condition)
                .orderBy(RecordData.recordTime, SortOrder.ASC)
                .map { it.convertToRecordObj() }
        }
    }

    override fun cleanupDatabase(): Boolean {
        // TODO: Implement archives

        transaction(dbConnection) {
            val deletionCutoff = LocalDate.now().minusYears(5)
            RecordData.deleteWhere {
                //@formatter:off
                (
                    Date(recordTime) less deletionCutoff
                ) and (
                    RecordData.id notInSubQuery (
                        RecordData.select(id).orderBy(id, SortOrder.DESC).limit(10_000)
                    )
                )
                //@formatter:on
            }
            AdditionalData
                .deleteWhere {
                    id notInSubQuery (RecordData.select(RecordData.additionalData).withDistinct(true))
                }
            VariableData
                .deleteWhere {
                    map notInSubQuery (AdditionalData.select(id))
                }
        }

        return true
    }

    private fun addRecord(
        recordType: RecordType,
        recordedTime: Instant? = null,
    ): Record {
        return transaction(dbConnection) {
            val currentTimestamp = Instant.now()
            val insertId = RecordData.insertAndGetId {
                it[creationTime] = currentTimestamp
                it[changeTime] = currentTimestamp
                it[recordTime] = recordedTime ?: currentTimestamp
                it[type] = recordType
                it[additionalData] = AdditionalData.insertAndGetId { }
            }

            return@transaction RecordData
                .selectAll()
                .where(RecordData.id eq insertId)
                .first()
                .convertToRecordObj()
        }
    }
}