package eu.models.responses.users

import kotlinx.serialization.Serializable

@Serializable
data class OweeUser(
    val id: Long,
    val name: String,
    val usersTo: List<PayerUser>,
)
