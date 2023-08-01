package eu.models.responses.users

import eu.tables.UserExpenditureDAO
import kotlinx.serialization.Serializable

@Serializable
class ExpenditureUser(
    val id: Long,
    val name: String?,
    val paidAmount: Float,
    val dueAmount: Float,
)

fun UserExpenditureDAO.toExpenditureUser(): ExpenditureUser {
    return ExpenditureUser(
        user.id.value,
        user.name,
        paidAmount,
        dueAmount,
    )
}
