package eu.services

import eu.models.parameters.AllExpensesQueryParameters
import eu.models.parameters.Sort
import eu.models.parameters.expense.AddExpenseParameters
import eu.models.parameters.expense.ExpensePayerParameters
import eu.models.parameters.expense.UpdateExpenseParameters
import eu.models.responses.Expense
import eu.models.responses.PagedData
import eu.models.responses.PagingMeta
import eu.models.responses.toExpense
import eu.modules.ITransactionHandler
import eu.tables.*
import eu.utils.ExpenseUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

interface IExpenseService {
    suspend fun getExpense(id: Int): Expense
    suspend fun getAllExpenses(groupId: Int, queryParameters: AllExpensesQueryParameters): PagedData<Expense>
    suspend fun addExpense(params: AddExpenseParameters, groupId: Int): Expense
    suspend fun updateExpense(
        params: UpdateExpenseParameters,
        expenseId: Int,
    ): Expense
}

class ExpenseService(
    private val transactionHandler: ITransactionHandler,
) : IExpenseService {
    override suspend fun getExpense(id: Int): Expense {
        return transactionHandler.perform {
            ExpenseDAO[id].getUsersAndMakeExpense()
//            val usersInGroup = UserGroupDAO.find { UsersGroups.groupId eq expense.id }
        }
    }

    override suspend fun getAllExpenses(
        groupId: Int,
        queryParameters: AllExpensesQueryParameters,
    ): PagedData<Expense> {
        return transactionHandler
            .perform {
                val sort = queryParameters.sort ?: Sort.DESC
                val expenses = ExpenseDAO
                    .find { Expenses.groupId eq groupId }
                val orderedExpensesQuery = when (sort) {
                    Sort.ASC -> expenses.orderBy(Expenses.createdAt to SortOrder.ASC)
                    Sort.DESC -> expenses.orderBy(Expenses.createdAt to SortOrder.DESC)
                }
                val offset = queryParameters.offset ?: 0
                val expenseModels = if (queryParameters.limit != null || offset > 0) {
                    orderedExpensesQuery.limit(queryParameters.limit!!, offset)
                } else {
                    orderedExpensesQuery
                }.map { it.toExpense(null) }

                PagedData(
                    expenseModels,
                    PagingMeta(expenses.count(), offset),
                )
            }
    }

    override suspend fun addExpense(params: AddExpenseParameters, groupId: Int): Expense {
        return transactionHandler.perform {
            val expense = ExpenseDAO.new {
                this.description = params.description
                this.totalAmount = ExpenseUtils.getTotalPaidAmount(params.users).toFloat()
                this.groupId = GroupDAO[groupId]
            }
            /*
            Note: This function must preceed  fillUserExpenseTable, because otherwise it can spoil calculations
            for the debt. App cannot rely upon the fact, that the new records should not be selectable till
            end of the transaction. When this is after the fillUserExpenseTable function, sometimes it gets
            from database a data including current transaction UserExpense data, sometimes not, so the calculation
            was not reliable.
             */
            fillOrUpdateDebtorTable(params.users, groupId)
            fillUserExpenseTable(params.users, expense)
            expense.getUsersAndMakeExpense()
        }
    }

    override suspend fun updateExpense(
        params: UpdateExpenseParameters,
        expenseId: Int,
    ): Expense {
        return transactionHandler.perform {
            val expense = ExpenseDAO[expenseId]
            if (params.description != null) {
                expense.description = params.description
            }
            if (params.users != null) {
                expense.totalAmount = ExpenseUtils.getTotalPaidAmount(params.users).toFloat()
                updateUserExpenseTable(params.users, expense)
            }
            expense.getUsersAndMakeExpense()
        }
    }

    private fun fillUserExpenseTable(users: List<ExpensePayerParameters>, expense: ExpenseDAO) {
        users.forEach { user ->
            if (user.paidAmount > 0) {
                UserExpenseDAO.new {
                    this.expenseID = expense
                    this.paidAmount = user.paidAmount
                    this.dueAmount = user.dueAmount
                    this.user = UserDAO[user.id]
                }
            }
        }
    }

    private fun updateUserExpenseTable(users: List<ExpensePayerParameters>, expense: ExpenseDAO) {
        users.forEach { user ->
            try {
                val userExpense = UserExpenseDAO
                    .find {
                        (UsersExpenses.userId eq user.id) and (UsersExpenses.expenseId eq expense.id)
                    }
                    .first()
                userExpense.paidAmount = user.paidAmount
                userExpense.dueAmount = user.dueAmount
            } catch (e: Exception) {
                print(e)
            }
        }
    }

    private fun fillOrUpdateDebtorTable(users: List<ExpensePayerParameters>, groupId: Int) {
        val (higherPayers, lesserPayers) = categorizeUsersByPayment(users, groupId)
        val remainingHigherPayers = higherPayers.toMutableList()

        clearExistingDebtors(groupId)

        lesserPayers.forEach { debtor ->
            var diff = debtor.dueAmount - debtor.paidAmount
            if (diff == 0f) return@forEach
            var amountToWrite = 0f
            do {
                var userToUseToEven = remainingHigherPayers[0]
                val higherDiff = userToUseToEven.paidAmount - userToUseToEven.dueAmount
                if (higherDiff >= diff) {
                    amountToWrite = diff
                    val index = remainingHigherPayers.indexOf(userToUseToEven)
                    remainingHigherPayers[index] = userToUseToEven.copy(paidAmount = userToUseToEven.paidAmount - diff)
                } else {
                    amountToWrite = diff - higherDiff
                    // we depleted users all resources, so the user is on the same level with due amount, and we cannot use it anymore
                    remainingHigherPayers.remove(userToUseToEven)
                }
                DebtorDAO.new {
                    this.groupId = GroupDAO[groupId]
                    this.debtor = UserDAO[debtor.id]
                    this.creditor = UserDAO[userToUseToEven.id]
                    this.amountOwed = amountToWrite
                }
                diff -= amountToWrite
            } while (diff > 0)
        }
    }

    private fun clearExistingDebtors(groupId: Int) {
        DebtorDAO.find { Debtors.groupId eq groupId }.forEach { it.delete() }
    }

    private fun categorizeUsersByPayment(
        users: List<ExpensePayerParameters>,
        groupId: Int,
    ): Pair<List<ExpensePayerParameters>, List<ExpensePayerParameters>> {
        return users
            .map {
                val priorExpenses = getAllUserExpensesWithGroupId(groupId, it.id)
                it.copy(
                    paidAmount = it.paidAmount + priorExpenses.paidAmount,
                    dueAmount = it.dueAmount + priorExpenses.dueAmount,
                )
            }
            .partition { user ->
                user.paidAmount > user.dueAmount
            }
    }

    private fun getAllUserExpensesWithGroupId(groupId: Int, userId: Long): _UserExpense {
        // Join the UserExpenses and Expenses tables based on the group ID
        try {
            val userExpenses = (UsersExpenses innerJoin Expenses)
                .slice(UsersExpenses.paidAmount, UsersExpenses.dueAmount)
                .select {
                    (Expenses.groupId eq groupId) and (UsersExpenses.userId eq userId)
                }
                .map { _UserExpense(it[UsersExpenses.paidAmount], it[UsersExpenses.dueAmount]) }

            val (paidAmountSum, dueAmountSum) = userExpenses.fold(0.0f to 0.0f) { acc, expense ->
                acc.first + expense.paidAmount to acc.second + expense.dueAmount
            }

            return _UserExpense(paidAmountSum, dueAmountSum)
        } catch (e: Exception) {
            return _UserExpense(0f, 0f)
        }
    }
}

private data class _UserExpense(
    val paidAmount: Float,
    val dueAmount: Float,
)

private fun ExpenseDAO.getUsersAndMakeExpense(): Expense {
    val users = UserExpenseDAO
        .find { UsersExpenses.expenseId eq id }
        .toList()
    return toExpense(users)
}
