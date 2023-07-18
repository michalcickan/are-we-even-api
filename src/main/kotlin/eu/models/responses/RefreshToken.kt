
import eu.tables.RefreshTokenDAO
import eu.utils.toDate
import java.util.*

data class RefreshToken(
    val id: Int,
    val refreshToken: String,
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
