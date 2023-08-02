package eu.modules

import eu.validation.AuthRequestValidation
import eu.validation.ExpenseRequestValidation
import eu.validation.IAuthRequestValidation
import eu.validation.IExpenseRequestValidation
import org.koin.dsl.module

val validationModule = module {
    factory<IAuthRequestValidation> { AuthRequestValidation() }
    factory<IExpenseRequestValidation> { ExpenseRequestValidation() }
}
