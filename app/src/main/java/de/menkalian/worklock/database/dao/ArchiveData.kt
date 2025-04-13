package de.menkalian.worklock.database.dao

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object ArchiveData : LongIdTable() {
    val year = integer("YEAR")
    val updateRequired = bool("UPDATE_REQUIRED")
    val deletedTimestamp = timestamp("DELETED").nullable()
}