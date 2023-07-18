package eu.models.parameters

import kotlinx.serialization.Serializable

@Serializable
data class LoginParameters(
    val idToken: String?,
    val password: String?,
    val email: String?,
)
