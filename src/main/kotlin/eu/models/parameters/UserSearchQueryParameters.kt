package eu.models.parameters

import eu.exceptions.ValidationException
import io.ktor.server.application.*

class UserSearchQueryParameters(
    val filterCol: UserFilterColumn?,
    val query: String,
    val limit: Int?,
    val offset: Long?,
)

fun ApplicationCall.extractUserSearchQueryParameters(): UserSearchQueryParameters {
    val queryParameters = request.queryParameters
    val rawFilterCol = queryParameters[QueryParameter.FILTER_COL]
    val query = queryParameters[QueryParameter.QUERY] ?: throw ValidationException.QueryIsMissing
    val limit = queryParameters[QueryParameter.LIMIT]?.toInt()
    val offset = queryParameters[QueryParameter.OFFSET]?.toLong()
    val filterCol = rawFilterCol?.let { it1 -> enumValueOf<UserFilterColumn>(it1) }

    return UserSearchQueryParameters(filterCol, query, limit, offset)
}
