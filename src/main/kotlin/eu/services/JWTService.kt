package eu.services

import LoginType
import LoginTypeDao
import LoginTypes
import RefreshToken
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import eu.exceptions.APIException
import eu.models.responses.AccessToken
import eu.models.responses.toAccessToken
import eu.modules.ITransactionHandler
import eu.tables.AccessTokenDAO
import eu.tables.RefreshTokenDAO
import eu.tables.RefreshTokens
import eu.tables.UserDAO
import eu.utils.accessTokenExpiry
import eu.utils.refreshTokenExpiry
import eu.utils.toLocalDateTime
import io.ktor.server.auth.jwt.*
import toRefreshToken
import java.time.LocalDateTime

interface IJWTService {
    suspend fun createAccessToken(
        platformSpecificToken: String?,
        loginType: LoginType,
        userId: Long,
    ): AccessToken

    suspend fun getRefreshToken(refreshToken: String): RefreshToken?

    fun buildVerifierForToken(): JWTVerifier

    fun getJWTPrincipal(payload: Payload): JWTPrincipal?
    fun getUserIdFromPrincipalPayload(principal: JWTPrincipal?): Long
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
        if (payload?.getClaim(claim)?.asLong() != null) {
            return JWTPrincipal(payload)
        }
        return null
    }

    override fun getUserIdFromPrincipalPayload(principal: JWTPrincipal?): Long {
        return principal?.payload?.getClaim(claim)?.asLong() ?: throw APIException.TokenExpired
    }

    override suspend fun createAccessToken(
        platformSpecificToken: String?,
        loginType: LoginType,
        userId: Long,
    ): AccessToken {
        return transactionHandler.perform {
            val user = UserDAO[userId]
            val refreshToken = RefreshTokenDAO.new {
                this.user = user
                this.refreshToken = generateToken(userId, true)
                this.expiryDate = refreshTokenExpiry().toLocalDateTime()
            }
            AccessTokenDAO.new {
                this.platformAgnosticToken = platformSpecificToken
                this.accessToken = generateToken(userId, false)
                this.loginType = LoginTypeDao.find { LoginTypes.loginType eq loginType }.first()
                this.user = UserDAO[userId]
                this.expiryDate = accessTokenExpiry().toLocalDateTime()
            }.toAccessToken(refreshToken.refreshToken)
        }
    }

    override suspend fun getRefreshToken(refreshToken: String): RefreshToken? {
        return transactionHandler.perform {
            try {
                RefreshTokenDAO
                    .find { RefreshTokens.refreshToken eq refreshToken }
                    .first { it.expiryDate.isAfter(LocalDateTime.now()) || it.expiryDate.isEqual(LocalDateTime.now()) }
                    .toRefreshToken()
            } catch (e: Exception) {
                print(e.toString())
                null
            }
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
