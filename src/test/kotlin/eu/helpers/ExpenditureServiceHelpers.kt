package eu.helpers

import eu.models.parameters.expense.AddExpenseParameters
import eu.models.parameters.expense.ExpensePayerParameters
import eu.models.responses.users.User
import eu.models.responses.users.toUser
import eu.tables.GroupDAO
import eu.tables.OweeDAO
import eu.tables.Owees
import eu.tables.UserDAO
import org.jetbrains.exposed.sql.and

suspend fun MockTransactionHandler.getUsersOwes(count: Int, groupId: Int, users: List<User>): FloatArray {
    val databaseUsers = mutableListOf<Float>()
    perform {
        for (index in 0 until count) {
            databaseUsers.add(OweeDAO.getUsersOweAmount(groupId, users[index].id))
        }
    }
    return databaseUsers.toFloatArray()
}

suspend fun MockTransactionHandler.makeGroupAndGetId(userId: Long): Int {
    return perform {
        GroupDAO.new {
            this.name = "test"
            this.createdBy = UserDAO[userId]
        }
    }.id.value
}

fun List<User>.makeParams(paidAmounts: List<Float>, dueAmounts: List<Float>): AddExpenseParameters {
    return AddExpenseParameters(
        mapIndexed { index, payer ->
            ExpensePayerParameters(
                id = payer.id,
                paidAmounts[index],
                dueAmounts[index],
            )
        },
        "test",
    )
}

fun OweeDAO.Companion.getUsersOweAmount(groupId: Int, userId: Long): Float {
    return OweeDAO
        .find {
            (Owees.oweeUserId eq userId) and (Owees.groupId eq groupId)
        }
        .fold(0f) { acc, owee -> acc + owee.amountOwed }
        .toFloat()
}

suspend fun MockTransactionHandler.fillUsers(count: Int = 3, startIndex: Int = 0): List<User> {
    val payerUsers = mutableListOf<User>()

    perform {
        for (index in startIndex until startIndex + count) {
            val email = "something@test$index.com"
            val user = UserDAO.new {
                this.email = email
            }.toUser()
            payerUsers.add(user)
        }
    }

    return payerUsers
}
