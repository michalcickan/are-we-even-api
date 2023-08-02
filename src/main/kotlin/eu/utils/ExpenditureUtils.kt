package eu.utils

import eu.models.parameters.expense.ExpensePayerParameters

class ExpenseUtils {
    companion object {
        fun getTotalPaidAmount(users: List<ExpensePayerParameters>): Double {
            return users.fold(0.0) { acc, payer ->
                acc + payer.paidAmount
            }
        }

        fun getTotalDueAmount(users: List<ExpensePayerParameters>): Double {
            return users.fold(0.0) { acc, payer ->
                acc + payer.dueAmount
            }
        }
    }
}
