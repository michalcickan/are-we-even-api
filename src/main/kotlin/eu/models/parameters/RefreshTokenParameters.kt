package eu.models.parameters

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenParameters(
    val refreshToken: String,
    val clientId: String,
)
