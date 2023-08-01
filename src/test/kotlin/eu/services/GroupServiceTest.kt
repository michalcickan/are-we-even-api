package eu.services

import eu.helpers.MockTransactionHandler
import eu.helpers.fillUsers
import eu.helpers.makeGroupAndGetId
import eu.models.parameters.CreateGroupParameters
import eu.tables.*
import io.mockk.clearAllMocks
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.and
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals

class GroupServiceTest {
    private val transactionHandler = MockTransactionHandler()
    private lateinit var groupService: GroupService

    @Before
    fun setup() {
        transactionHandler.createTables(arrayOf(UsersGroups, Groups))
        groupService = GroupService(transactionHandler)
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
                    this.userId = UserDAO[firstUsers[0].id]
                    this.groupId = GroupDAO[groupId]
                }
            }
            secondUsers.forEach { user ->
                UserGroupDAO.new {
                    this.userId = UserDAO[user.id]
                    this.groupId = GroupDAO[thirdGroupId]
                }
            }
        }

        val result = groupService.getGroupsForUser(firstUsers[0].id).map { it.id }.sorted()
        val expected = listOf(firstGroupId, secondGroupId).sorted()
        assertContentEquals(expected, result)
    }

    @Test
    fun `getGroupsForUser should return empy list when user is not in any`() = runBlocking {
        val firstUsers = transactionHandler.fillUsers(2)
        val firstGroupId = transactionHandler.makeGroupAndGetId(firstUsers[0].id)
        val secondUsers = transactionHandler.fillUsers(3, 2)
        val secondGroupId = transactionHandler.makeGroupAndGetId(secondUsers[0].id)
        val thirdGroupId = transactionHandler.makeGroupAndGetId(secondUsers[0].id)
        transactionHandler.perform {
            listOf(firstGroupId, secondGroupId).forEachIndexed { index, groupId ->
                UserGroupDAO.new {
                    this.userId = UserDAO[firstUsers[0].id]
                    this.groupId = GroupDAO[groupId]
                }
            }
            secondUsers.forEach { user ->
                UserGroupDAO.new {
                    this.userId = UserDAO[user.id]
                    this.groupId = GroupDAO[thirdGroupId]
                }
            }
        }

        val result = groupService.getGroupsForUser(firstUsers[1].id).map { it.id }.sorted()
        assertContentEquals(emptyList(), result)
    }

    @Test
    fun `should add group to database`() = runBlocking {
        // 3.
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
        TestCase.assertNotNull(databaseGroup)
    }
}
