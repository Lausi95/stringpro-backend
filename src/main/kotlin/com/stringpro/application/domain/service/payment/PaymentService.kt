package com.stringpro.application.domain.service.payment

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.job.JobNotFoundException
import com.stringpro.application.domain.model.payment.Payment
import com.stringpro.application.domain.model.payment.PaymentCustomerMismatchException
import com.stringpro.application.domain.model.payment.PaymentNotFoundException
import com.stringpro.application.ports.`in`.payment.CreatePaymentCommand
import com.stringpro.application.ports.`in`.payment.CreatePaymentUseCase
import com.stringpro.application.ports.`in`.payment.DeletePaymentUseCase
import com.stringpro.application.ports.`in`.payment.GetPaymentUseCase
import com.stringpro.application.ports.`in`.payment.ListPaymentsQuery
import com.stringpro.application.ports.`in`.payment.ListPaymentsUseCase
import com.stringpro.application.ports.out.job.JobRepository
import com.stringpro.application.ports.out.payment.PaymentRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val jobRepository: JobRepository,
) : CreatePaymentUseCase,
    GetPaymentUseCase,
    ListPaymentsUseCase,
    DeletePaymentUseCase {
    override fun create(command: CreatePaymentCommand): Payment {
        val job =
            jobRepository.findById(command.jobId)
                ?: throw JobNotFoundException(command.jobId)
        if (job.customerId != command.customerId) {
            throw PaymentCustomerMismatchException(command.jobId, command.customerId)
        }

        val payment =
            paymentRepository.save(
                Payment(
                    id = UUID.randomUUID().toString(),
                    jobId = command.jobId,
                    customerId = command.customerId,
                    amountCents = command.amountCents,
                    method = command.method,
                    createdAt = Instant.now(),
                ),
            )

        recomputeJobPaidTotal(command.jobId)
        return payment
    }

    override fun get(id: String): Payment = paymentRepository.findById(id) ?: throw PaymentNotFoundException(id)

    override fun list(query: ListPaymentsQuery): PageResult<Payment> =
        paymentRepository.findAll(query.page, query.size, query.jobId, query.customerId)

    override fun delete(id: String) {
        val payment = paymentRepository.findById(id) ?: throw PaymentNotFoundException(id)
        paymentRepository.save(payment.copy(deletedAt = Instant.now()))
        recomputeJobPaidTotal(payment.jobId)
    }

    /**
     * Recompute and cache the Job's paid total from the single source of truth — the sum of its
     * non-deleted Payments. Re-summing (rather than incrementing) means any prior drift self-heals.
     */
    private fun recomputeJobPaidTotal(jobId: String) {
        val job = jobRepository.findById(jobId) ?: return
        val total = paymentRepository.findAllByJobId(jobId).sumOf { it.amountCents }
        jobRepository.save(job.withAmountPaid(total))
    }
}
