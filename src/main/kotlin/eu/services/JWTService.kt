package eu.services

import LoginType
import LoginTypeDao
import LoginTypeTable
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import eu.models.responses.AccessToken
import eu.models.responses.toAccessToken
import eu.modules.ITransactionHandler
import eu.tables.AccessTokenDAO
import eu.tables.RefreshTokenDAO
import eu.tables.RefreshTokens
import eu.tables.UserDAO
import io.ktor.server.auth.jwt.*
import java.time.LocalDateTime
import java.util.*

interface IJWTService {
    suspend fun createAccessToken(
        platformSpecificToken: String?,
        loginType: LoginType,
        userId: Long,
        expiryDate: LocalDateTime?,
    ): AccessToken

    suspend fun getRefreshToken(refreshToken: String): RefreshTokenDAO?

    fun buildVerifierForToken(): JWTVerifier

    fun getJWTPrincipal(payload: Payload): JWTPrincipal?
}

class JWTService(
    private val transactionHandler: ITransactionHandler,
    private val secret: String,
    private val audience: String,
    private val issuer: String,
) : IJWTService {
    private val claim = "userId"

    override fun buildVerifierForToken(): JWTVerifier {
        return JWT
            .require(Algorithm.HMAC256(secret))
            .withAudience(audience)
            .withIssuer(issuer)
            .build()
    }

    override fun getJWTPrincipal(payload: Payload): JWTPrincipal? {
        if (payload.getClaim(claim).asLong() != null) {
            return JWTPrincipal(payload)
        }
        return null
    }

    override suspend fun createAccessToken(
        platformSpecificToken: String?,
        loginType: LoginType,
        userId: Long,
        expiryDate: LocalDateTime?,
    ): AccessToken {
        return transactionHandler.perform {
            val user = UserDAO[userId]
            val refreshToken = RefreshTokenDAO.new {
                this.user = user
                this.refreshToken = generateToken(userId, true)
            }
            AccessTokenDAO.new {
                this.platformAgnosticToken = platformSpecificToken
                this.accessToken = generateToken(userId, false)
                this.loginType = LoginTypeDao.find { LoginTypeTable.loginType eq loginType }.first()
                this.user = UserDAO[userId]
                this.expiryDate = expiryDate ?: LocalDateTime.now().plusHours(2)
            }.toAccessToken(refreshToken.refreshToken)
        }
    }

    override suspend fun getRefreshToken(refreshToken: String): RefreshTokenDAO? {
        return transactionHandler.perform {
            RefreshTokenDAO.find { RefreshTokens.refreshToken eq refreshToken }.first()
        }
    }

    private fun generateToken(userId: Long, isRefreshToken: Boolean): String {
        val expiryDate = if (isRefreshToken) {
            refreshTokenExpiry()
        } else {
            accessTokenExpiry()
        }
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim(claim, userId)
            .withExpiresAt(expiryDate)
            .sign(Algorithm.HMAC256(secret))
    }
}

private fun accessTokenExpiry(): Date {
    return Date(System.currentTimeMillis() + 20 * 60 * 1000)
}

private fun refreshTokenExpiry(): Date {
    val expiryDurationMillis = 90 * 24 * 60 * 60 * 1000L // 90 days in milliseconds
    return Date(System.currentTimeMillis() + expiryDurationMillis)
}
