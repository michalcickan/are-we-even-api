package eu.models.responses

import eu.models.responses.users.ExpenditureUser
import eu.models.responses.users.toExpenditureUser
import eu.tables.ExpenditureDAO
import eu.tables.UserExpenditureDAO

data class Expenditure(
    val id: Int,
    val description: String,
    val totalAmount: Float,
    val participants: List<ExpenditureUser>?,
)

fun ExpenditureDAO.toExpenditure(users: List<UserExpenditureDAO>?): Expenditure {
    return Expenditure(
        id.value,
        description,
        totalAmount,
        users?.map { it.toExpenditureUser() },
    )
}
