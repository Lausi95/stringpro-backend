package com.stringpro.application.ports.out.payment

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.payment.Payment

interface PaymentRepository {
    fun save(payment: Payment): Payment

    fun findById(id: String): Payment?

    fun findAll(
        page: Int,
        size: Int,
        jobId: String?,
        customerId: String?,
    ): PageResult<Payment>

    /** All non-deleted Payments for a Job — used to recompute the Job's paid total and to cascade. */
    fun findAllByJobId(jobId: String): List<Payment>
}
