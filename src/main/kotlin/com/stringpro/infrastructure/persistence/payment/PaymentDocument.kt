package com.stringpro.infrastructure.persistence.payment

import com.stringpro.application.domain.model.payment.PaymentMethod
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "payments")
data class PaymentDocument(
    @Id val id: String,
    val jobId: String,
    val customerId: String,
    val amountCents: Long,
    val method: PaymentMethod,
    val createdAt: Instant,
    val deletedAt: Instant?,
)
