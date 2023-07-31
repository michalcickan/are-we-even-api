package eu.services

import eu.helpers.MockTransactionHandler
import eu.models.parameters.AddExpenditureParameters
import eu.models.parameters.AddExpenditureParametersPayer
import eu.models.parameters.CreateGroupParameters
import eu.models.parameters.UpdateExpenditureParameters
import eu.models.responses.toExpenditure
import eu.models.responses.users.User
import eu.models.responses.users.toUser
import eu.tables.*
import io.mockk.clearAllMocks
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.and
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ExpenditureServiceTest {
    private val transactionManager = MockTransactionHandler()
    private lateinit var expenditureService: ExpenditureService

    @Before
    fun setup() {
        transactionManager.createTables(arrayOf(UserExpenditures, Users, Expenditures, Owees, Groups))
        expenditureService = ExpenditureService(transactionManager)
    }

    @After
    fun teardown() {
        transactionManager.databaseTearDown()
        clearAllMocks()
    }

    @Test
    fun `should insert correct total amount when adding expenditure`() = runBlocking {
        val users = transactionManager.fillUsers()
        val paidAmounts = listOf(20f, 30f, 40f)
        val dueAmounts = listOf(30f, 30f, 10f)
        val expenditure = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            transactionManager.makeGroup(users[0].id),
        )
        val databaseExpenditure = transactionManager.perform {
            ExpenditureDAO.find { Expenditures.id eq expenditure.id }.first()
        }.toExpenditure()
        assertEquals(databaseExpenditure.totalAmount, paidAmounts.sum())
    }

    @Test
    fun `should insert have all payers with correct amount in userexpenditures table`() = runBlocking {
        val users = transactionManager.fillUsers()
        val paidAmounts = listOf(20f, 30f, 40f)
        val dueAmounts = listOf(30f, 30f, 10f)
        val expenditure = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            transactionManager.makeGroup(users[0].id),
        )
        val databaseUserExpenditureIds = runCatching {
            transactionManager.perform {
                UserExpenditureDAO
                    .find { UserExpenditures.expenditureId eq expenditure.id }
                    .map { it.user.id.value }
            }
        }
            .onSuccess { e -> e }
            .getOrDefault(emptySet())
        val payersUserIds = users.map { it.id }
        assertContentEquals(payersUserIds, databaseUserExpenditureIds)
    }

    @Test
    fun `should insert only payers with amount above zero in userexpenditures table`() = runBlocking {
        val users = transactionManager.fillUsers()
        val paidAmounts = listOf(20f, 30f, 0f)
        val dueAmounts = listOf(10f, 30f, 10f)
        val groupId = transactionManager.makeGroup(users[0].id)
        val expenditure = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        )
        val databaseUserExpenditureIds = runCatching {
            transactionManager.perform {
                UserExpenditureDAO
                    .find { UserExpenditures.expenditureId eq expenditure.id }
                    .map { it.user.id.value }
            }
        }
            .onSuccess { e -> e }
            .getOrDefault(emptySet())
        val payersUserIds = users
            .filterIndexed { index, _ -> paidAmounts[index] > 0f }
            .map { it.id }

        assertContentEquals(payersUserIds, databaseUserExpenditureIds)
    }

    @Test
    fun `should correctly calculate debts when one is not in debt and other two makes a difference to settle`() =
        runBlocking {
            val users = transactionManager.fillUsers()
            val paidAmounts = listOf(20f, 30f, 40f)
            val dueAmounts = listOf(30f, 30f, 30f)
            var groupId = transactionManager.makeGroup(users[0].id)
            expenditureService.addExpenditure(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val firstOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[0].id)
            }
            val secondOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[1].id)
            }
            val thirdOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[2].id)
            }
            val expected = arrayOf(10f, 0f, 0f)
            val actual = arrayOf(firstOwesInDatabase, secondOwesInDatabase, thirdOwesInDatabase)
            assertContentEquals(
                expected,
                actual,
            )
        }

    @Test
    fun `should correctly calculate debts when first has greater paid amount than due and rest has lesser`() =
        runBlocking {
            val users = transactionManager.fillUsers()
            val paidAmounts = listOf(40f, 20f, 50f)
            val dueAmounts = listOf(20f, 30f, 60f)
            var groupId = transactionManager.makeGroup(users[0].id)
            expenditureService.addExpenditure(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val firstOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[0].id)
            }
            val secondOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[1].id)
            }
            val thirdOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[2].id)
            }
            val expected = arrayOf(0f, 10f, 10f)
            val actual = arrayOf(firstOwesInDatabase, secondOwesInDatabase, thirdOwesInDatabase)
            assertContentEquals(
                expected,
                actual,
            )
        }

    @Test
    fun `should not have anything in owee table when all have same paid amount as due amount`() = runBlocking {
        val users = transactionManager.fillUsers()
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(40f, 20f, 50f)
        var groupId = transactionManager.makeGroup(users[0].id)
        expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        )
        val firstOwesInDatabase = transactionManager.perform {
            OweeDAO.getUsersOweAmount(groupId, users[0].id)
        }
        val secondOwesInDatabase = transactionManager.perform {
            OweeDAO.getUsersOweAmount(groupId, users[1].id)
        }
        val thirdOwesInDatabase = transactionManager.perform {
            OweeDAO.getUsersOweAmount(groupId, users[2].id)
        }
        val expected = arrayOf(0f, 0f, 0f)
        val actual = arrayOf(firstOwesInDatabase, secondOwesInDatabase, thirdOwesInDatabase)
        assertContentEquals(
            expected,
            actual,
        )
    }

    @Test
    fun `should add group to database`() = runBlocking {
        // 3.
        val users = transactionManager.fillUsers()
        val name = "testing"
        expenditureService.createGroup(
            CreateGroupParameters(
                name,
            ),
            users[0].id,
        )
        val databaseGroup = transactionManager.perform {
            GroupDAO.find {
                Groups.name eq name and (Groups.createdBy eq users[0].id)
            }
                .first()
        }
        assertNotNull(databaseGroup)
    }

    @Test
    fun `should correctly update expenditure description`() = runBlocking {
        val users = transactionManager.fillUsers()
        val name = "testing"
        val group = transactionManager.perform {
            GroupDAO.new {
                this.name = name
                this.createdBy = UserDAO[users[0].id]
            }
        }
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 60f)
        val expenditure = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            group.id.value,
        )
        val expectedDescription = "updated_description"
        expenditureService.updateExpenditure(
            UpdateExpenditureParameters(
                expenditure.id,
                null,
                description = expectedDescription,
            ),
            group.id.value,
        )
        val resultExpenditure = transactionManager.perform {
            ExpenditureDAO[expenditure.id]
        }
        assertEquals(expectedDescription, resultExpenditure.description)
    }

    @Test
    fun `should correctly update total sum for expenditure`() = runBlocking {
        val users = transactionManager.fillUsers()
        val name = "testing"
        val group = transactionManager.perform {
            GroupDAO.new {
                this.name = name
                this.createdBy = UserDAO[users[0].id]
            }
        }
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 60f)
        val expenditure = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            group.id.value,
        )
        val updatedPaidAmounts = listOf(30f, 20f, 10f)
        val updatedDueAmounts = listOf(10f, 20f, 30f)

        expenditureService.updateExpenditure(
            UpdateExpenditureParameters(
                expenditure.id,
                users.mapIndexed { index, payer ->
                    AddExpenditureParametersPayer(
                        id = payer.id,
                        updatedPaidAmounts[index],
                        updatedDueAmounts[index],
                    )
                },
                null,
            ),
            group.id.value,
        )
        val resultExpenditure = transactionManager.perform {
            ExpenditureDAO[expenditure.id]
        }
        assertEquals(60f, resultExpenditure.totalAmount)
    }

    @Test
    fun `should correctly update payers sums when updating expenditure`() = runBlocking {
        val users = transactionManager.fillUsers()
        val name = "testing"
        val group = transactionManager.perform {
            GroupDAO.new {
                this.name = name
                this.createdBy = UserDAO[users[0].id]
            }
        }
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 60f)
        val expenditure = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            group.id.value,
        )
        val updatedPaidAmounts = listOf(30f, 20f, 10f)
        val updatedDueAmounts = listOf(10f, 20f, 30f)

        expenditureService.updateExpenditure(
            UpdateExpenditureParameters(
                expenditure.id,
                users.mapIndexed { index, payer ->
                    AddExpenditureParametersPayer(
                        id = payer.id,
                        updatedPaidAmounts[index],
                        updatedDueAmounts[index],
                    )
                },
                null,
            ),
            group.id.value,
        )
        transactionManager.perform {
            val resultExpenditure = UserExpenditureDAO
                .find { UserExpenditures.expenditureId eq expenditure.id }
            resultExpenditure.forEach { userExpenditure ->
                val userIndex = users.indexOfFirst { it.email == userExpenditure.user.email }
                assertEquals(
                    updatedPaidAmounts[userIndex],
                    userExpenditure.paidAmount,
                    "Updated paid amount is not correct for ${userExpenditure.user.email}",
                )
                assertEquals(
                    updatedDueAmounts[userIndex],
                    userExpenditure.dueAmount,
                    "Updated due amount is not correct for ${userExpenditure.user.email}",
                )
            }
        }
    }

    @Test
    fun `uneven should update owing amount after adding other expenditure when one is in debt and others are not`() =
        runBlocking {
            // 1.
            val users = transactionManager.fillUsers()
            val paidAmounts = listOf(40f, 20f, 50f)
            val dueAmounts = listOf(20f, 30f, 60f)
            var groupId = transactionManager.makeGroup(users[0].id)
            expenditureService.addExpenditure(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val nextPaidAmounts = listOf(30f, 20f, 30f)
            val nextDueAmounts = listOf(20f, 30f, 30f)
            expenditureService.addExpenditure(
                users.makeParams(nextPaidAmounts, nextDueAmounts),
                groupId,
            )
            val expected = arrayOf(0f, 20f, 10f)

            val firstOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[0].id)
            }
            val secondOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[1].id)
            }
            val thirdOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[2].id)
            }
            val actual = arrayOf(firstOwesInDatabase, secondOwesInDatabase, thirdOwesInDatabase)
            assertContentEquals(
                expected,
                actual,
            )
        }

    @Test
    fun `uneven should update owing amount after adding other expenditure when two are not in debt and one owes only to one`() =
        runBlocking {
            // 2.
            val users = transactionManager.fillUsers()
            val paidAmounts = listOf(40f, 20f, 50f)
            val dueAmounts = listOf(20f, 30f, 60f)
            var groupId = transactionManager.makeGroup(users[0].id)
            expenditureService.addExpenditure(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val nextPaidAmounts = listOf(50f, 20f, 30f)
            val nextDueAmounts = listOf(60f, 10f, 30f)
            expenditureService.addExpenditure(
                users.makeParams(nextPaidAmounts, nextDueAmounts),
                groupId,
            )
            val expected = arrayOf(0f, 0f, 10f)

            val firstOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[0].id)
            }
            val secondOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[1].id)
            }
            val thirdOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[2].id)
            }
            val actual = arrayOf(firstOwesInDatabase, secondOwesInDatabase, thirdOwesInDatabase)
            assertContentEquals(
                expected,
                actual,
            )
        }

    @Test
    fun `uneven should update owing amount after adding other expenditure when one is in debt to both`() =
        runBlocking {
            // 3.
            val users = transactionManager.fillUsers()
            val paidAmounts = listOf(40f, 20f, 50f)
            val dueAmounts = listOf(40f, 10f, 60f)
            var groupId = transactionManager.makeGroup(users[0].id)
            expenditureService.addExpenditure(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val nextPaidAmounts = listOf(40f, 20f, 50f)
            val nextDueAmounts = listOf(30f, 20f, 60f)
            expenditureService.addExpenditure(
                users.makeParams(nextPaidAmounts, nextDueAmounts),
                groupId,
            )
            val expected = arrayOf(0f, 0f, 20f)

            val firstOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[0].id)
            }
            val secondOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[1].id)
            }
            val thirdOwesInDatabase = transactionManager.perform {
                OweeDAO.getUsersOweAmount(groupId, users[2].id)
            }
            val actual = arrayOf(firstOwesInDatabase, secondOwesInDatabase, thirdOwesInDatabase)
            assertContentEquals(
                expected,
                actual,
            )
        }

    @Test
    fun `even should update owing amount after adding other expenditure when two are not in debt and one owes only to one`() =
        runBlocking {
            // 2.
            val users = transactionManager.fillUsers(4)
            val paidAmounts = listOf(40f, 20f, 50f, 40f)
            val dueAmounts = listOf(20f, 30f, 80f, 20f)
            var groupId = transactionManager.makeGroup(users[0].id)
            expenditureService.addExpenditure(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val nextPaidAmounts = listOf(50f, 20f, 30f, 50f)
            val nextDueAmounts = listOf(80f, 10f, 30f, 30f)
            expenditureService.addExpenditure(
                users.makeParams(nextPaidAmounts, nextDueAmounts),
                groupId,
            )
            val expected = arrayOf(10f, 0f, 30f, 0f).toFloatArray()
            val databaseValues = transactionManager.getUsersOwes(4, groupId, users)
            assertContentEquals(
                expected,
                databaseValues,
            )
        }

    @Test
    fun `even should update owing amount after adding other expenditure when one is in debt to both`() =
        runBlocking {
            // 3.
            val users = transactionManager.fillUsers(4)
            val paidAmounts = listOf(40f, 20f, 50f, 40f)
            val dueAmounts = listOf(40f, 10f, 80f, 20f)
            var groupId = transactionManager.makeGroup(users[0].id)
            expenditureService.addExpenditure(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val nextPaidAmounts = listOf(40f, 30f, 50f, 20f)
            val nextDueAmounts = listOf(30f, 20f, 60f, 30f)
            expenditureService.addExpenditure(
                users.makeParams(nextPaidAmounts, nextDueAmounts),
                groupId,
            )
            val expected = arrayOf(0f, 0f, 40f, 0f).toFloatArray()
            val databaseValues = transactionManager.getUsersOwes(4, groupId, users)
            assertContentEquals(
                expected,
                databaseValues,
            )
        }
}

private fun OweeDAO.Companion.getUsersOweAmount(groupId: Int, userId: Long): Float {
    return OweeDAO
        .find {
            (Owees.oweeUserId eq userId) and (Owees.groupId eq groupId)
        }
        .fold(0f) { acc, owee -> acc + owee.amountOwed }
        .toFloat()
}

private fun MutableList<User>.addUserDAO(users: List<String>) {
    users.forEach { email ->
        add(
            UserDAO.new {
                this.email = email
            }.toUser(),
        )
    }
}

private suspend fun MockTransactionHandler.getUsersOwes(count: Int, groupId: Int, users: List<User>): FloatArray {
    val databaseUsers = mutableListOf<Float>()
    perform {
        for (index in 0 until count) {
            databaseUsers.add(OweeDAO.getUsersOweAmount(groupId, users[index].id))
        }
    }
    return databaseUsers.toFloatArray()
}

private suspend fun MockTransactionHandler.fillUsers(count: Int = 3): List<User> {
    val payerUsers = mutableListOf<User>()

    perform {
        for (index in 0 until count) {
            val email = "something@test$index.com"
            val user = UserDAO.new {
                this.email = email
            }.toUser()
            payerUsers.add(user)
        }
    }

    return payerUsers
}

private suspend fun MockTransactionHandler.makeGroup(userId: Long): Int {
    return perform {
        GroupDAO.new {
            this.name = "test"
            this.createdBy = UserDAO[userId]
        }
    }.id.value
}

private fun List<User>.makeParams(paidAmounts: List<Float>, dueAmounts: List<Float>): AddExpenditureParameters {
    return AddExpenditureParameters(
        mapIndexed { index, payer ->
            AddExpenditureParametersPayer(
                id = payer.id,
                paidAmounts[index],
                dueAmounts[index],
            )
        },
        "test",
    )
}
