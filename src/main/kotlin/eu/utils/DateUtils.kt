package eu.utils

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

fun LocalDateTime.toDate(): Date {
    val localDateTime = LocalDateTime.of(this.toLocalDate(), LocalTime.MIDNIGHT)
    val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()
    return Date.from(instant)
}
