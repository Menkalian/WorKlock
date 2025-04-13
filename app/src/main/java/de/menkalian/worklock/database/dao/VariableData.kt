package de.menkalian.worklock.database.dao

import de.menkalian.vela.TransferableValue
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow

object VariableData : LongIdTable() {
    val key = text("KEY")
    val value = text("VALUE")
    val type = enumeration<VariableType>("TYPE")

    val map = reference("ADDITIONAL_DATA_ID", AdditionalData.id)

    init {
        uniqueIndex(map, key)
    }

    fun convertToEntry(result: ResultRow): Pair<String, TransferableValue> {
        val key = result[VariableData.key]
        val value = result[VariableData.value]
        val type = when (result[VariableData.type]) {
            VariableType.BOOL   -> TransferableValue.TransferableValueType.BOOLEAN
            VariableType.STRING -> TransferableValue.TransferableValueType.STRING
            VariableType.INT    -> TransferableValue.TransferableValueType.INTEGER
        }
        return key to TransferableValue(type, value)
    }
}