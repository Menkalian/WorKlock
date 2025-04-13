package de.menkalian.worklock.database.dao

import de.menkalian.worklock.controller.Record
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.javatime.timestamp

object RecordData : LongIdTable() {
    val creationTime = timestamp("CREATED")
    val changeTime = timestamp("CHANGED")
    val recordTime = timestamp("RECORDED").index("idx_record_recordtime")
    val type = enumeration<RecordType>("TYPE").index("idx_record_type")
    val deleted = bool("DELETED").default(false).index("idx_record_deleted")

    val correctionFor = reference("CORRECTION_FOR", RecordData.id).nullable().default(null).index("idx_record_correction")
    val additionalData = reference("ADDITIONAL_DATA", AdditionalData.id)

    fun ResultRow.convertToRecordObj(): Record {
        return Record(
            this[id].value,
            this[recordTime],
            this[type].convert(),
            this[correctionFor]?.value,
            this[creationTime] != this[changeTime]
                    || this[creationTime] != this[recordTime]
                    || this[correctionFor] != null,
            this[deleted]
        )
    }
}