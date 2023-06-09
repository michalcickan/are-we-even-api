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
    companion object {
        fun fromUserDAO(userDAO: UserDAO): User {
            val addresses = userDAO.addresses.map { Address.fromAddressDAO(it) }
            return User(userDAO.id.value, userDAO.name, userDAO.middleName, userDAO.surname, addresses)
        }
    }
    fun toUserDAO(): UserDAO {
        return UserDAO.new(id) {
            this.name = this@User.name
            this.middleName = this@User.middleName
            this.surname = this@User.surname
        }
    }
}

/*
https://www.baeldung.com/kotlin/json-convert-data-class

var jsonString = """{"id":1,"description":"Test"}""";
var testModel = gson.fromJson(jsonString, TestModel::class.java)
Assert.assertEquals(testModel.id, 1)
Assert.assertEquals(testModel.description, "Test")
 */
