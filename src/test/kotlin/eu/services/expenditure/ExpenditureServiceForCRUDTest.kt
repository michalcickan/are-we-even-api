package eu.services

import eu.helpers.*
import eu.models.parameters.AddExpenditureParametersPayer
import eu.models.parameters.UpdateExpenditureParameters
import eu.models.responses.toExpenditure
import eu.tables.*
import io.mockk.clearAllMocks
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ExpenditureServiceForCRUDTest {
    private val transactionHandler = MockTransactionHandler()
    private lateinit var expenditureService: ExpenditureService

    @Before
    fun setup() {
        transactionHandler.createTables(arrayOf(UserExpenditures, Users, Expenditures, Owees, Groups))
        expenditureService = ExpenditureService(transactionHandler)
    }

    @After
    fun teardown() {
        transactionHandler.databaseTearDown()
        clearAllMocks()
    }

    @Test
    fun `should insert correct total amount when adding expenditure`() = runBlocking {
        val users = transactionHandler.fillUsers()
        val paidAmounts = listOf(20f, 30f, 40f)
        val dueAmounts = listOf(30f, 30f, 10f)
        val expenditure = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            transactionHandler.makeGroupAndGetId(users[0].id),
        )
        val databaseExpenditure = transactionHandler.perform {
            ExpenditureDAO.find { Expenditures.id eq expenditure.id }.first()
        }.toExpenditure(null)
        assertEquals(databaseExpenditure.totalAmount, paidAmounts.sum())
    }

    @Test
    fun `should insert have all payers with correct amount in userexpenditures table`() = runBlocking {
        val users = transactionHandler.fillUsers()
        val paidAmounts = listOf(20f, 30f, 40f)
        val dueAmounts = listOf(30f, 30f, 10f)
        val expenditure = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            transactionHandler.makeGroupAndGetId(users[0].id),
        )
        val databaseUserExpenditureIds = runCatching {
            transactionHandler.perform {
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
        val users = transactionHandler.fillUsers()
        val paidAmounts = listOf(20f, 30f, 0f)
        val dueAmounts = listOf(10f, 30f, 10f)
        val groupId = transactionHandler.makeGroupAndGetId(users[0].id)
        val expenditure = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        )
        val databaseUserExpenditureIds = runCatching {
            transactionHandler.perform {
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
    fun `no body should owe when all have same paid amount as due amount`() = runBlocking {
        val users = transactionHandler.fillUsers()
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(40f, 20f, 50f)
        var groupId = transactionHandler.makeGroupAndGetId(users[0].id)
        expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        )
        val expected = arrayOf(0f, 0f, 0f)
        val databaseValues = transactionHandler.getUsersOwes(3, groupId, users)
        assertContentEquals(
            expected.toFloatArray(),
            databaseValues,
        )
    }

    @Test
    fun `should correctly update expenditure description`() = runBlocking {
        val users = transactionHandler.fillUsers()
        val name = "testing"
        val group = transactionHandler.perform {
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
                null,
                description = expectedDescription,
            ),
            group.id.value,
            expenditureId = expenditure.id,
        )
        val resultExpenditure = transactionHandler.perform {
            ExpenditureDAO[expenditure.id]
        }
        assertEquals(expectedDescription, resultExpenditure.description)
    }

    @Test
    fun `should correctly update total sum for expenditure`() = runBlocking {
        val users = transactionHandler.fillUsers()
        val name = "testing"
        val group = transactionHandler.perform {
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
            expenditureId = expenditure.id,
        )
        val resultExpenditure = transactionHandler.perform {
            ExpenditureDAO[expenditure.id]
        }
        assertEquals(60f, resultExpenditure.totalAmount)
    }

    @Test
    fun `should correctly update payers sums when updating expenditure`() = runBlocking {
        val users = transactionHandler.fillUsers()
        val name = "testing"
        val group = transactionHandler.perform {
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
            expenditureId = expenditure.id,
        )
        transactionHandler.perform {
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
    fun `should get expenditure users when requesting for detail`() = runBlocking {
        val users = transactionHandler.fillUsers(3)
        val groupId = transactionHandler.makeGroupAndGetId(users[0].id)
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 60f)
        val expenditure = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        )

        val result = expenditureService.getExpenditure(expenditure.id)
        assertEquals(result.participants?.size, 3)
    }

    @Test
    fun `should get expenditure users with appropriate amounts when requesting for detail`() = runBlocking {
        val users = transactionHandler.fillUsers(3)
        val groupId = transactionHandler.makeGroupAndGetId(users[0].id)
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 60f)
        val expenditure = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        )

        val result = expenditureService.getExpenditure(expenditure.id)
        var actualPaidAmounts = mutableListOf(0f, 0f, 0f)
        var actualDueAmounts = mutableListOf(0f, 0f, 0f)
        for (user in result.participants!!) {
            var index = users.indexOfFirst { it.id == user.id }
            actualPaidAmounts[index] = paidAmounts[index]
            actualDueAmounts[index] = dueAmounts[index]
        }
        assertContentEquals(paidAmounts, actualPaidAmounts)
        assertContentEquals(dueAmounts, actualDueAmounts)
    }

    @Test
    fun `should get only expenditures in given group`() = runBlocking {
        val users = transactionHandler.fillUsers(3)
        val groupId = transactionHandler.makeGroupAndGetId(users[0].id)
        val nextGroupId = transactionHandler.makeGroupAndGetId(users[0].id)
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 60f)
        val first = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        ).id
        val second = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        ).id
        val third = expenditureService.addExpenditure(
            users.makeParams(paidAmounts, dueAmounts),
            nextGroupId,
        ).id
        val expected = listOf(first, second).sorted()
        val expenditures = expenditureService.getAllExpenditures(groupId).map { it.id }.sorted()
        assertEquals(expected, expenditures)
        assertFalse(expenditures.contains(third))
    }
}
