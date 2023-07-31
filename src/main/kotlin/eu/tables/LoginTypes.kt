import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

enum class LoginType {
    GOOGLE, APPLE, EMAIL
}

object LoginTypes : IntIdTable() {
    val loginType: Column<LoginType> = enumerationByName("login_type", 20, LoginType::class).uniqueIndex()
}

class LoginTypeDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LoginTypeDao>(LoginTypes) {
        @JvmStatic
        fun initializeTable() {
            for (type in LoginType.values()) {
                if (find { LoginTypes.loginType eq type }.empty()) {
                    new {
                        loginType = type
                    }
                }
            }
        }
    }

    var loginType by LoginTypes.loginType
}
