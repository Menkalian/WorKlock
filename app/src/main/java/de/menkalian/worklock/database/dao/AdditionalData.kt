package de.menkalian.worklock.database.dao

import de.menkalian.vela.TransferableValue
import de.menkalian.worklock.database.dao.VariableData.convertToEntry
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object AdditionalData : LongIdTable() {
    fun getAsMap(id: Long): Map<String, TransferableValue> {
        return VariableData
            .select(VariableData.map eq id)
            .orderBy(VariableData.key, SortOrder.ASC)
            .associate { result ->
                convertToEntry(result)
            }
    }
}