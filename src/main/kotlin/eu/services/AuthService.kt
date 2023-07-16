package eu.services

import LoginType
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import eu.models.responses.AccessToken
import eu.models.responses.User
import eu.routes.env
import eu.routes.jsonFactory
import eu.routes.transport
import eu.utils.APIException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@Serializable
data class LoginParameters(
    val idToken: String?,
    val password: String?,
    val email: String?,
    val loginType: LoginType,
)

@Serializable
data class RegistrationParameters(
    val password: String,
    val email: String,
)

interface IAuthService {
    suspend fun loginWith(parameters: LoginParameters): AccessToken
    suspend fun registerWith(parameters: RegistrationParameters): AccessToken
}

class AuthService(private val userService: IUserService, private val passwordEncoder: PasswordEncoder) : IAuthService {
    override suspend fun loginWith(parameters: LoginParameters): AccessToken {
        val user = when (parameters.loginType) {
            LoginType.GOOGLE -> loginWithGoogle(parameters.idToken)
            LoginType.EMAIL -> loginWithEmail(parameters.email, parameters.password)
            else -> null
        } ?: throw APIException.UserDoesNotExist
        return userService.createAccessToken(
            parameters.idToken,
            parameters.loginType,
            user.id,
            null,
        )
    }

    override suspend fun registerWith(parameters: RegistrationParameters): AccessToken {
        val user = userService.createUser(parameters.email, null, null, null)
        // no need to add "salt", since library does it automatically
        userService.createUserPassword(user.id, passwordEncoder.encode(parameters.password))
        return userService.createAccessToken(null, LoginType.EMAIL, user.id, null)
    }

    private suspend fun loginWithGoogle(idToken: String?): User? {
        if (idToken == null) {
            throw APIException.LoginTypeNeedsIdToken
        }
        val response = withContext(Dispatchers.IO) {
            async { verifyGoogleIdToken(idToken) }
        }
        val googleIdToken = response.await()
        val payload = googleIdToken?.payload
        return when {
            payload != null -> userService.createUser(
                payload.email,
                payload.get("given_name").toString(),
                null,
                payload.get("family_name").toString(),
            )

            else -> null
        }
    }

    private suspend fun loginWithEmail(email: String?, password: String?): User {
        if (email == null || password == null) {
            throw APIException.InvalidEmailFormat
        }
        val user = userService.getUser(email) ?: throw APIException.UserDoesNotExist
        val storedPassword = userService.getPassword(user.id)
        if (passwordEncoder.matches(password, storedPassword)) {
            return user
        } else {
            throw APIException.LoginNotMatch
        }
    }

    private suspend fun verifyGoogleIdToken(accessToken: String): GoogleIdToken? =
        withContext(Dispatchers.IO) {
            val verifier = GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(env["GOOGLE_CLIENT_ID"]))
                .build()

            return@withContext try {
                verifier.verify(accessToken)
            } catch (e: Exception) {
                print(e.toString())
                null
            }
        }
}
