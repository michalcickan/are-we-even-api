package eu.modules

import eu.validation.*
import org.koin.dsl.module

val validationModule = module {
    factory<IAuthRequestValidation> { AuthRequestValidation() }
    factory<IExpenseRequestValidation> { ExpenseRequestValidation() }
    factory<IGroupRequestValidation> { GroupRequestValidation() }
}
