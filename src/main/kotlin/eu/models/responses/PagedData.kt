package eu.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class PagedData<T>(
    val data: List<T>,
    val meta: PagingMeta,
)
