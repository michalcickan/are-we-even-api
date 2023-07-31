package eu.models.parameters

import kotlinx.serialization.Serializable

@Serializable
data class CreateGroupParameters(
    val name: String,
)
