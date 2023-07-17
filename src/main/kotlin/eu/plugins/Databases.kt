package eu.plugins

import LoginTypeDao
import LoginTypeTable
import eu.modules.ITransactionHandler
import eu.tables.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.koin.ktor.ext.inject

fun Application.configureDatabases() {
    val transactionHandler by inject<ITransactionHandler>()
    transactionHandler.syncPerform {
        SchemaUtils.createMissingTablesAndColumns(
            Users,
            Addresses,
            LoginTypeTable,
            Owes,
            Expenditures,
            UserExpenditure,
            AccessTokens,
            UserPasswords,
            RefreshTokens,
        )
        LoginTypeDao.initializeTable()
    }
}
