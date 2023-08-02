package eu.services.expense

import eu.helpers.*
import eu.services.ExpenseService
import eu.tables.*
import io.mockk.clearAllMocks
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals

class ExpenseServiceForDebtComputationTest {
    private val transactionHandler = MockTransactionHandler()
    private lateinit var expenseService: ExpenseService

    @Before
    fun setup() {
        transactionHandler.createTables(arrayOf(UsersExpenses, Users, Expenses, Owees, Groups))
        expenseService = ExpenseService(transactionHandler)
    }

    @After
    fun teardown() {
        transactionHandler.databaseTearDown()
        clearAllMocks()
    }

    @Test
    fun `should correctly calculate debts when one is not in debt and other two makes a difference to settle`() =
        runBlocking {
            val users = transactionHandler.fillUsers()
            val paidAmounts = listOf(20f, 30f, 40f)
            val dueAmounts = listOf(30f, 30f, 30f)
            var groupId = transactionHandler.makeGroupAndGetId(users[0].id)
            expenseService.addExpense(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val expected = arrayOf(10f, 0f, 0f)
            val databaseValues = transactionHandler.getUsersOwes(3, groupId, users)
            assertContentEquals(
                expected.toFloatArray(),
                databaseValues,
            )
        }

    @Test
    fun `should correctly calculate debts when first has greater paid amount than due and rest has lesser`() =
        runBlocking {
            val users = transactionHandler.fillUsers()
            val paidAmounts = listOf(40f, 20f, 50f)
            val dueAmounts = listOf(20f, 30f, 60f)
            var groupId = transactionHandler.makeGroupAndGetId(users[0].id)
            expenseService.addExpense(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val expected = arrayOf(0f, 10f, 10f)
            val databaseValues = transactionHandler.getUsersOwes(3, groupId, users)
            assertContentEquals(
                expected.toFloatArray(),
                databaseValues,
            )
        }

    @Test
    fun `uneven should update owing amount after adding other expense when one is in debt and others are not`() =
        runBlocking {
            // 1.
            val users = transactionHandler.fillUsers()
            val paidAmounts = listOf(40f, 20f, 50f)
            val dueAmounts = listOf(20f, 30f, 60f)
            var groupId = transactionHandler.makeGroupAndGetId(users[0].id)
            expenseService.addExpense(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val nextPaidAmounts = listOf(30f, 20f, 30f)
            val nextDueAmounts = listOf(20f, 30f, 30f)
            expenseService.addExpense(
                users.makeParams(nextPaidAmounts, nextDueAmounts),
                groupId,
            )
            val expected = arrayOf(0f, 20f, 10f)

            val databaseValues = transactionHandler.getUsersOwes(3, groupId, users)
            assertContentEquals(
                expected.toFloatArray(),
                databaseValues,
            )
        }

    @Test
    fun `uneven should update owing amount after adding other expense when two are not in debt and one owes only to one`() =
        runBlocking {
            // 2.
            val users = transactionHandler.fillUsers()
            val paidAmounts = listOf(40f, 20f, 50f)
            val dueAmounts = listOf(20f, 30f, 60f)
            var groupId = transactionHandler.makeGroupAndGetId(users[0].id)
            expenseService.addExpense(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val nextPaidAmounts = listOf(50f, 20f, 30f)
            val nextDueAmounts = listOf(60f, 10f, 30f)
            expenseService.addExpense(
                users.makeParams(nextPaidAmounts, nextDueAmounts),
                groupId,
            )
            val expected = arrayOf(0f, 0f, 10f)

            val databaseValues = transactionHandler.getUsersOwes(3, groupId, users)
            assertContentEquals(
                expected.toFloatArray(),
                databaseValues,
            )
        }

    @Test
    fun `uneven should update owing amount after adding other expense when one is in debt to both`() =
        runBlocking {
            // 3.
            val users = transactionHandler.fillUsers()
            val paidAmounts = listOf(40f, 20f, 50f)
            val dueAmounts = listOf(40f, 10f, 60f)
            var groupId = transactionHandler.makeGroupAndGetId(users[0].id)
            expenseService.addExpense(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val nextPaidAmounts = listOf(40f, 20f, 50f)
            val nextDueAmounts = listOf(30f, 20f, 60f)
            expenseService.addExpense(
                users.makeParams(nextPaidAmounts, nextDueAmounts),
                groupId,
            )
            val expected = arrayOf(0f, 0f, 20f)
            val databaseValues = transactionHandler.getUsersOwes(3, groupId, users)
            assertContentEquals(
                expected.toFloatArray(),
                databaseValues,
            )
        }

    @Test
    fun `even should update owing amount after adding other expense when two are not in debt and one owes only to one`() =
        runBlocking {
            // 2.
            val users = transactionHandler.fillUsers(4)
            val paidAmounts = listOf(40f, 20f, 50f, 40f)
            val dueAmounts = listOf(20f, 30f, 80f, 20f)
            var groupId = transactionHandler.makeGroupAndGetId(users[0].id)
            expenseService.addExpense(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val nextPaidAmounts = listOf(50f, 20f, 30f, 50f)
            val nextDueAmounts = listOf(80f, 10f, 30f, 30f)
            expenseService.addExpense(
                users.makeParams(nextPaidAmounts, nextDueAmounts),
                groupId,
            )
            val expected = arrayOf(10f, 0f, 30f, 0f).toFloatArray()
            val databaseValues = transactionHandler.getUsersOwes(4, groupId, users)
            assertContentEquals(
                expected,
                databaseValues,
            )
        }

    @Test
    fun `even should update owing amount after adding other expense when one is in debt to both`() =
        runBlocking {
            // 3.
            val users = transactionHandler.fillUsers(4)
            val paidAmounts = listOf(40f, 20f, 50f, 40f)
            val dueAmounts = listOf(40f, 10f, 80f, 20f)
            var groupId = transactionHandler.makeGroupAndGetId(users[0].id)
            expenseService.addExpense(
                users.makeParams(paidAmounts, dueAmounts),
                groupId,
            )
            val nextPaidAmounts = listOf(40f, 30f, 50f, 20f)
            val nextDueAmounts = listOf(30f, 20f, 60f, 30f)
            expenseService.addExpense(
                users.makeParams(nextPaidAmounts, nextDueAmounts),
                groupId,
            )
            val expected = arrayOf(0f, 0f, 40f, 0f).toFloatArray()
            val databaseValues = transactionHandler.getUsersOwes(4, groupId, users)
            assertContentEquals(
                expected,
                databaseValues,
            )
        }
}
