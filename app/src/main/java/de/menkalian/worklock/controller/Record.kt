package de.menkalian.worklock.controller

import java.time.Instant

data class Record(
    val id: Long,
    val timestamp: Instant,
    val type: RecordType,

    val correctionForId: Long?,
    val manual: Boolean,
    val deleted: Boolean,
)
