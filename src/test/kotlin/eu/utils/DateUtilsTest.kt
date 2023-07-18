package eu.utils

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class DateUtilsTest {
    @Test
    fun `refresh token expiration is in 3 months`() {
        val expectedCalendar = Calendar.getInstance()
        expectedCalendar.add(Calendar.DAY_OF_MONTH, 90) // Add 90 days to the current date
        val calendar = Calendar.getInstance()
        calendar.time = refreshTokenExpiry()
        assertEquals(calendar.get(Calendar.MONTH), expectedCalendar.get(Calendar.MONTH))
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), expectedCalendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(calendar.get(Calendar.YEAR), expectedCalendar.get(Calendar.YEAR))
        assertEquals(calendar.get(Calendar.HOUR), expectedCalendar.get(Calendar.HOUR))
    }
}
