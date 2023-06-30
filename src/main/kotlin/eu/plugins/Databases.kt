package eu.plugins

import eu.modules.ITransactionHandler
import io.ktor.server.application.Application
import io.lettuce.core.*
import org.koin.ktor.ext.inject

fun Application.configureDatabases() {
    val transactionHandler by inject<ITransactionHandler>()
    transactionHandler.createTables()
}
