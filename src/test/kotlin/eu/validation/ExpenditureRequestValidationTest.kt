package eu.validation

import eu.exceptions.ValidationException
import eu.helpers.getInvalidMessage
import eu.models.parameters.AddExpenditureParameters
import eu.models.parameters.AddExpenditureParametersPayer
import eu.models.parameters.UpdateExpenditureParameters
import io.ktor.server.plugins.requestvalidation.*
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ExpenditureRequestValidationTest {
    private lateinit var expenditureRequestValidation: ExpenditureRequestValidation

    @Before
    fun setup() {
        expenditureRequestValidation = ExpenditureRequestValidation()
    }

    @Test
    fun `validate AddExpenditure should throw an error when sum of due and paid amounts are not equal`() {
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 30f)

        val result = expenditureRequestValidation.addExpenditure(
            AddExpenditureParameters(
                paidAmounts.mapIndexed() { index, paidAmount ->
                    AddExpenditureParametersPayer(
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
    fun `validate AddExpenditure should throw an error when one of due or paid amounts has negative value`() {
        val paidAmounts = listOf(40f, -20f, 50f)
        val dueAmounts = listOf(-20f, 30f, 60f)

        val result = expenditureRequestValidation.addExpenditure(
            AddExpenditureParameters(
                paidAmounts.mapIndexed() { index, paidAmount ->
                    AddExpenditureParametersPayer(
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
    fun `validate UpdateExpenditure should throw an error when there is no change to perform`() {
        val result = expenditureRequestValidation.updateExpenditure(
            UpdateExpenditureParameters(
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
    fun `validate UpdateExpenditure should be ok, when at least one change is provided`() {
        val result = expenditureRequestValidation.updateExpenditure(
            UpdateExpenditureParameters(
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
    fun `validate UpdateExpenditure should throw an error when sum of due and paid amounts are not equal`() {
        val paidAmounts = listOf(40f, 20f, 50f)
        val dueAmounts = listOf(20f, 30f, 30f)

        val result = expenditureRequestValidation.updateExpenditure(
            UpdateExpenditureParameters(
                paidAmounts.mapIndexed() { index, paidAmount ->
                    AddExpenditureParametersPayer(
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
    fun `validate UpdateExpenditure should throw an error when one of due or paid amounts has negative value`() {
        val paidAmounts = listOf(40f, -20f, 50f)
        val dueAmounts = listOf(-20f, 30f, 60f)

        val result = expenditureRequestValidation.updateExpenditure(
            UpdateExpenditureParameters(
                paidAmounts.mapIndexed() { index, paidAmount ->
                    AddExpenditureParametersPayer(
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
