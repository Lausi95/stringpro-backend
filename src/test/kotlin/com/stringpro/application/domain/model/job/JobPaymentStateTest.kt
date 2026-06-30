package com.stringpro.application.domain.model.job

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate

class JobPaymentStateTest {
    private fun job(
        serviceFeeCents: Long = 2000,
        mainsFeeCents: Long = 1000,
        amountPaidCents: Long = 0,
        fullyPaid: Boolean = false,
    ) = Job(
        id = "job-1",
        customerId = "cust-1",
        racketId = "rack-1",
        dueDate = LocalDate.of(2026, 7, 1),
        notes = null,
        mainsTensionDeciKg = 250,
        crossesTensionDeciKg = 240,
        hybrid = false,
        mainsString = StringChoice.Reel("reel-1", mainsFeeCents),
        crossesString = null,
        serviceFeeCents = serviceFeeCents,
        stage = Stage.ANNOUNCED,
        createdAt = Instant.parse("2026-06-28T10:00:00Z"),
        amountPaidCents = amountPaidCents,
        fullyPaid = fullyPaid,
    )

    @Test
    fun `should not be fully paid when amount paid is below total`() {
        // total = 3000
        val result = job().withAmountPaid(2999)

        assertEquals(2999, result.amountPaidCents)
        assertFalse(result.fullyPaid)
    }

    @Test
    fun `should be fully paid when amount paid equals total`() {
        val result = job().withAmountPaid(3000)

        assertEquals(3000, result.amountPaidCents)
        assertTrue(result.fullyPaid)
    }

    @Test
    fun `should be fully paid when amount paid exceeds total`() {
        val result = job().withAmountPaid(5000)

        assertTrue(result.fullyPaid)
    }

    @Test
    fun `should be fully paid from birth when total is zero`() {
        val result = job(serviceFeeCents = 0, mainsFeeCents = 0, amountPaidCents = 0, fullyPaid = true)

        assertTrue(result.fullyPaid)
    }

    @Test
    fun `should reject a job whose fullyPaid contradicts the amount paid`() {
        assertThrows<IllegalArgumentException> {
            job(amountPaidCents = 0, fullyPaid = true)
        }
    }
}
