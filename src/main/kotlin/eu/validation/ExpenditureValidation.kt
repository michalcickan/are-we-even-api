package eu.validation

import eu.exceptions.ValidationException
import eu.models.parameters.AddExpenditureParameters
import eu.models.parameters.AddExpenditureParametersPayer
import eu.models.parameters.UpdateExpenditureParameters
import eu.utils.ExpenditureUtils
import io.ktor.server.plugins.requestvalidation.*

interface IExpenditureRequestValidation {
    fun addExpenditure(params: AddExpenditureParameters): ValidationResult
    fun updateExpenditure(params: UpdateExpenditureParameters): ValidationResult
}

class ExpenditureRequestValidation : IExpenditureRequestValidation {
    override fun addExpenditure(params: AddExpenditureParameters): ValidationResult {
        return checkUsers(params.users)
    }

    override fun updateExpenditure(params: UpdateExpenditureParameters): ValidationResult {
        if (params.users == null && params.description == null) {
            return ValidationResult.Invalid(ValidationException.NoChange.message)
        }
        if (params.users != null) {
            return checkUsers(params.users)
        }
        return ValidationResult.Valid
    }

    private fun checkUsers(users: List<AddExpenditureParametersPayer>): ValidationResult {
        val totalPaidAmount = ExpenditureUtils.getTotalPaidAmount(users)
        val totalDueAmount = ExpenditureUtils.getTotalDueAmount(users)
        if (totalPaidAmount != totalDueAmount) {
            return ValidationResult.Invalid(ValidationException.TotalPaidAndDueAmountsAreNotEqual.message)
        }
        return checkForNegativeValue(users) ?: ValidationResult.Valid
    }

    private fun checkForNegativeValue(users: List<AddExpenditureParametersPayer>): ValidationResult? {
        for (user in users) {
            if (user.dueAmount < 0 || user.paidAmount < 0) {
                return ValidationResult.Invalid(ValidationException.PaidOrDueAmountCannotBeNegative.message)
            }
        }
        return null
    }
}
