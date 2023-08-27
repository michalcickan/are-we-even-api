package eu.validation

import eu.exceptions.ValidationException
import eu.models.parameters.expense.AddExpenseParameters
import eu.models.parameters.expense.ExpensePayerParameters
import eu.models.parameters.expense.UpdateExpenseParameters
import eu.utils.ExpenseUtils
import io.ktor.server.plugins.requestvalidation.*

interface IExpenseRequestValidation {
    fun addExpense(params: AddExpenseParameters): ValidationResult
    fun updateExpense(params: UpdateExpenseParameters): ValidationResult
}

class ExpenseRequestValidation : IExpenseRequestValidation {
    override fun addExpense(params: AddExpenseParameters): ValidationResult {
        return checkUsers(params.users)
    }

    override fun updateExpense(params: UpdateExpenseParameters): ValidationResult {
        if (params.users == null && params.description == null) {
            return ValidationResult.Invalid(ValidationException.NoChange.message)
        }
        if (params.users != null) {
            return checkUsers(params.users)
        }
        return ValidationResult.Valid
    }

    private fun checkUsers(users: List<ExpensePayerParameters>): ValidationResult {
        val totalPaidAmount = ExpenseUtils.getTotalPaidAmount(users)
        val totalDueAmount = ExpenseUtils.getTotalDueAmount(users)
        if (totalPaidAmount != totalDueAmount) {
            return ValidationResult.Invalid(ValidationException.TotalPaidAndDueAmountsAreNotEqual.message)
        }
        return checkForNegativeValue(users) ?: ValidationResult.Valid
    }

    private fun checkForNegativeValue(users: List<ExpensePayerParameters>): ValidationResult? {
        for (user in users) {
            if (user.dueAmount < 0 || user.paidAmount < 0) {
                return ValidationResult.Invalid(ValidationException.PaidOrDueAmountCannotBeNegative.message)
            }
        }
        return null
    }
}
