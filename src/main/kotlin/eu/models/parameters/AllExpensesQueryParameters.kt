package eu.models.parameters

import io.ktor.server.application.*

class AllExpensesQueryParameters(
    offset: Long?,
    limit: Int?,
    val sort: Sort?,
) : PagingParameters(offset, limit)

fun ApplicationCall.extractAllExpensesQueryParameters(): AllExpensesQueryParameters {
    val queryParameters = request.queryParameters
    val pagingParameters = PagingParameters(queryParameters)

    return AllExpensesQueryParameters(
        pagingParameters.offset,
        pagingParameters.limit,
        queryParameters.getSort(),
    )
}
