import eu.exceptions.APIException
import eu.models.parameters.LoginParameters
import eu.models.parameters.RefreshTokenParameters
import eu.models.parameters.RegistrationParameters
import eu.models.responses.AccessToken
import eu.models.responses.users.User
import eu.services.AuthService
import eu.services.IJWTService
import eu.services.IUserService
import eu.utils.toDate
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.*

class AuthServiceTest {
    private lateinit var authService: AuthService

    private val userService: IUserService = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val jwtService: IJWTService = mockk()

    @Before
    fun setup() {
        authService = AuthService(userService, passwordEncoder, jwtService)

        every { passwordEncoder.encode(any()) } returns "fdafdadfasdfdasf"
    }

    @After
    fun teardown() {
        print("tearing down")
        clearAllMocks()
    }

    @Test
    fun `loginWith should throw UserDoesNotExist exception for non-existent user`() = runBlocking {
        // Arrange
        val parameters = LoginParameters(email = "nonexistent@test.eu", password = "password", idToken = null)
        val loginType = LoginType.EMAIL
        coEvery { userService.getUser(email = any()) } returns null
        coEvery { userService.getPassword(any()) } returns "something-encoded"
        every { passwordEncoder.matches(parameters.password, "something-encoded") } returns false
        // Act & Assert
        val result = runCatching {
            authService.loginWith(parameters, loginType)
        }
            .onFailure { e -> e }
            .exceptionOrNull()
        assertEquals(result, APIException.UserDoesNotExist)
    }

    @Test
    fun `loginWith should throw LoginNotMatch exception for incorrect password`() = runBlocking {
        // Arrange
        val parameters = LoginParameters(email = "test@test.eu", password = "incorrect", idToken = null)
        val loginType = LoginType.EMAIL
        coEvery { userService.getUser(email = any()) } returns makeUser()
        coEvery { userService.getPassword(any()) } returns "something-encoded"
        every { passwordEncoder.matches(parameters.password, "something-encoded") } returns false

        // Act
        val result = runCatching {
            authService.loginWith(parameters, loginType)
        }
            .onFailure { e -> e }
            .exceptionOrNull()
        assertEquals(result, APIException.LoginNotMatch)
    }

    @Test
    fun `registerWith should create a new user and return an access token`() = runBlocking {
        // Arrange
        val parameters = RegistrationParameters(email = "newuser@test.eu", password = "Qwerty123!")

        coEvery { userService.createUser(any(), any(), any(), any()) } returns makeUser()
        coEvery { jwtService.createAccessToken(any(), any(), any()) } returns AccessToken(
            accessToken = "access_token",
            refreshToken = "refresh_token",
            expiryDate = LocalDateTime.now().plusHours(2).toDate(),
        )
        coEvery { userService.storeUserPassword(any(), any()) } returns Unit
        // Act
        val accessToken = authService.registerWith(parameters)

        // Assert
        assertNotNull(accessToken)
        assertNotNull(accessToken.accessToken)
        assertNotNull(accessToken.refreshToken)
        assertNotNull(accessToken.expiryDate)
    }

    @Test
    fun `recreateAccessToken should create a new access token for a valid refresh token`() = runBlocking {
        // Arrange
        val refreshToken = "valid_token"
        val parameters = RefreshTokenParameters(refreshToken = refreshToken, clientId = "")

        // Mock JWT service to return a valid access token
        coEvery { jwtService.createAccessToken(any(), any(), any()) } returns AccessToken(
            accessToken = "access_token",
            refreshToken = "refresh_token",
            expiryDate = LocalDateTime.now().plusHours(2).toDate(),
        )

        coEvery { jwtService.getRefreshToken(any()) } returns RefreshToken(
            id = 2,
            refreshToken = "",
            expiryDate = Calendar.getInstance().apply {
                time = Date()
                add(Calendar.MINUTE, 2)
            }.time,
            2,
        )

        // Act
        val accessToken = authService.recreateAccessToken(parameters)

        // Assert
        assertNotNull(accessToken)
        assertEquals("access_token", accessToken.accessToken)
        assertEquals("refresh_token", accessToken.refreshToken)
        assertNotNull(accessToken.expiryDate)
    }

    @Test
    fun `recreateAccessToken should throw TokenExpired exception for an expired refresh token`() = runBlocking {
        // Arrange
        val refreshToken = "expired_token"
        val parameters = RefreshTokenParameters(refreshToken = refreshToken, clientId = "")

        // Mock JWT service to return an expired refresh token
        coEvery {
            jwtService.getRefreshToken(any())
        } returns RefreshToken(
            id = 2,
            refreshToken = "",
            expiryDate = Calendar.getInstance().apply {
                time = Date()
                add(Calendar.MINUTE, -2)
            }.time,
            userId = 2,
        )
        val result = runCatching {
            authService.recreateAccessToken(parameters)
        }
            .onFailure { e -> e }
            .exceptionOrNull()
        assertEquals(result, APIException.TokenExpired)
    }
}

private fun makeUser(): User {
    return User(
        2,
        null,
        null,
        null,
        emptyList(),
        "test@test.eu",
    )
}
