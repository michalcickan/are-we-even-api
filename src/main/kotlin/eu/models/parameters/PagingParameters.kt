package eu.models.parameters

import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
open class PagingParameters(
    val offset: Long?,
    val limit: Int?,
)

fun PagingParameters(queryParameters: Parameters) = PagingParameters(
    queryParameters[QueryParameter.OFFSET]?.toLong(),
    queryParameters[QueryParameter.LIMIT]?.toInt(),
)
