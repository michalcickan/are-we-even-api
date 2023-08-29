package eu.models.responses

import eu.models.responses.users.User
import eu.models.responses.users.toSimpleUser
import eu.tables.DebtorDAO
import kotlinx.serialization.Serializable

@Serializable
class Debt(
    val id: Int,
    val debtor: User,
    val creditor: User,
    val amountOwed: Double,
)

fun DebtorDAO.toDebt(): Debt {
    return Debt(
        id.value,
        debtor.toSimpleUser(),
        creditor.toSimpleUser(),
        amountOwed.toDouble(),
    )
}
