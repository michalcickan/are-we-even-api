package eu.services

import eu.exceptions.APIException
import eu.helpers.MockTransactionHandler
import eu.helpers.fillUsers
import eu.helpers.makeGroupAndGetId
import eu.models.responses.toInvitation
import eu.tables.GroupDAO
import eu.tables.InvitationDAO
import eu.tables.Invitations
import eu.tables.UserDAO
import io.mockk.clearAllMocks
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.and
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class InvitationServiceTest {
    private val transactionHandler = MockTransactionHandler()
    private lateinit var invitationService: InvitationService

    @Before
    fun setup() {
        transactionHandler.createTables(arrayOf(Invitations))
        invitationService = InvitationService(transactionHandler)
    }

    @After
    fun tearDown() {
        transactionHandler.databaseTearDown()
        clearAllMocks()
    }

    @Test
    fun `should insert invitation into table`() =
        runBlocking {
            val user = transactionHandler.fillUsers(1)[0]
            val groupId = transactionHandler.makeGroupAndGetId(user.id)

            val result = invitationService.makeUserInvitationForGroup(groupId, user.id)

            val databaseRow = transactionHandler.perform {
                InvitationDAO.find {
                    (Invitations.userId eq user.id) and (Invitations.groupId eq groupId)
                }
                    .first()
                    .toInvitation()
            }
            assertEquals(result.id, databaseRow.id)
        }

    @Test
    fun `should throw the UserAlreadyInvited exception when invitation pending for user and group`() =
        runBlocking {
            val user = transactionHandler.fillUsers(1)[0]
            val groupId = transactionHandler.makeGroupAndGetId(user.id)
            transactionHandler.perform {
                InvitationDAO.new {
                    this.user = UserDAO[user.id]
                    this.group = GroupDAO[groupId]
                }
            }
            val exception = runCatching {
                invitationService.makeUserInvitationForGroup(groupId, user.id)
            }
                .onFailure { e -> e }
                .exceptionOrNull()
            assertEquals(APIException.UserAlreadyInvited, exception)
        }

    @Test
    fun `should not throw the UserAlreadyInvited exception when invitation was accepted`() =
        runBlocking {
            val user = transactionHandler.fillUsers(1)[0]
            val groupId = transactionHandler.makeGroupAndGetId(user.id)
            transactionHandler.perform {
                InvitationDAO.new {
                    this.user = UserDAO[user.id]
                    this.group = GroupDAO[groupId]
                    this.accepted = true
                }
            }
            val exception = runCatching {
                invitationService.makeUserInvitationForGroup(groupId, user.id)
            }
                .onFailure { e -> e }
                .exceptionOrNull()
            assertEquals(null, exception)
        }

    @Test
    fun `should not throw the UserAlreadyInvited exception when invitation was declined`() =
        runBlocking {
            val user = transactionHandler.fillUsers(1)[0]
            val groupId = transactionHandler.makeGroupAndGetId(user.id)
            transactionHandler.perform {
                InvitationDAO.new {
                    this.user = UserDAO[user.id]
                    this.group = GroupDAO[groupId]
                    this.accepted = false
                }
            }
            val exception = runCatching {
                invitationService.makeUserInvitationForGroup(groupId, user.id)
            }
                .onFailure { e -> e }
                .exceptionOrNull()
            assertEquals(null, exception)
        }

    @Test
    fun `should get only pending invitations`() =
        runBlocking {
            val users = transactionHandler.fillUsers(2)
            val owner = users[0]
            val invited = users[1]
            val groupId1 = transactionHandler.makeGroupAndGetId(owner.id)
            val groupId2 = transactionHandler.makeGroupAndGetId(owner.id)
            val groupId3 = transactionHandler.makeGroupAndGetId(owner.id)
            val declinedInvitation = transactionHandler.perform {
                InvitationDAO.new {
                    this.user = UserDAO[invited.id]
                    this.group = GroupDAO[groupId1]
                    this.accepted = false
                }
            }
            val pendingInvitation1 = transactionHandler.perform {
                InvitationDAO.new {
                    this.user = UserDAO[invited.id]
                    this.group = GroupDAO[groupId2]
                }
            }
            val pendingInvitation2 = transactionHandler.perform {
                InvitationDAO.new {
                    this.user = UserDAO[invited.id]
                    this.group = GroupDAO[groupId3]
                }
            }
            val expected = arrayOf(pendingInvitation1.id.value, pendingInvitation2.id.value).sorted()
            val result = invitationService.getInvitations(invited.id).map { it.id }.sorted()
            assertContentEquals(expected, result)
        }
}
