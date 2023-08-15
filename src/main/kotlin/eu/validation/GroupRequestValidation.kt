package eu.validation

import eu.exceptions.ValidationException
import eu.models.parameters.CreateGroupParameters
import io.ktor.server.plugins.requestvalidation.*

interface IGroupRequestValidation {
    fun createGroup(params: CreateGroupParameters): ValidationResult
}

class GroupRequestValidation : IGroupRequestValidation {
    override fun createGroup(params: CreateGroupParameters): ValidationResult {
        if (params.name.isEmpty()) {
            return ValidationResult.Invalid(ValidationException.CannotBeEmpty("group name").message)
        }
        return ValidationResult.Valid
    }
}
