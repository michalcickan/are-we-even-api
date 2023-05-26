package eu.plugins

import eu.modules.TransactionHandler
import io.ktor.server.application.Application
import io.lettuce.core.*
import org.koin.ktor.ext.inject

fun Application.configureDatabases() {
    val transactionHandler by inject<TransactionHandler>()
    transactionHandler.createTables()
}
