package eu.modules

import eu.validation.AuthRequestValidation
import eu.validation.ExpenditureRequestValidation
import eu.validation.IAuthRequestValidation
import eu.validation.IExpenditureRequestValidation
import org.koin.dsl.module

val validationModule = module {
    factory<IAuthRequestValidation> { AuthRequestValidation() }
    factory<IExpenditureRequestValidation> { ExpenditureRequestValidation() }
}
