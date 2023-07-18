package eu.services

import LoginType
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import eu.models.parameters.LoginParameters
import eu.models.parameters.RefreshTokenParameters
import eu.models.parameters.RegistrationParameters
import eu.models.responses.AccessToken
import eu.models.responses.User
import eu.routes.env
import eu.routes.jsonFactory
import eu.routes.transport
import eu.utils.APIException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

interface IAuthService {
    suspend fun loginWith(parameters: LoginParameters, loginType: LoginType): AccessToken
    suspend fun registerWith(parameters: RegistrationParameters): AccessToken
    suspend fun recreateAccessToken(parameters: RefreshTokenParameters): AccessToken
}

class AuthService(
    private val userService: IUserService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: IJWTService,
) : IAuthService {
    override suspend fun loginWith(parameters: LoginParameters, loginType: LoginType): AccessToken {
        validateLoginParameters(parameters, loginType)
        val user = when (loginType) {
            LoginType.GOOGLE -> loginWithGoogle(parameters.idToken!!)
            LoginType.EMAIL -> loginWithEmail(parameters.email!!, parameters.password!!)
            else -> null
        } ?: throw APIException.UserDoesNotExist
        return jwtService.createAccessToken(
            parameters.idToken,
            loginType,
            user.id,
        )
    }

    override suspend fun registerWith(parameters: RegistrationParameters): AccessToken {
        val user = userService.createUser(parameters.email, null, null, null)
        // no need to add "salt", library does it automatically
        userService.storeUserPassword(user.id, passwordEncoder.encode(parameters.password))
        return jwtService.createAccessToken(
            null,
            LoginType.EMAIL,
            user.id,
        )
    }

    override suspend fun recreateAccessToken(parameters: RefreshTokenParameters): AccessToken {
        val refreshToken = jwtService.getRefreshToken(parameters.refreshToken) ?: throw APIException.TokenExpired
        if (refreshToken.expiryDate < Date()) {
            throw APIException.TokenExpired
        }
        return jwtService.createAccessToken(
            null,
            LoginType.EMAIL,
            refreshToken.userId,
        )
    }

    private suspend fun loginWithGoogle(idToken: String): User? {
        val response = withContext(Dispatchers.IO) {
            async { verifyGoogleIdToken(idToken) }
        }
        val googleIdToken = response.await()
        val payload = googleIdToken?.payload
        return when {
            payload != null -> userService.createUser(
                payload.email,
                payload["given_name"].toString(),
                null,
                payload["family_name"].toString(),
            )

            else -> null
        }
    }

    private suspend fun loginWithEmail(email: String, password: String): User {
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

    private fun validateLoginParameters(params: LoginParameters, loginType: LoginType) {
        when (loginType) {
            LoginType.EMAIL -> {
                if (params.email == null || params.password == null) {
                    throw APIException.IncorrectLoginValues
                }
            }

            LoginType.APPLE, LoginType.GOOGLE -> {
                if (params.idToken == null) {
                    throw APIException.IncorrectLoginValues
                }
            }
        }
    }
}
