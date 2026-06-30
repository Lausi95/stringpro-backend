package com.stringpro.application.domain.model.payment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class PaymentTest {
    private fun payment(amountCents: Long) =
        Payment(
            id = "pay-1",
            jobId = "job-1",
            customerId = "cust-1",
            amountCents = amountCents,
            method = PaymentMethod.CASH,
            createdAt = Instant.parse("2026-06-28T10:00:00Z"),
        )

    @Test
    fun `should create a payment with a positive amount`() {
        val result = payment(1500)

        assertEquals(1500, result.amountCents)
        assertEquals(PaymentMethod.CASH, result.method)
    }

    @Test
    fun `should reject a zero amount`() {
        assertThrows<IllegalArgumentException> { payment(0) }
    }

    @Test
    fun `should reject a negative amount`() {
        assertThrows<IllegalArgumentException> { payment(-100) }
    }
}
