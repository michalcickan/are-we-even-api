package eu.routes

import eu.models.parameters.expense.AddExpenseParameters
import eu.services.*
import handleRequestWithExceptions
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.groupRoutes() {
    val jwtService by inject<IJWTService>()
    val groupsRouteService by inject<IGroupService>()
    val expenseService by inject<IExpenseService>()
    val invitationService by inject<IInvitationService>()

    authenticate("auth-jwt") {
        post("groups/{groupId}/expense") {
            handleRequestWithExceptions(call) {
                val groupId = call.parameters["groupId"]!!.toInt()
                val params = call.receive<AddExpenseParameters>()
                expenseService.addExpense(params, groupId)
            }
        }

        post("groups/invitations") {
            handleRequestWithExceptions(call) {
                invitationService.getInvitations(
                    jwtService.getUserIdFromPrincipalPayload(call.principal()),
                )
            }
        }

        put("groups/{groupId}/expenses/{expenseId}") {
            handleRequestWithExceptions(call) {
                expenseService.updateExpense(
                    call.receive(),
                    call.parameters["groupId"]!!.toInt(),
                    call.parameters["expenseId"]!!.toInt(),
                )
            }
        }

        get("groups/{groupId}/expenses") {
            handleRequestWithExceptions(call) {
                val groupId = call.parameters["groupId"]!!.toInt()
                expenseService.getAllExpenses(groupId)
            }
        }

        post("group") {
            handleRequestWithExceptions(call) {
                groupsRouteService.createGroup(
                    call.receive(),
                    jwtService.getUserIdFromPrincipalPayload(call.principal()),
                )
            }
        }

        get("groups") {
            handleRequestWithExceptions(call) {
                groupsRouteService.getGroupsForUser(
                    jwtService.getUserIdFromPrincipalPayload(call.principal()),
                )
            }
        }

        post("groups/{groupId}/users/{userId}") {
            handleRequestWithExceptions(call) {
                groupsRouteService.addUserToGroup(
                    call.parameters["groupId"]!!.toInt(),
                    call.parameters["userId"]!!.toLong(),
                )
            }
        }
    }
}
