package eu.models.responses

import eu.tables.AccessTokenDAO
import eu.utils.DateSerializer
import eu.utils.toDate
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class AccessToken(
    val id: Int,
    val accessToken: String,
    @Serializable(with = DateSerializer::class)
    val expiryDate: Date,
)

fun AccessTokenDAO.toAccessToken(): AccessToken {
    return AccessToken(
        id.value,
        accessToken,
        expiryDate.toDate(),
    )
}
