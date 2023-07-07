package eu.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

interface IJWTService {
    fun generateToken(userId: Long): String
}

class JWTService(private val secret: String) : IJWTService {
    override fun generateToken(userId: Long): String {
//        val audience = environment.config.property("jwt.audience").getString()
//        val issuer = environment.config.property("jwt.audience").getString()
        return JWT.create()
            .withAudience("localhost:8080")
            .withIssuer("localhost:8000")
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256(secret))
    }
}
