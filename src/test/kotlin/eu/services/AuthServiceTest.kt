import eu.models.parameters.LoginParameters
import eu.models.parameters.RefreshTokenParameters
import eu.models.parameters.RegistrationParameters
import eu.models.responses.AccessToken
import eu.models.responses.User
import eu.services.AuthService
import eu.services.IJWTService
import eu.services.IUserService
import eu.utils.APIException
import eu.utils.toDate
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.*
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
    fun `withLogin should throw the IncorrectLoginValues exception if password or email are null`() = runBlocking {
        val loginType = LoginType.EMAIL

        try {
            val parameters = LoginParameters(email = null, password = "password", idToken = null)
            val result = authService.loginWith(parameters, loginType)
        } catch (e: APIException.IncorrectLoginValues) {
            //
        } catch (e: Exception) {
            fail("not a good exception $e")
        }

        try {
            val parameters = LoginParameters(email = "mic@test.eu", password = null, idToken = null)
            val result = authService.loginWith(parameters, loginType)
        } catch (e: APIException.IncorrectLoginValues) {
            //
        } catch (e: Exception) {
            fail("not a good exception $e")
        }
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

        try {
            val result = authService.loginWith(parameters, loginType)
        } catch (e: APIException.UserDoesNotExist) {
            //
        } catch (e: Exception) {
            fail("not a good exception $e")
        }
    }

    @Test
    fun `loginWith should throw LoginNotMatch exception for incorrect password`() = runBlocking {
        // Arrange
        val parameters = LoginParameters(email = "test@test.eu", password = "incorrect", idToken = null)
        val loginType = LoginType.EMAIL
        coEvery { userService.getUser(email = any()) } returns makeUser()
        coEvery { userService.getPassword(any()) } returns "something-encoded"
        every { passwordEncoder.matches(parameters.password, "something-encoded") } returns false
        try {
            val result = authService.loginWith(parameters, loginType)
        } catch (e: APIException.LoginNotMatch) {
            //
        } catch (e: Exception) {
            fail("not a good exception $e")
        }
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

        try {
            val token = authService.recreateAccessToken(parameters)
            fail("should throw exception")
        } catch (e: APIException.TokenExpired) {
            //
        } catch (e: Exception) {
            fail("not good exception $e")
        }
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
