package eu.services

import eu.models.parameters.AddExpenditureParameters
import eu.models.parameters.AddExpenditureParametersPayer
import eu.models.parameters.UpdateExpenditureParameters
import eu.models.responses.Expenditure
import eu.models.responses.toExpenditure
import eu.modules.ITransactionHandler
import eu.tables.*
import eu.utils.ExpenditureUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

interface IExpenditureService {
    suspend fun getExpenditure(id: Int): Expenditure
    suspend fun getAllExpenditures(groupId: Int): List<Expenditure>
    suspend fun addExpenditure(params: AddExpenditureParameters, groupId: Int): Expenditure
    suspend fun updateExpenditure(
        params: UpdateExpenditureParameters,
        groupId: Int,
        expenditureId: Int,
    ): Expenditure
}

class ExpenditureService(
    private val transactionHandler: ITransactionHandler,
) : IExpenditureService {
    override suspend fun getExpenditure(id: Int): Expenditure {
        return transactionHandler.perform {
            ExpenditureDAO[id].getUsersAndMakeExpenditure()
//            val usersInGroup = UserGroupDAO.find { UsersGroups.groupId eq expenditure.id }
        }
    }

    override suspend fun getAllExpenditures(groupId: Int): List<Expenditure> {
        return transactionHandler
            .perform {
                ExpenditureDAO.find { Expenditures.groupId eq groupId }
                    .map { it.toExpenditure(null) }
            }
    }

    override suspend fun addExpenditure(params: AddExpenditureParameters, groupId: Int): Expenditure {
        return transactionHandler.perform {
            val expenditure = ExpenditureDAO.new {
                this.description = params.description
                this.totalAmount = ExpenditureUtils.getTotalPaidAmount(params.users).toFloat()
                this.groupId = GroupDAO[groupId]
            }
            /*
            Note: This function must preceed  fillUserExpenditureTable, because otherwise it can spoil calculations
            for the debt. App cannot rely upon the fact, that the new records should not be selectable till
            end of the transaction. When this is after the fillUserExpenditureTable function, sometimes it gets
            from database a data including current transaction UserExpenditure data, sometimes not, so the calculation
            was not reliable.
             */
            fillOrUpdateOweeTable(params.users, groupId)
            fillUserExpenditureTable(params.users, expenditure)
            expenditure.getUsersAndMakeExpenditure()
        }
    }

    override suspend fun updateExpenditure(
        params: UpdateExpenditureParameters,
        groupId: Int,
        expenditureId: Int,
    ): Expenditure {
        return transactionHandler.perform {
            val expenditure = ExpenditureDAO[expenditureId]
            if (params.description != null) {
                expenditure.description = params.description
            }
            if (params.users != null) {
                expenditure.totalAmount = ExpenditureUtils.getTotalPaidAmount(params.users).toFloat()
                updateUserExpenditureTable(params.users, expenditure)
            }
            expenditure.getUsersAndMakeExpenditure()
        }
    }

    private fun fillUserExpenditureTable(users: List<AddExpenditureParametersPayer>, expenditure: ExpenditureDAO) {
        users.forEach { user ->
            if (user.paidAmount > 0) {
                UserExpenditureDAO.new {
                    this.expenditureID = expenditure
                    this.paidAmount = user.paidAmount
                    this.dueAmount = user.dueAmount
                    this.user = UserDAO[user.id]
                }
            }
        }
    }

    private fun updateUserExpenditureTable(users: List<AddExpenditureParametersPayer>, expenditure: ExpenditureDAO) {
        users.forEach { user ->
            try {
                val userExpenditure = UserExpenditureDAO
                    .find {
                        (UserExpenditures.userId eq user.id) and (UserExpenditures.expenditureId eq expenditure.id)
                    }
                    .first()
                userExpenditure.paidAmount = user.paidAmount
                userExpenditure.dueAmount = user.dueAmount
            } catch (e: Exception) {
                print(e)
            }
        }
    }

    private fun fillOrUpdateOweeTable(users: List<AddExpenditureParametersPayer>, groupId: Int) {
        val paidHigherThanShould = mutableListOf<AddExpenditureParametersPayer>()
        val paidLesserThanShould = mutableListOf<AddExpenditureParametersPayer>()
        for (user in users) {
            val priorExpenditures = getAllUserExpendituresWithGroupId(groupId, user.id)
            val newUser = user.copy(
                paidAmount = user.paidAmount + priorExpenditures.paidAmount,
                dueAmount = user.dueAmount + priorExpenditures.dueAmount,
            )
            if (newUser.paidAmount > newUser.dueAmount) {
                paidHigherThanShould.add(newUser)
            } else if (newUser.paidAmount < newUser.dueAmount) {
                paidLesserThanShould.add(newUser)
            } else {
                // neutral. They are useless in this function
            }
        }
        OweeDAO
            .find { Owees.groupId eq groupId }
            .forEach { it.delete() }
        paidLesserThanShould.forEach { owee ->
            var diff = owee.dueAmount - owee.paidAmount
            var amountToWrite = 0f
            do {
                var userToUseToEven = paidHigherThanShould[0]
                val higherDiff = userToUseToEven.paidAmount - userToUseToEven.dueAmount
                if (higherDiff >= diff) {
                    amountToWrite = diff
                    val index = paidHigherThanShould.indexOf(userToUseToEven)
                    paidHigherThanShould[index] = userToUseToEven.copy(paidAmount = userToUseToEven.paidAmount - diff)
                } else {
                    amountToWrite = diff - higherDiff
                    // we depleted users all resources, so the user is on the same level with due amount, and we cannot use it anymore
                    paidHigherThanShould.remove(userToUseToEven)
                }
                OweeDAO.new {
                    this.groupId = GroupDAO[groupId]
                    this.payerUser = UserDAO[userToUseToEven.id]
                    this.oweeUser = UserDAO[owee.id]
                    this.amountOwed = amountToWrite
                }
                diff -= amountToWrite
            } while (diff > 0)
        }
    }

    private fun getAllUserExpendituresWithGroupId(groupId: Int, userId: Long): _UserExpenditure {
        // Join the UserExpenditures and Expenditures tables based on the group ID
        try {
            val expenditure = (UserExpenditures innerJoin Expenditures)
                .select { (Expenditures.groupId eq groupId) and (UserExpenditures.userId eq userId) }
                .first()

            return _UserExpenditure(
                expenditure[UserExpenditures.paidAmount],
                expenditure[UserExpenditures.dueAmount],
            )
        } catch (e: Exception) {
            return _UserExpenditure(0f, 0f)
        }
    }
}

private data class _UserExpenditure(
    val paidAmount: Float,
    val dueAmount: Float,
)

private fun ExpenditureDAO.getUsersAndMakeExpenditure(): Expenditure {
    val users = UserExpenditureDAO
        .find { UserExpenditures.expenditureId eq id }
        .toList()
    return toExpenditure(users)
}
