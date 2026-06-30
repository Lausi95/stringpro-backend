package com.stringpro.application.domain.model.payment

import java.time.Instant

data class Payment(
    val id: String,
    val jobId: String,
    val customerId: String,
    val amountCents: Long,
    val method: PaymentMethod,
    val createdAt: Instant,
    val deletedAt: Instant? = null,
) {
    init {
        require(amountCents > 0) { "Payment amount must be positive" }
    }
}
