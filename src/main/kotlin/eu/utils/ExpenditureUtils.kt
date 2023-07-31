package eu.utils

import eu.models.parameters.AddExpenditureParametersPayer

class ExpenditureUtils {
    companion object {
        fun getTotalPaidAmount(users: List<AddExpenditureParametersPayer>): Double {
            return users.fold(0.0) { acc, payer ->
                acc + payer.paidAmount
            }
        }

        fun getTotalDueAmount(users: List<AddExpenditureParametersPayer>): Double {
            return users.fold(0.0) { acc, payer ->
                acc + payer.dueAmount
            }
        }
    }
}
