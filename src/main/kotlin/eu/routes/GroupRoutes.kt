package eu.routes

import eu.exceptions.APIException
import eu.models.parameters.expense.AddExpenseParameters
import eu.models.parameters.extractAllExpensesQueryParameters
import eu.services.IExpenseService
import eu.services.IGroupService
import eu.services.IInvitationService
import eu.services.IJWTService
import handleRequestWithExceptions
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.groupRoutes() {
    val jwtService by inject<IJWTService>()
    val groupsService by inject<IGroupService>()
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

        get("groups/invitations") {
            handleRequestWithExceptions(call) {
                invitationService.getInvitations(
                    jwtService.getUserIdFromPrincipalPayload(call.principal()),
                )
            }
        }

        // resolution can be either accept or decline
        post("groups/invitations/{invitationId}/{resolution}") {
            handleRequestWithExceptions(call) {
                val resolution = call.parameters["resolution"]
                if (resolution != "accept" && resolution != "decline") {
                    throw APIException.NotFound
                }
                groupsService.resolveInvitationToGroup(
                    jwtService.getUserIdFromPrincipalPayload(call.principal()),
                    call.parameters["invitationId"]!!.toInt(),
                    resolution == "accept",
                )
            }
        }

        put("groups/expenses/{expenseId}") {
            handleRequestWithExceptions(call) {
                expenseService.updateExpense(
                    call.receive(),
                    call.parameters["expenseId"]!!.toInt(),
                )
            }
        }

        get("groups/{groupId}/expenses") {
            handleRequestWithExceptions(call) {
                val groupId = call.parameters["groupId"]!!.toInt()
                expenseService.getAllExpenses(
                    groupId,
                    call.extractAllExpensesQueryParameters(),
                )
            }
        }

        post("group") {
            handleRequestWithExceptions(call) {
                groupsService.createGroup(
                    call.receive(),
                    jwtService.getUserIdFromPrincipalPayload(call.principal()),
                )
            }
        }

        get("groups") {
            handleRequestWithExceptions(call) {
                groupsService.getGroupsForUser(
                    jwtService.getUserIdFromPrincipalPayload(call.principal()),
                )
            }
        }

        delete("groups/{groupId}") {
            handleRequestWithExceptions(call) {
                groupsService.deleteGroup(call.parameters["groupId"]!!.toInt())
            }
        }

        get("groups/{groupId}") {
            handleRequestWithExceptions(call) {
                groupsService.getGroupDetail(call.parameters["groupId"]!!.toInt())
            }
        }

        post("groups/{groupId}/inviteUser/{inviteeId}") {
            handleRequestWithExceptions(call) {
                groupsService.inviteUserToGroup(
                    call.parameters["groupId"]!!.toInt(),
                    call.parameters["inviteeId"]!!.toLong(),
                )
            }
        }

        put("groups/{groupId}/default") {
            handleRequestWithExceptions(call) {
                groupsService.setDefaultGroup(
                    call.parameters["groupId"]!!.toInt(),
                    jwtService.getUserIdFromPrincipalPayload(call.principal()),
                )
            }
        }

        get("groups/default") {
            handleRequestWithExceptions(call) {
                groupsService.getDefaultGroup(
                    jwtService.getUserIdFromPrincipalPayload(call.principal()),
                )
            }
        }
    }
}
