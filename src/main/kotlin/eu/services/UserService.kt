package eu.services

import eu.models.responses.User
import eu.models.responses.toUser
import eu.modules.ITransactionHandler
import eu.tables.UserDAO

interface UserService {
    suspend fun getUsers(): List<User>
    suspend fun getUser(id: Long): User
    suspend fun createUser(params: User): User
}

class UserServiceImpl(private val transactionHandler: ITransactionHandler) : UserService {
    override suspend fun getUsers(): List<User> {
        return transactionHandler.perform {
            val test = UserDAO.all().map { it.toUser() }
            test
        }
    }

    override suspend fun createUser(params: User): User {
        return transactionHandler.perform {
            params.toUserDAO()
            params
//            UserDAO.new {
//                name = params.name
//                surname = params.surname
//                middleName = params.middleName
//            }.toUser()
        }
    }

    override suspend fun getUser(id: Long): User {
        return transactionHandler.perform {
            UserDAO[id].toUser()
        }
    }
}
