import eu.tables.RefreshTokenDAO
import eu.utils.DateSerializer
import eu.utils.toDate
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class RefreshToken(
    val id: Int,
    val refreshToken: String,
    @Serializable(with = DateSerializer::class)
    val expiryDate: Date,
    val userId: Long,
)

fun RefreshTokenDAO.toRefreshToken(): RefreshToken {
    return RefreshToken(
        id = id.value,
        refreshToken = refreshToken,
        expiryDate = expiryDate.toDate(),
        userId = user.id.value,
    )
}
