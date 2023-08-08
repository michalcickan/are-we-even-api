import eu.helpers.DateHelpers
import eu.helpers.MockTransactionHandler
import eu.helpers.StringHelpers
import eu.models.responses.users.toUser
import eu.services.JWTService
import eu.tables.*
import eu.utils.toLocalDateTime
import io.mockk.clearAllMocks
import junit.framework.TestCase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
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
        transactionHandler.createTables(arrayOf(RefreshTokens, Users, Devices, AccessTokens))
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
                LoginTypeDAO.initializeTable()
                UserDAO.new {
                    this.email = "test@test.com"
                }.toUser()
            }
            // Act
            val accessToken = jwtService.createAccessToken(
                platformSpecificToken,
                loginType,
                StringHelpers.generateRandomUUID(),
                user.id,
            )

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
    fun `createAccessToken should create device when creating access tokens`() =
        runBlocking {
            // Arrange
            val platformSpecificToken = "platform_token"
            val loginType = LoginType.EMAIL
            transactionHandler.createTables(arrayOf(LoginTypes, AccessTokens))
            val user = transactionHandler.perform {
                LoginTypeDAO.initializeTable()
                UserDAO.new {
                    this.email = "test@test.com"
                }.toUser()
            }
            val deviceUUID = StringHelpers.generateRandomUUID()
            // Act
            jwtService.createAccessToken(
                platformSpecificToken,
                loginType,
                deviceUUID,
                user.id,
            )
            val expectedUUID = UUID.fromString(deviceUUID)
            val resultDevice = transactionHandler.perform {
                DeviceDAO.findById(expectedUUID)
            }
            assertEquals(expectedUUID, resultDevice?.id?.value)
        }

    @Test
    fun `getRefreshToken should return matching refresh token`() = runBlocking {
        // Arrange
        val refreshToken = "refresh_token"
        var expectedRefreshToken = transactionHandler.perform {
            val device = DeviceDAO.new(UUID.randomUUID()) {}
            val user = UserDAO.new { this.email = "test@email.com" }
            createRefreshToken(
                user,
                device,
                1,
                refreshToken,
            )
                .toRefreshToken()
        }
        // Act
        val result = jwtService.getRefreshToken(refreshToken)

        assertEquals(expectedRefreshToken, result)
    }

    @Test
    fun `getRefreshToken should return null if no refresh token found`() = runBlocking {
        // Arrange
        val refreshToken = "invalid_token"
        // Act
        val result = jwtService.getRefreshToken(refreshToken)

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
            val device = DeviceDAO.new(UUID.randomUUID()) {}
            createRefreshToken(user, device, -1, refreshToken)
        }
        // Act
        val result = jwtService.getRefreshToken(refreshToken)

        assertNull(result)
    }

    @Test
    fun `logout should remove tokens bound to uuid`() = runBlocking {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -1)
        val refreshToken = "some_invalid_refresh_token"

        val jwtService = JWTService(
            transactionHandler,
            "",
            "",
            "",
        )
        val deviceID = StringHelpers.generateRandomUUID()
        val userId = transactionHandler.perform {
            val user = UserDAO.new {
                this.email = "some@email.com"
            }
            val device = DeviceDAO.new(UUID.fromString(deviceID)) {}
            createRefreshToken(user, device, 1, refreshToken)
            createAccessToken(user, device, 1, "some")
            user.id.value
        }
        // Act
        jwtService.removeTokens(userId, deviceID)
        val uuid = UUID.fromString(deviceID)
        val resultAccessTokenRemoved = transactionHandler.perform {
            AccessTokenDAO.find {
                (AccessTokens.deviceId eq uuid) and (AccessTokens.userId eq userId)
            }
                .empty()
        }
        val resultRefreshTokenRemoved = transactionHandler.perform {
            RefreshTokenDAO.find {
                (RefreshTokens.deviceId eq uuid) and (RefreshTokens.userId eq userId)
            }
                .empty()
        }

        assertTrue(resultRefreshTokenRemoved)
        assertTrue(resultAccessTokenRemoved)
    }

    @Test
    fun `logout should not remove tokens bound to a different device id`() = runBlocking {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -1)

        val jwtService = JWTService(
            transactionHandler,
            "",
            "",
            "",
        )

        val secondDeviceStringId = StringHelpers.generateRandomUUID()
        val secondDevice = transactionHandler.perform {
            DeviceDAO.new(UUID.fromString(secondDeviceStringId)) {}
        }
        val firstDeviceStringId = StringHelpers.generateRandomUUID()
        val firstDevice = transactionHandler.perform {
            DeviceDAO.new(UUID.fromString(firstDeviceStringId)) {}
        }
        val userId = transactionHandler.perform {
            val user = UserDAO.new {
                this.email = "some@email.com"
            }
            createRefreshToken(user, firstDevice, 1, "refreshToken")
            createAccessToken(user, firstDevice, 1, "some")

            createRefreshToken(user, secondDevice, 1, "refreshToken2")
            createAccessToken(user, secondDevice, 1, "some2")
            user.id.value
        }
        // Act
        jwtService.removeTokens(userId, firstDeviceStringId)
        val resultAccessTokenRemoved = transactionHandler.perform {
            AccessTokenDAO.find {
                (AccessTokens.deviceId eq secondDevice.id) and (AccessTokens.userId eq userId)
            }
                .empty()
        }
        val resultRefreshTokenRemoved = transactionHandler.perform {
            RefreshTokenDAO.find {
                (RefreshTokens.deviceId eq secondDevice.id) and (RefreshTokens.userId eq userId)
            }
                .empty()
        }

        assertFalse(resultRefreshTokenRemoved)
        assertFalse(resultAccessTokenRemoved)
    }
}

private fun CoroutineScope.createRefreshToken(
    user: UserDAO,
    device: DeviceDAO,
    dayAmount: Int,
    refreshToken: String,
): RefreshTokenDAO {
    return RefreshTokenDAO.new {
        this.expiryDate = DateHelpers.addDaysHours(dayAmount).toLocalDateTime()
        this.refreshToken = refreshToken
        this.user = user
        this.device = device
    }
}

private fun CoroutineScope.createAccessToken(
    user: UserDAO,
    device: DeviceDAO,
    dayAmount: Int,
    accessToken: String = "some_access_token",
): AccessTokenDAO {
    val type = LoginType.EMAIL
    val loginType =
        LoginTypeDAO.find { LoginTypes.loginType eq type }.firstOrNull() ?: LoginTypeDAO.new { this.loginType = type }
    return AccessTokenDAO.new {
        this.expiryDate = DateHelpers.addDaysHours(dayAmount).toLocalDateTime()
        this.accessToken = accessToken
        this.user = user
        this.device = device
        this.loginType = loginType
    }
}
