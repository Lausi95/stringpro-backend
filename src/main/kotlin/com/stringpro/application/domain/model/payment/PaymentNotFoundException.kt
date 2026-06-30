package com.stringpro.application.domain.model.payment

class PaymentNotFoundException(id: String) : RuntimeException("Payment not found: $id")
