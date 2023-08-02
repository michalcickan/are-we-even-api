package eu.models.responses.users

import eu.tables.UserExpenseDAO
import kotlinx.serialization.Serializable

@Serializable
class ExpenseUser(
    val id: Long,
    val name: String?,
    val paidAmount: Float,
    val dueAmount: Float,
)

fun UserExpenseDAO.toExpenseUser(): ExpenseUser {
    return ExpenseUser(
        user.id.value,
        user.name,
        paidAmount,
        dueAmount,
    )
}
