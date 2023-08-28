package eu.tables

import eu.tables.AccessTokens.defaultExpression
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Expenses : IntIdTable() {
    val description = varchar("description", 1000)
    val totalAmount = float("totalAmount")
    val groupId = reference("groupId", Groups)
    val createdAt = datetime("createdAt").defaultExpression(CurrentDateTime)
}

class ExpenseDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ExpenseDAO>(Expenses)

    var description by Expenses.description
    var totalAmount by Expenses.totalAmount
    var groupId by GroupDAO referencedOn Expenses.groupId
    var createdAt by Expenses.createdAt
}
