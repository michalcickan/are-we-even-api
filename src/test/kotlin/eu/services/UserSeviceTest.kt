package eu.services

import eu.exceptions.APIException
import eu.helpers.MockTransactionHandler
import eu.models.parameters.UpdateUserParameters
import eu.models.responses.users.toUser
import eu.tables.RefreshTokens
import eu.tables.UserDAO
import eu.tables.Users
import eu.validation.AuthRequestValidation
import io.mockk.clearAllMocks
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNull

class UserSeviceTest {
    private val transactionHandler = MockTransactionHandler()
    private lateinit var userService: UserService

    @Before
    fun setup() {
        transactionHandler.createTables(arrayOf(RefreshTokens, Users))
        userService = UserService(transactionHandler, AuthRequestValidation())
    }

    @After
    fun teardown() {
        transactionHandler.databaseTearDown()
        clearAllMocks()
    }

    @Test
    fun `should return user already exists when tries to register email already in database`() = runBlocking {
        val email = "some@email.com"
        val exception = runCatching {
            userService.createUser(
                email,
                null,
                null,
                null,
            )
            userService.createUser(
                email,
                null,
                null,
                null,
            )
        }
            .onSuccess {
                Exception("should not be successful")
            }
            .onFailure { e -> e }
            .exceptionOrNull()
        assertEquals(APIException.UserAlreadyExists, exception)
    }

    @Test
    fun `should create user when email is not in database`() = runBlocking {
        val email = "some@email.com"
        val user = runCatching {
            userService.createUser(
                email,
                null,
                null,
                null,
            )
        }.onFailure { e ->
            null
        }.onSuccess {
            it
        }.getOrNull()
        assertEquals(user?.email, email)
    }

    @Test
    fun `should returns all users in database`() = runBlocking {
        val emails = setOf("some@email.com", "some@email2.com", "some@email3.com")
        transactionHandler.perform {
            emails.forEach { email ->
                UserDAO.new {
                    this.email = email
                }
            }
        }
        val userEmails = userService.getUsers().map { it.email }
        assertEquals(emails.containsAll(userEmails), true)
    }

    @Test
    fun `should update an user in the database`() = runBlocking {
        val originalUser = userService.createUser("some@email.com", "not_updated", null, null)
        val updatedUserParams = UpdateUserParameters(null, "updated_user_name", null, null)

        userService.updateUser(originalUser.id, updatedUserParams)
        val updatedUser = transactionHandler.perform {
            UserDAO[originalUser.id].toUser()
        }
        assertEquals(updatedUser.email, originalUser.email)
        assertEquals(updatedUser.id, originalUser.id)
        assertEquals(updatedUser.name, updatedUserParams.name)
        assertNull(updatedUser.middleName)
    }
}
