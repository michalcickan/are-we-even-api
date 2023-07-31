package eu.helpers

import java.util.*

class DateHelpers {
    companion object {
        fun addDaysHours(amount: Int): Date {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR_OF_DAY, amount)
            return calendar.time
        }
    }
}
