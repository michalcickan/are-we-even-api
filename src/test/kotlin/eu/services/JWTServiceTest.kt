import eu.models.responses.AccessToken
import eu.modules.ITransactionHandler
import eu.services.JWTService
import eu.utils.toDate
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.*

class JWTServiceTest {
    private lateinit var jwtService: JWTService

    private val transactionHandler: ITransactionHandler = mockk()

    private val secret = "secret"
    private val audience = "audience"
    private val issuer = "issuer"

    @Before
    fun setup() {
        jwtService = JWTService(transactionHandler, secret, audience, issuer)
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `createAccessToken should generate and return an access token`() = runBlocking {
        // Arrange
        val platformSpecificToken = "platform_token"
        val loginType = LoginType.EMAIL
        val userId = 123L
        val expiryDate = LocalDateTime.now().plusHours(2)
        val expectedAccessToken = AccessToken("access_token", "refresh_token", expiryDate.toDate())

        coEvery { transactionHandler.perform<AccessToken>(any()) } returns expectedAccessToken
        // Act
        val accessToken = jwtService.createAccessToken(platformSpecificToken, loginType, userId)

        // Assert
        assertEquals(expectedAccessToken, accessToken)
    }

    @Test
    fun `getRefreshToken should return matching refresh token`() = runBlocking {
        // Arrange
        val refreshToken = "refresh_token"
        val expectedRefreshToken = RefreshToken(2, "refresh_token", expiryDate = Date(), userId = 2)
        coEvery { transactionHandler.perform<RefreshToken>(any()) } returns expectedRefreshToken
        // Act
        val result = jwtService.getRefreshToken(refreshToken)

        // Assert
        assertEquals(expectedRefreshToken, result)
    }

    @Test
    fun `getRefreshToken should return null if no refresh token found`() = runBlocking {
        // Arrange
        val refreshToken = "invalid_token"
        coEvery { transactionHandler.perform<RefreshToken?>(any()) } returns null
        // Act
        val result = jwtService.getRefreshToken(refreshToken)

        // Assert
        assertNull(result)
    }
}
