package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object UsersExpenses : IntIdTable() {
    val userId = reference("userId", Users)
    val expenseId = reference("expenseId", Expenses)
    val paidAmount = float("paidAmount")
    val dueAmount = float("dueAmount")
}

class UserExpenseDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserExpenseDAO>(UsersExpenses)

    var user by UserDAO referencedOn UsersExpenses.userId
    var expenseID by ExpenseDAO referencedOn UsersExpenses.expenseId
    var paidAmount by UsersExpenses.paidAmount
    var dueAmount by UsersExpenses.dueAmount
}
