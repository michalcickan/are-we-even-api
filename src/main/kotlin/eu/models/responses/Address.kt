package eu.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val id: Int,
    val street: String,
    val zip: String,
    val city: String?,
    val country: String,
)
