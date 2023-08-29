package eu.models.responses.users

import eu.tables.UserExpenseDAO
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class ExpenseUser(
    val id: Long,
    val name: String?,
    val email: String,
    val paidAmount: Float,
    val dueAmount: Float,
)

fun UserExpenseDAO.toExpenseUser(): ExpenseUser {
    return ExpenseUser(
        user.id.value,
        user.name,
        user.email,
        paidAmount,
        dueAmount,
    )
}
