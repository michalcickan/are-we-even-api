package eu.routes

import eu.models.parameters.AddExpenditureParameters
import eu.services.IExpenditureService
import eu.services.IGroupService
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
    val groupService by inject<IGroupService>()

    authenticate("auth-jwt") {
        post("groups/{groupId}/expenditure") {
            handleRequestWithExceptions(call) {
                val groupId = call.parameters["groupId"]!!.toInt()
                val params = call.receive<AddExpenditureParameters>()
                expenditureService.addExpenditure(params, groupId)
            }
        }

        put("groups/{groupId}/expenditures/{expenditureId}") {
            handleRequestWithExceptions(call) {
                expenditureService.updateExpenditure(
                    call.receive(),
                    call.parameters["groupId"]!!.toInt(),
                    call.parameters["expenditureId"]!!.toInt(),
                )
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
                groupService.createGroup(
                    call.receive(),
                    jwtService.getUserIdFromPrincipalPayload(call.principal()),
                )
            }
        }

        get("groups") {
            handleRequestWithExceptions(call) {
                groupService.getGroupsForUser(
                    jwtService.getUserIdFromPrincipalPayload(call.principal()),
                )
            }
        }

        post("groups/{groupId}/users/{userId}") {
            handleRequestWithExceptions(call) {
                groupService.addUserToGroup(
                    call.parameters["groupId"]!!.toInt(),
                    call.parameters["userId"]!!.toLong(),
                )
            }
        }
    }
}
