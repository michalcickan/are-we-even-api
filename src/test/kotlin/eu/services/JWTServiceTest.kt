import eu.helpers.DateHelpers
import eu.helpers.MockTransactionHandler
import eu.models.responses.users.toUser
import eu.services.JWTService
import eu.tables.*
import eu.utils.toLocalDateTime
import io.mockk.clearAllMocks
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

class JWTServiceTest {
    private val transactionHandler = MockTransactionHandler()
    private lateinit var jwtService: JWTService

    private val secret = "secret"
    private val audience = "audience"
    private val issuer = "issuer"

    @Before
    fun setup() {
        transactionHandler.createTables(arrayOf(RefreshTokens, Users))
        jwtService = JWTService(transactionHandler, secret, audience, issuer)
    }

    @After
    fun teardown() {
        transactionHandler.databaseTearDown()
        clearAllMocks()
    }

    @Test
    fun `createAccessToken should generate and return an access token and is associated to the correct user`() =
        runBlocking {
            // Arrange
            val platformSpecificToken = "platform_token"
            val loginType = LoginType.EMAIL
            transactionHandler.createTables(arrayOf(LoginTypes, AccessTokens))
            val user = transactionHandler.perform {
                LoginTypeDao.initializeTable()
                UserDAO.new {
                    this.email = "test@test.com"
                }.toUser()
            }
            // Act
            val accessToken = jwtService.createAccessToken(platformSpecificToken, loginType, user.id)

            // Assert
            val resultAccessTokenUserId = transactionHandler.perform {
                AccessTokenDAO
                    .find(AccessTokens.accessToken eq accessToken.accessToken)
                    .first()
                    .user
                    .id
                    .value
            }
            assertEquals(resultAccessTokenUserId, user.id)
        }

    @Test
    fun `getRefreshToken should return matching refresh token`() = runBlocking {
        // Arrange
        val refreshToken = "refresh_token"
        var expectedRefreshToken = transactionHandler.perform {
            RefreshTokenDAO.new {
                this.refreshToken = refreshToken
                this.expiryDate = DateHelpers.addDaysHours(1).toLocalDateTime()
                this.user = UserDAO.new { this.email = "test@email.com" }
            }.toRefreshToken()
        }
        // Act
        val result = jwtService.getRefreshToken(refreshToken)

        // Assert
        assertEquals(expectedRefreshToken, result)
    }

    @Test
    fun `getRefreshToken should return null if no refresh token found`() = runBlocking {
        // Arrange
        val refreshToken = "invalid_token"
        // Act
        val result = jwtService.getRefreshToken(refreshToken)

        // Assert
        assertNull(result)
    }

    @Test
    fun `getRefreshToken should return null if refresh token expired`() = runBlocking {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -1)
        val refreshToken = "some_invalid_refresh_token"

        val jwtService = JWTService(
            transactionHandler,
            "",
            "",
            "",
        )
        transactionHandler.perform {
            val user = UserDAO.new {
                this.email = "some@email.com"
            }
            RefreshTokenDAO.new {
                this.expiryDate = DateHelpers.addDaysHours(-1).toLocalDateTime()
                this.refreshToken = refreshToken
                this.user = user
            }
        }
        // Act
        val result = jwtService.getRefreshToken(refreshToken)

        // Assert
        assertNull(result)
    }
}
