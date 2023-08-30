package eu.services

import eu.helpers.*
import eu.models.parameters.AllExpensesQueryParameters
import eu.models.parameters.expense.ExpensePayerParameters
import eu.models.parameters.expense.UpdateExpenseParameters
import eu.models.responses.toExpense
import eu.tables.*
import io.mockk.clearAllMocks
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ExpenseServiceForCRUDTest {
    private val transactionHandler = MockTransactionHandler()
    private lateinit var expenseService: ExpenseService

    @Before
    fun setup() {
        transactionHandler.createTables(arrayOf(UsersExpenses, Users, Expenses, Debtors, Groups))
        expenseService = ExpenseService(transactionHandler)
    }

    @After
    fun teardown() {
        transactionHandler.databaseTearDown()
        clearAllMocks()
    }

    @Test
    fun `should insert correct total amount when adding expense`() = runBlocking {
        val users = transactionHandler.fillUsers()
        val paidAmounts = listOf(20f, 30f, 40f)
        val dueAmounts = listOf(30f, 30f, 10f)
        val expense = expenseService.addExpense(
            users.makeParams(paidAmounts, dueAmounts),
            transactionHandler.makeGroupAndGetId(users[0].id),
        )
        val databaseExpense = transactionHandler.perform {
            ExpenseDAO.find { Expenses.id eq expense.id }.first()
        }.toExpense(null)
        assertEquals(databaseExpense.totalAmount, paidAmounts.sum())
    }

    @Test
    fun `should insert have all payers with correct amount in userexpenses table`() = runBlocking {
        val users = transactionHandler.fillUsers()
        val paidAmounts = listOf(20f, 30f, 40f)
        val dueAmounts = listOf(30f, 30f, 10f)
        val expense = expenseService.addExpense(
            users.makeParams(paidAmounts, dueAmounts),
            transactionHandler.makeGroupAndGetId(users[0].id),
        )
        val databaseUserExpenseIds = runCatching {
            transactionHandler.perform {
                UserExpenseDAO
                    .find { UsersExpenses.expenseId eq expense.id }
                    .map { it.user.id.value }
            }
        }
            .onSuccess { e -> e }
            .getOrDefault(emptySet())
        val payersUserIds = users.map { it.id }
        assertContentEquals(payersUserIds, databaseUserExpenseIds)
    }

    @Test
    fun `should insert only payers with amount above zero in userexpenses table`() = runBlocking {
        val users = transactionHandler.fillUsers()
        val paidAmounts = listOf(20f, 30f, 0f)
        val dueAmounts = listOf(10f, 30f, 10f)
        val groupId = transactionHandler.makeGroupAndGetId(users[0].id)
        val expense = expenseService.addExpense(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        )
        val databaseUserExpenseIds = runCatching {
            transactionHandler.perform {
                UserExpenseDAO
                    .find { UsersExpenses.expenseId eq expense.id }
                    .map { it.user.id.value }
            }
        }
            .onSuccess { e -> e }
            .getOrDefault(emptySet())
        val payersUserIds = users
            .filterIndexed { index, _ -> paidAmounts[index] > 0f }
            .map { it.id }

        assertContentEquals(payersUserIds, databaseUserExpenseIds)
    }

    @Test
    fun `no body should owe when all have same paid amount as due amount`() = runBlocking {
        val users = transactionHandler.fillUsers()
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(40f, 20f, 50f)
        var groupId = transactionHandler.makeGroupAndGetId(users[0].id)
        expenseService.addExpense(
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
    fun `should correctly update expense description`() = runBlocking {
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
        val expense = expenseService.addExpense(
            users.makeParams(paidAmounts, dueAmounts),
            group.id.value,
        )
        val expectedDescription = "updated_description"
        expenseService.updateExpense(
            UpdateExpenseParameters(
                null,
                description = expectedDescription,
            ),
            expense.id,
        )
        val resultExpense = transactionHandler.perform {
            ExpenseDAO[expense.id]
        }
        assertEquals(expectedDescription, resultExpense.description)
    }

    @Test
    fun `should correctly update total sum for expense`() = runBlocking {
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
        val expense = expenseService.addExpense(
            users.makeParams(paidAmounts, dueAmounts),
            group.id.value,
        )
        val updatedPaidAmounts = listOf(30f, 20f, 10f)
        val updatedDueAmounts = listOf(10f, 20f, 30f)

        expenseService.updateExpense(
            UpdateExpenseParameters(
                users.mapIndexed { index, payer ->
                    ExpensePayerParameters(
                        id = payer.id,
                        updatedPaidAmounts[index],
                        updatedDueAmounts[index],
                    )
                },
                null,
            ),
            expenseId = expense.id,
        )
        val resultExpense = transactionHandler.perform {
            ExpenseDAO[expense.id]
        }
        assertEquals(60f, resultExpense.totalAmount)
    }

    @Test
    fun `should correctly update payers sums when updating expense`() = runBlocking {
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
        val expense = expenseService.addExpense(
            users.makeParams(paidAmounts, dueAmounts),
            group.id.value,
        )
        val updatedPaidAmounts = listOf(30f, 20f, 10f)
        val updatedDueAmounts = listOf(10f, 20f, 30f)

        expenseService.updateExpense(
            UpdateExpenseParameters(
                users.mapIndexed { index, payer ->
                    ExpensePayerParameters(
                        id = payer.id,
                        updatedPaidAmounts[index],
                        updatedDueAmounts[index],
                    )
                },
                null,
            ),
            expenseId = expense.id,
        )
        transactionHandler.perform {
            val resultExpense = UserExpenseDAO
                .find { UsersExpenses.expenseId eq expense.id }
            resultExpense.forEach { userExpense ->
                val userIndex = users.indexOfFirst { it.email == userExpense.user.email }
                assertEquals(
                    updatedPaidAmounts[userIndex],
                    userExpense.paidAmount,
                    "Updated paid amount is not correct for ${userExpense.user.email}",
                )
                assertEquals(
                    updatedDueAmounts[userIndex],
                    userExpense.dueAmount,
                    "Updated due amount is not correct for ${userExpense.user.email}",
                )
            }
        }
    }

    @Test
    fun `should get expense users when requesting for detail`() = runBlocking {
        val users = transactionHandler.fillUsers(3)
        val groupId = transactionHandler.makeGroupAndGetId(users[0].id)
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 60f)
        val expense = expenseService.addExpense(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        )

        val result = expenseService.getExpense(expense.id)
        assertEquals(result.participants?.size, 3)
    }

    @Test
    fun `should get expense users with appropriate amounts when requesting for detail`() = runBlocking {
        val users = transactionHandler.fillUsers(3)
        val groupId = transactionHandler.makeGroupAndGetId(users[0].id)
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 60f)
        val expense = expenseService.addExpense(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        )

        val result = expenseService.getExpense(expense.id)
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
    fun `should get only expenses in given group`() = runBlocking {
        val users = transactionHandler.fillUsers(3)
        val groupId = transactionHandler.makeGroupAndGetId(users[0].id)
        val nextGroupId = transactionHandler.makeGroupAndGetId(users[0].id)
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 60f)
        val first = expenseService.addExpense(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        ).id
        val second = expenseService.addExpense(
            users.makeParams(paidAmounts, dueAmounts),
            groupId,
        ).id
        val third = expenseService.addExpense(
            users.makeParams(paidAmounts, dueAmounts),
            nextGroupId,
        ).id
        val expected = listOf(first, second).sorted()
        val expenses = expenseService
            .getAllExpenses(groupId, AllExpensesQueryParameters(0, 50, null))
            .data.map { it.id }
            .sorted()
        assertEquals(expected, expenses)
        assertFalse(expenses.contains(third))
    }
}
