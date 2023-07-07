package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Expenditures : IntIdTable() {
    val description = varchar("description", 1000)
    val totalAmount = float("totalAmount")
}

class ExpenditureDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ExpenditureDAO>(Expenditures)

    val description by Expenditures.description
    val totalAmount by Expenditures.totalAmount
}
