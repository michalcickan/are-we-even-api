package eu.validation

import eu.exceptions.ValidationException
import eu.helpers.getInvalidMessage
import eu.models.parameters.expense.AddExpenseParameters
import eu.models.parameters.expense.ExpensePayerParameters
import eu.models.parameters.expense.UpdateExpenseParameters
import io.ktor.server.plugins.requestvalidation.*
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ExpenseRequestValidationTest {
    private lateinit var expenseRequestValidation: ExpenseRequestValidation

    @Before
    fun setup() {
        expenseRequestValidation = ExpenseRequestValidation()
    }

    @Test
    fun `validate AddExpense should throw an error when sum of due and paid amounts are not equal`() {
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 30f)

        val result = expenseRequestValidation.addExpense(
            AddExpenseParameters(
                paidAmounts.mapIndexed() { index, paidAmount ->
                    ExpensePayerParameters(
                        2,
                        paidAmount,
                        dueAmounts[index],
                    )
                },
                "test",
            ),
        )
        assertEquals(
            ValidationException.TotalPaidAndDueAmountsAreNotEqual.message,
            result.getInvalidMessage(),
        )
    }

    @Test
    fun `validate AddExpense should throw an error when one of due or paid amounts has negative value`() {
        val paidAmounts = listOf(40f, -20f, 50f)
        val dueAmounts = listOf(-20f, 30f, 60f)

        val result = expenseRequestValidation.addExpense(
            AddExpenseParameters(
                paidAmounts.mapIndexed() { index, paidAmount ->
                    ExpensePayerParameters(
                        2,
                        paidAmount,
                        dueAmounts[index],
                    )
                },
                "test",
            ),
        )
        assertEquals(
            ValidationException.PaidOrDueAmountCannotBeNegative.message,
            result.getInvalidMessage(),
        )
    }

    @Test
    fun `validate UpdateExpense should throw an error when there is no change to perform`() {
        val result = expenseRequestValidation.updateExpense(
            UpdateExpenseParameters(
                null,
                null,
            ),
        )
        assertEquals(
            ValidationException.NoChange.message,
            result.getInvalidMessage(),
        )
    }

    @Test
    fun `validate UpdateExpense should be ok, when at least one change is provided`() {
        val result = expenseRequestValidation.updateExpense(
            UpdateExpenseParameters(
                null,
                "new desc",
            ),
        )
        assertEquals(
            ValidationResult.Valid,
            result,
        )
    }

    @Test
    fun `validate UpdateExpense should throw an error when sum of due and paid amounts are not equal`() {
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 30f)

        val result = expenseRequestValidation.updateExpense(
            UpdateExpenseParameters(
                paidAmounts.mapIndexed() { index, paidAmount ->
                    ExpensePayerParameters(
                        2,
                        paidAmount,
                        dueAmounts[index],
                    )
                },
                null,
            ),
        )
        assertEquals(
            ValidationException.TotalPaidAndDueAmountsAreNotEqual.message,
            result.getInvalidMessage(),
        )
    }

    @Test
    fun `validate UpdateExpense should throw an error when one of due or paid amounts has negative value`() {
        val paidAmounts = listOf(40f, -20f, 50f)
        val dueAmounts = listOf(-20f, 30f, 60f)

        val result = expenseRequestValidation.updateExpense(
            UpdateExpenseParameters(
                paidAmounts.mapIndexed() { index, paidAmount ->
                    ExpensePayerParameters(
                        2,
                        paidAmount,
                        dueAmounts[index],
                    )
                },
                null,
            ),
        )
        assertEquals(
            ValidationException.PaidOrDueAmountCannotBeNegative.message,
            result.getInvalidMessage(),
        )
    }
}
