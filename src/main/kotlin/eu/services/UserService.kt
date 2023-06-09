package eu.services

import eu.models.parameters.UserParameters
import eu.models.responses.User
import eu.modules.TransactionHandler
import eu.tables.UserDAO

interface UserService {
    suspend fun getUsers(): List<User>
    suspend fun getUser(id: Long): User
    suspend fun createUser(params: UserParameters): User
}

class UserServiceImpl(val transactionHandler: TransactionHandler) : UserService {
    override suspend fun getUsers(): List<User> {
        return transactionHandler.perform {
            val test = UserDAO.all().map { User.fromUserDAO(it) }
            test
        }
    }

    override suspend fun createUser(params: UserParameters): User {
        return transactionHandler.perform {
            User.fromUserDAO(
                UserDAO.new {
                    name = params.name
                    surname = params.surname
                    middleName = params.middleName
                },
            )
        }
    }

    override suspend fun getUser(id: Long): User {
        return transactionHandler.perform {
            User.fromUserDAO(UserDAO[id])
        }
    }
}
