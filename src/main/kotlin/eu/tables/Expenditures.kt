package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Expenditures : IntIdTable() {
    val description = varchar("description", 1000)
    val totalAmount = float("totalAmount")
    val groupId = reference("groupId", Groups)
}

class ExpenditureDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ExpenditureDAO>(Expenditures)

    var description by Expenditures.description
    var totalAmount by Expenditures.totalAmount
    var groupId by GroupDAO referencedOn Expenditures.groupId
}
