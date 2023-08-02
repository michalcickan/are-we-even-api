package eu.models.parameters.expense

import kotlinx.serialization.Serializable

@Serializable
data class AddExpenseParameters(
    val users: List<ExpensePayerParameters>,
    val description: String,
)
