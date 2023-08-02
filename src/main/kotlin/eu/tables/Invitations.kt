package eu.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Invitations : IntIdTable() {
    val userId = reference("userId", Users)
    val groupId = reference("groupId", Groups)
    val accepted = bool("accepted").nullable()
}

class InvitationDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<InvitationDAO>(Invitations)

    var accepted by Invitations.accepted
    var user by UserDAO referencedOn Invitations.userId
    var group by GroupDAO referencedOn Invitations.groupId
}
