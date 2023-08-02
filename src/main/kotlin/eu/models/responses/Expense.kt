package eu.models.responses

import eu.models.responses.users.ExpenseUser
import eu.models.responses.users.toExpenseUser
import eu.tables.ExpenseDAO
import eu.tables.UserExpenseDAO
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: Int,
    val description: String,
    val totalAmount: Float,
    val participants: List<ExpenseUser>?,
)

fun ExpenseDAO.toExpense(users: List<UserExpenseDAO>?): Expense {
    return Expense(
        id.value,
        description,
        totalAmount,
        users?.map { it.toExpenseUser() },
    )
}
