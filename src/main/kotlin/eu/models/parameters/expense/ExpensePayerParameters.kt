package eu.models.parameters.expense

import kotlinx.serialization.Serializable

@Serializable
data class ExpensePayerParameters(
    val id: Long,
    val paidAmount: Float,
    val dueAmount: Float,
)
