package de.menkalian.worklock.database.dao

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object MetaData : Table() {
    val schemaVersion = long("SCHEMA_VERSION")
    val schemaTimestamp = timestamp("SCHEMA_TIMESTAMP")
    val lastOpenTimestamp = timestamp("LAST_OPENED")
    val softwareVersion = text("SOFTWARE_VERSION")
}