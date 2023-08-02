package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Expenses : IntIdTable() {
    val description = varchar("description", 1000)
    val totalAmount = float("totalAmount")
    val groupId = reference("groupId", Groups)
}

class ExpenseDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ExpenseDAO>(Expenses)

    var description by Expenses.description
    var totalAmount by Expenses.totalAmount
    var groupId by GroupDAO referencedOn Expenses.groupId
}
