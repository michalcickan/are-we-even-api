package eu.models.parameters

import io.ktor.server.application.*

class AllExpensesQueryParameters(
    offset: Long?,
    limit: Int?,
) : PagingParameters(offset, limit)

fun ApplicationCall.extractAllExpensesQueryParameters(): AllExpensesQueryParameters {
    val pagingParameters = PagingParameters(request.queryParameters)

    return AllExpensesQueryParameters(pagingParameters.offset, pagingParameters.limit)
}
