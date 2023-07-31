package eu.models.responses

import eu.tables.GroupDAO

class Group(
    val name: String,
)

fun GroupDAO.toGroup(): Group {
    return Group(this.name)
}
