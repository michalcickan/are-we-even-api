package eu.models.parameters

import kotlinx.serialization.Serializable

@Serializable
data class AddExpenditureParameters(
    val users: List<AddExpenditureParametersPayer>,
    val description: String,
)

@Serializable
data class UpdateExpenditureParameters(
    val users: List<AddExpenditureParametersPayer>?,
    val description: String?,
)

@Serializable
data class AddExpenditureParametersPayer(
    val id: Long,
    val paidAmount: Float,
    val dueAmount: Float,
)
