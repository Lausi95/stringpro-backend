package com.stringpro.application.ports.`in`.payment

import com.stringpro.application.domain.model.payment.Payment
import com.stringpro.application.domain.model.payment.PaymentMethod

interface CreatePaymentUseCase {
    fun create(command: CreatePaymentCommand): Payment
}

data class CreatePaymentCommand(
    val jobId: String,
    val customerId: String,
    val amountCents: Long,
    val method: PaymentMethod,
)
