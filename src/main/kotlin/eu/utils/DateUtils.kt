package eu.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

fun LocalDateTime.toDate(): Date {
    val instant = this.atZone(ZoneId.systemDefault()).toInstant()
    return Date.from(instant)
}

fun Date.toLocalDateTime(): LocalDateTime {
    val instant = Instant.ofEpochMilli(time)
    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
}

fun accessTokenExpiry(): Date {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR, 2) // Add 90 days to the current date
    return calendar.time
}

fun refreshTokenExpiry(): Date {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 90) // Add 90 days to the current date
    return calendar.time
}
