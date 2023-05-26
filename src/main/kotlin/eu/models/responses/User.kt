package eu.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val name: String,
    val middleName: String?,
    val surname: String,
    val addresses: List<Address>?,
)

/*
https://www.baeldung.com/kotlin/json-convert-data-class

var jsonString = """{"id":1,"description":"Test"}""";
var testModel = gson.fromJson(jsonString, TestModel::class.java)
Assert.assertEquals(testModel.id, 1)
Assert.assertEquals(testModel.description, "Test")
 */
