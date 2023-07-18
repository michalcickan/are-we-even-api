package eu.models.parameters

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserParameters(
    val email: String?,
    val name: String?,
    val middleName: String?,
    val surname: String?,
)

@Serializable
data class CreateUserAddressParameters(
    val city: String,
    val zip: String,
    val street: String,
    val country: String,
)
