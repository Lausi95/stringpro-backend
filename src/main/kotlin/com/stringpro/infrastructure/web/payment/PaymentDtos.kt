package com.stringpro.infrastructure.web.payment

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.payment.Payment
import com.stringpro.application.domain.model.payment.PaymentMethod
import com.stringpro.infrastructure.web.centsToEuros
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant

// --- Request (API speaks decimal euros; storage is integer minor units, see ADR 0002) ---

data class CreatePaymentRequest(
    @field:NotBlank val jobId: String,
    @field:NotBlank val customerId: String,
    @field:NotNull
    @field:DecimalMin(value = "0.00", inclusive = false)
    @field:Digits(integer = 7, fraction = 2)
    val amount: BigDecimal?,
    @field:NotNull val method: PaymentMethod?,
)

// --- Responses ---

data class PaymentResponse(
    val id: String,
    val jobId: String,
    val customerId: String,
    val amount: BigDecimal,
    val method: PaymentMethod,
    val createdAt: Instant,
)

data class PagedPaymentResponse(
    val content: List<PaymentResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
)

fun Payment.toResponse() =
    PaymentResponse(
        id = id,
        jobId = jobId,
        customerId = customerId,
        amount = centsToEuros(amountCents),
        method = method,
        createdAt = createdAt,
    )

fun PageResult<Payment>.toResponse() =
    PagedPaymentResponse(
        content = content.map { it.toResponse() },
        totalElements = totalElements,
        totalPages = totalPages,
        page = page,
        size = size,
    )
