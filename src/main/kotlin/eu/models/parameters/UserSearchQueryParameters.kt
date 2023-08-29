package eu.models.parameters

import eu.exceptions.ValidationException
import io.ktor.server.application.*

class UserSearchQueryParameters(
    val filterCol: UserFilterColumn? = null,
    val query: String,
    offset: Long? = null,
    limit: Int? = null,
) : PagingParameters(offset, limit)

fun ApplicationCall.extractUserSearchQueryParameters(): UserSearchQueryParameters {
    val queryParameters = request.queryParameters
    val rawFilterCol = queryParameters[QueryParameter.FILTER_COL]
    val query = queryParameters[QueryParameter.QUERY] ?: throw ValidationException.QueryIsMissing
    val filterCol = rawFilterCol?.let { it1 -> enumValueOf<UserFilterColumn>(it1) }
    val pagingParameters = PagingParameters(queryParameters)

    return UserSearchQueryParameters(filterCol, query, pagingParameters.offset, pagingParameters.limit)
}
