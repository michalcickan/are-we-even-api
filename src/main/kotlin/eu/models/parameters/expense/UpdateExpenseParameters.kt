package eu.models.parameters.expense

import kotlinx.serialization.Serializable

@Serializable
data class UpdateExpenseParameters(
    val users: List<ExpensePayerParameters>?,
    val description: String?,
)
