package eu.models.parameters

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationParameters(
    val password: String,
    val email: String,
)
