package eu.services

import LoginType
import LoginTypeDAO
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
import eu.tables.*
import eu.utils.accessTokenExpiry
import eu.utils.refreshTokenExpiry
import eu.utils.toLocalDateTime
import io.ktor.server.auth.jwt.*
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import toRefreshToken
import java.time.LocalDateTime
import java.util.*

interface IJWTService {
    suspend fun createAccessToken(
        platformSpecificToken: String?,
        loginType: LoginType,
        deviceId: String,
        userId: Long,
    ): AccessToken

    suspend fun removeTokens(
        userId: Long,
        deviceId: String,
    )

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
        deviceId: String,
        userId: Long,
    ): AccessToken {
        return transactionHandler.perform {
            val user = UserDAO[userId]
            val uuid = deviceId.uuid()
            val device = DeviceDAO.findById(uuid) ?: DeviceDAO.new(uuid) {}
            val refreshToken = RefreshTokenDAO.new {
                this.user = user
                this.refreshToken = generateToken(userId, true)
                this.expiryDate = refreshTokenExpiry().toLocalDateTime()
                this.device = device
            }
            AccessTokenDAO.new {
                this.platformAgnosticToken = platformSpecificToken
                this.accessToken = generateToken(userId, false)
                this.loginType = LoginTypeDAO.find { LoginTypes.loginType eq loginType }.first()
                this.user = UserDAO[userId]
                this.expiryDate = accessTokenExpiry().toLocalDateTime()
                this.device = device
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

    override suspend fun removeTokens(
        userId: Long,
        deviceId: String,
    ) {
        transactionHandler.perform {
            AccessTokenDAO
                .find(
                    (AccessTokens.userId eq userId) and (AccessTokens.deviceId eq deviceId.uuid()),
                )
                .safeRemove()
            RefreshTokenDAO
                .find(
                    (RefreshTokens.userId eq userId) and (RefreshTokens.deviceId eq deviceId.uuid()),
                )
                .safeRemove()
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

fun SizedIterable<IntEntity>.safeRemove() {
    if (!empty()) {
        first().delete()
    }
}

private fun String.uuid(): UUID {
    return UUID.fromString(this)
}
