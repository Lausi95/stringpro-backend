package com.stringpro.application.ports.`in`.payment

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.payment.Payment

interface ListPaymentsUseCase {
    fun list(query: ListPaymentsQuery): PageResult<Payment>
}

data class ListPaymentsQuery(
    val page: Int,
    val size: Int,
    val jobId: String?,
    val customerId: String?,
)
