package com.expensetracker.app.ui.screens.transaction

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TransactionDatePickerTest {
    @Test
    fun `date picker millis round trip preserves back dated entry date`() {
        val dates = listOf(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2025, 12, 31),
            LocalDate.now().minusDays(30)
        )

        dates.forEach { date ->
            assertEquals(date, date.toDatePickerUtcMillis().toDatePickerLocalDate())
        }
    }
}
