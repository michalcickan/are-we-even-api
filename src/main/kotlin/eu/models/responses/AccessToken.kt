package eu.models.responses

import eu.tables.AccessTokenDAO
import eu.utils.DateSerializer
import eu.utils.toDate
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class AccessToken(
    val accessToken: String,
    val refreshToken: String,
    @Serializable(with = DateSerializer::class)
    val expiryDate: Date,
)

fun AccessTokenDAO.toAccessToken(refreshToken: String): AccessToken {
    return AccessToken(
        accessToken,
        refreshToken,
        expiryDate.toDate(),
    )
}
