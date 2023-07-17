package eu.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class APIError(val message: String)
