package eu.models.parameters

import io.ktor.http.*
import org.jetbrains.exposed.sql.exposedLogger

enum class Sort(val type: String) {
    ASC("asc"), DESC("desc"),
}

object QueryParameter {
    const val FILTER_COL = "filterCol"
    const val QUERY = "query"
    const val LIMIT = "limit"
    const val OFFSET = "offset"
    const val SORT = "sort"
}

fun Parameters.getSort(): Sort? {
    val rawSort = this[QueryParameter.SORT]
    try {
        return rawSort?.let { enumValueOf<Sort>(it.uppercase()) }
    } catch (e: Exception) {
        exposedLogger.debug(e.toString())
        return null
    }
}
