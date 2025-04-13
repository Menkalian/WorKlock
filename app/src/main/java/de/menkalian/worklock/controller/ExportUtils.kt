package de.menkalian.worklock.controller

import java.time.*
import kotlin.math.absoluteValue

fun getFullTimeForDayInMinutes(records: List<Record>, endOfDay: LocalTime = LocalTime.MAX): Pair<Boolean, Int> {
    val sortedRecords = records.sortedBy { it.timestamp }
        .filter { !it.deleted && records.none { other -> other.correctionForId == it.id } }
    var hasErrors = false
    var accumulated = 0
    var previousStartPoint: LocalTime? = LocalTime.MIN.takeIf { sortedRecords.firstOrNull()?.type in listOf(RecordType.End, RecordType.Pause) }

    for (record in sortedRecords) {
        when (record.type) {
            RecordType.Start, RecordType.Unpause -> {
                if (previousStartPoint != null) {
                    hasErrors = true
                    accumulated += diffSeconds(previousStartPoint, record.timestamp.getTime())
                }
                previousStartPoint = record.timestamp.getTime()
            }

            RecordType.End, RecordType.Pause     -> {
                if (previousStartPoint == null) {
                    // no way of correcting anything
                    hasErrors = true
                } else {
                    accumulated += diffSeconds(previousStartPoint, record.timestamp.getTime())
                    previousStartPoint = null
                }
            }
        }
    }

    if (previousStartPoint != null) {
        // Missing endpoint is not an error
        val startTime = previousStartPoint
        accumulated += (endOfDay.toSecondOfDay() - startTime.toSecondOfDay()).absoluteValue
    }

    return Pair(hasErrors, accumulated / 60) // convert seconds to minutes.
}

fun getPauseTimeForDayInMinutes(records: List<Record>, endOfDay: LocalTime = LocalTime.MAX): Pair<Boolean, Int> {
    val sortedRecords = records.sortedBy { it.timestamp }
        .filter { !it.deleted && records.none { other -> other.correctionForId == it.id } }
    var hasErrors = false
    var accumulated = 0
    var previousStartPoint: LocalTime? = LocalTime.MIN.takeIf { sortedRecords.firstOrNull()?.type in listOf(RecordType.Unpause) }

    for (record in sortedRecords) {
        when (record.type) {
            RecordType.Unpause               -> {
                if (previousStartPoint == null) {
                    // no way of correcting anything
                    hasErrors = true
                } else {
                    accumulated += diffSeconds(previousStartPoint, record.timestamp.getTime())
                    previousStartPoint = null
                }
            }

            RecordType.Pause                 -> {
                if (previousStartPoint != null) {
                    hasErrors = true
                    accumulated += diffSeconds(previousStartPoint, record.timestamp.getTime())
                }
                previousStartPoint = record.timestamp.getTime()
            }

            RecordType.Start, RecordType.End -> { /* ignored */
            }
        }
    }

    if (previousStartPoint != null) {
        // Missing endpoint is not an error
        val startTime = previousStartPoint
        accumulated += (endOfDay.toSecondOfDay() - startTime.toSecondOfDay()).absoluteValue
    }

    return Pair(hasErrors, accumulated / 60) // convert seconds to minutes.
}

fun diffSeconds(startPoint: LocalTime, endPoint: LocalTime): Int {
    return (startPoint.toSecondOfDay() - endPoint.toSecondOfDay()).absoluteValue
}

fun Instant.getTime(): LocalTime {
    return LocalDateTime.ofInstant(this, ZoneId.systemDefault()).toLocalTime()
}

fun Instant.getDate(): LocalDate {
    return LocalDateTime.ofInstant(this, ZoneId.systemDefault()).toLocalDate()
}
