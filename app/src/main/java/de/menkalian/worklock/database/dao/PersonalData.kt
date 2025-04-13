package de.menkalian.worklock.database.dao

import de.menkalian.worklock.controller.PersonalDataEntry
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow

object PersonalData : LongIdTable() {
    val enabled = bool("ENABLED")
    val name = text("NAME").uniqueIndex()
    val value = text("VALUE")

    fun ResultRow.convertToPersonalDataEntry(): PersonalDataEntry {
        return PersonalDataEntry(
            this[id].value,
            this[enabled],
            this[name],
            this[value],
        )
    }
}