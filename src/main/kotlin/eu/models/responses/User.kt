package eu.models.responses

import eu.tables.UserDAO
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val name: String,
    val middleName: String?,
    val surname: String,
    val addresses: List<Address>,
) {
    fun toUserDAO(): UserDAO {
        return UserDAO.new(id) {
            this.name = this@User.name
            this.middleName = this@User.middleName
            this.surname = this@User.surname
        }
    }
}

fun UserDAO.toUser(): User {
    val addresses = this.addresses.map { it.toAddress() }
    return User(
        this.id.value,
        this.name,
        this.middleName,
        this.surname,
        addresses,
    )
}

/*
https://www.baeldung.com/kotlin/json-convert-data-class

var jsonString = """{"id":1,"description":"Test"}""";
var testModel = gson.fromJson(jsonString, TestModel::class.java)
Assert.assertEquals(testModel.id, 1)
Assert.assertEquals(testModel.description, "Test")
 */
