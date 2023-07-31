package eu.models.responses.users

import kotlinx.serialization.Serializable

@Serializable
class PayerUser(
    val id: Long,
    val name: String,
    val amount: Double,
)
