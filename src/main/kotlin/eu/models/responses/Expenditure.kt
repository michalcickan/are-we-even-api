package eu.models.responses

import eu.models.responses.users.OweeUser
import eu.models.responses.users.PayerUser
import eu.tables.ExpenditureDAO

data class Expenditure(
    val id: Int,
    val description: String,
    val totalAmount: Float,
    val payers: List<PayerUser>,
    val owees: List<OweeUser>,
)

fun ExpenditureDAO.toExpenditure(): Expenditure {
    return Expenditure(
        id.value,
        description,
        totalAmount,
        emptyList(),
        emptyList(),
    )
}
