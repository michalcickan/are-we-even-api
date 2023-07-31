package eu.routes

import eu.models.parameters.AddExpenditureParameters
import eu.models.parameters.UpdateExpenditureParameters
import eu.services.IExpenditureService
import eu.services.IJWTService
import handleRequestWithExceptions
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.groupRoutes() {
    val expenditureService by inject<IExpenditureService>()
    val jwtService by inject<IJWTService>()

    authenticate("auth-jwt") {
        post("groups/{groupId}/expenditure") {
            handleRequestWithExceptions(call) {
                val groupId = call.parameters["groupId"]!!.toInt()
                val params = call.receive<AddExpenditureParameters>()
                expenditureService.addExpenditure(params, groupId)
            }
        }

        put("groups/{groupId}/expenditure") {
            handleRequestWithExceptions(call) {
                val groupId = call.parameters["groupId"]!!.toInt()
                val params = call.receive<UpdateExpenditureParameters>()
                expenditureService.updateExpenditure(params, groupId)
            }
        }

        get("groups/{groupId}/expenditures") {
            handleRequestWithExceptions(call) {
                val groupId = call.parameters["groupId"]!!.toInt()
                expenditureService.getAllExpenditures(groupId)
            }
        }

        post("group") {
            handleRequestWithExceptions(call) {
                expenditureService.createGroup(
                    call.receive(),
                    jwtService.getUserIdFromPrincipalPayload(call.principal()),
                )
            }
        }
    }
}
