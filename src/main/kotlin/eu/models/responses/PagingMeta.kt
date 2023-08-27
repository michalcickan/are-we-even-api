package eu.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class PagingMeta(
    val totalCount: Long,
    val offset: Long,
)
