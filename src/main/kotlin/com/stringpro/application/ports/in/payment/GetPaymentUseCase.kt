package com.stringpro.application.ports.`in`.payment

import com.stringpro.application.domain.model.payment.Payment

interface GetPaymentUseCase {
    fun get(id: String): Payment
}
