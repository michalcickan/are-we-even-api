package eu.services

import eu.exceptions.APIException
import eu.helpers.MockTransactionHandler
import eu.helpers.fillUsers
import eu.helpers.makeGroupAndGetId
import eu.models.parameters.CreateGroupParameters
import eu.models.responses.Group
import eu.models.responses.Invitation
import eu.tables.*
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.and
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GroupServiceTest {
    private val transactionHandler = MockTransactionHandler()
    private lateinit var groupService: GroupService
    private val mockInvitationService: IInvitationService = mockk()

    @Before
    fun setup() {
        transactionHandler.createTables(arrayOf(UsersGroups, Groups, Invitations))
        groupService = GroupService(transactionHandler, mockInvitationService)
    }

    @After
    fun tearDown() {
        transactionHandler.databaseTearDown()
        clearAllMocks()
    }

    @Test
    fun `getGroupsForUser should return only groups which the user is in`() = runBlocking {
        val firstUsers = transactionHandler.fillUsers(2)
        val firstGroupId = transactionHandler.makeGroupAndGetId(firstUsers[0].id)
        val secondUsers = transactionHandler.fillUsers(3, 2)
        val secondGroupId = transactionHandler.makeGroupAndGetId(secondUsers[0].id)
        val thirdGroupId = transactionHandler.makeGroupAndGetId(secondUsers[0].id)
        transactionHandler.perform {
            listOf(firstGroupId, secondGroupId).forEachIndexed { index, groupId ->
                UserGroupDAO.new {
                    this.user = UserDAO[firstUsers[0].id]
                    this.group = GroupDAO[groupId]
                    this.usersWorkingGroup = false
                }
            }
            secondUsers.forEach { user ->
                UserGroupDAO.new {
                    this.user = UserDAO[user.id]
                    this.group = GroupDAO[thirdGroupId]
                    this.usersWorkingGroup = false
                }
            }
        }

        val result = groupService.getGroupsForUser(firstUsers[0].id).map { it.id }.sorted()
        val expected = listOf(firstGroupId, secondGroupId).sorted()
        assertContentEquals(expected, result)
    }

    @Test
    fun `getGroupsForUser should return empty list when user is not in any`() = runBlocking {
        val firstUsers = transactionHandler.fillUsers(2)
        val firstGroupId = transactionHandler.makeGroupAndGetId(firstUsers[0].id)
        val secondUsers = transactionHandler.fillUsers(3, 2)
        val secondGroupId = transactionHandler.makeGroupAndGetId(secondUsers[0].id)
        val thirdGroupId = transactionHandler.makeGroupAndGetId(secondUsers[0].id)
        transactionHandler.perform {
            listOf(firstGroupId, secondGroupId).forEachIndexed { index, groupId ->
                UserGroupDAO.new {
                    this.user = UserDAO[firstUsers[0].id]
                    this.group = GroupDAO[groupId]
                    this.usersWorkingGroup = false
                }
            }
            secondUsers.forEach { user ->
                UserGroupDAO.new {
                    this.user = UserDAO[user.id]
                    this.group = GroupDAO[thirdGroupId]
                    this.usersWorkingGroup = false
                }
            }
        }

        val result = groupService.getGroupsForUser(firstUsers[1].id).map { it.id }.sorted()
        assertContentEquals(emptyList(), result)
    }

    @Test
    fun `should add group to database`() = runBlocking {
        val users = transactionHandler.fillUsers()
        val name = "testing"
        groupService.createGroup(
            CreateGroupParameters(
                name,
            ),
            users[0].id,
        )
        val databaseGroup = transactionHandler.perform {
            GroupDAO.find {
                Groups.name eq name and (Groups.createdBy eq users[0].id)
            }
                .first()
        }
        assertNotNull(databaseGroup)
    }

    @Test
    fun `should insert a user in to a group when creates its`() =
        runBlocking {
            val users = transactionHandler.fillUsers()
            val group = groupService.createGroup(
                CreateGroupParameters(
                    "test",
                ),
                users[0].id,
            )

            val databaseRecord = transactionHandler.perform {
                UserGroupDAO
                    .find {
                        (UsersGroups.groupId eq group.id) and (UsersGroups.userId eq users[0].id)
                    }
                    .toList()
            }
            assertNotEquals(emptyList(), databaseRecord)
        }

    @Test
    fun `should not insert a user in to a group and should call an adding invitation method`() =
        runBlocking {
            val users = transactionHandler.fillUsers(2)
            val groupId = transactionHandler.makeGroupAndGetId(users[0].id)
            val testingUser = users[1]
            coEvery { mockInvitationService.makeUserInvitationForGroup(any(), any()) } returns Invitation(
                1,
                testingUser,
                Group(groupId, "test", isDefault = false),
            )
            groupService.inviteUserToGroup(groupId, testingUser.id)

            val databaseRecord = transactionHandler.perform {
                UserGroupDAO
                    .find {
                        (UsersGroups.groupId eq groupId) and (UsersGroups.userId eq testingUser.id)
                    }
                    .toList()
            }
            assertEquals(emptyList(), databaseRecord)
            coVerify { mockInvitationService.makeUserInvitationForGroup(groupId, testingUser.id) }
        }

    @Test
    fun `handleInvitation should not add a user in to a group when user not accepted and called handleInvitation`() =
        runBlocking {
            val users = transactionHandler.fillUsers(2)
            val groupId = transactionHandler.makeGroupAndGetId(users[0].id)
            val testingUser = users[1].id
            val accepted = false
            val mockInvitationId = 2
            coEvery { mockInvitationService.handleInvitation(any(), any()) } returns Invitation(
                mockInvitationId,
                users[0],
                Group(groupId, "test", isDefault = false),
            )
            groupService.resolveInvitationToGroup(
                testingUser,
                mockInvitationId,
                accepted,
            )

            val databaseRecord = transactionHandler.perform {
                UserGroupDAO
                    .find {
                        (UsersGroups.groupId eq groupId) and (UsersGroups.userId eq testingUser)
                    }
                    .toList()
            }
            assertEquals(emptyList(), databaseRecord)
            coVerify { mockInvitationService.handleInvitation(mockInvitationId, accepted) }
        }

    @Test
    fun `handleInvitation should call handleInvitation from invitationService`() =
        runBlocking {
            val users = transactionHandler.fillUsers(2)
            val testingUser = users[1].id
            val accepted = false
            val mockInvitationId = 2
            coEvery { mockInvitationService.handleInvitation(any(), any()) } returns Invitation(
                mockInvitationId,
                users[0],
                Group(2, "test", isDefault = false),
            )
            groupService.resolveInvitationToGroup(
                testingUser,
                mockInvitationId,
                accepted,
            )
            coVerify { mockInvitationService.handleInvitation(mockInvitationId, accepted) }
        }

    @Test
    fun `handleInvitation should add a user in to a group when user accepted`() =
        runBlocking {
            val users = transactionHandler.fillUsers(2)
            val groupId = transactionHandler.makeGroupAndGetId(users[0].id)
            val testingUser = users[1].id
            val accepted = true
            coEvery { mockInvitationService.handleInvitation(any(), any()) } returns Invitation(
                2,
                users[0],
                Group(groupId, "test", isDefault = false),
            )
            groupService.resolveInvitationToGroup(
                testingUser,
                2,
                accepted,
            )

            val databaseRecord = transactionHandler.perform {
                UserGroupDAO
                    .find {
                        (UsersGroups.groupId eq groupId) and (UsersGroups.userId eq testingUser)
                    }
                    .toList()
            }
            assertNotEquals(emptyList(), databaseRecord)
        }

    @Test
    fun `should throw the UserAlreadyInGroup exception when adding user to a group where he is already in`() =
        runBlocking {
            val users = transactionHandler.fillUsers(1)
            val testingUserId = users[0].id
            val name = "testing"
            val group = groupService.createGroup(
                CreateGroupParameters(
                    name,
                ),
                testingUserId,
            )
            val result = runCatching {
                groupService.inviteUserToGroup(group.id, testingUserId)
            }
                .onFailure { e -> e }
                .exceptionOrNull()
            assertEquals(result, APIException.UserAlreadyInGroup)
        }
}
