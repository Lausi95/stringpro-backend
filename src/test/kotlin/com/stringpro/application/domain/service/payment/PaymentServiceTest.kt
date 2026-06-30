package com.stringpro.application.domain.service.payment

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.job.Job
import com.stringpro.application.domain.model.job.JobNotFoundException
import com.stringpro.application.domain.model.job.Stage
import com.stringpro.application.domain.model.job.StringChoice
import com.stringpro.application.domain.model.payment.Payment
import com.stringpro.application.domain.model.payment.PaymentCustomerMismatchException
import com.stringpro.application.domain.model.payment.PaymentMethod
import com.stringpro.application.domain.model.payment.PaymentNotFoundException
import com.stringpro.application.ports.`in`.payment.CreatePaymentCommand
import com.stringpro.application.ports.`in`.payment.ListPaymentsQuery
import com.stringpro.application.ports.out.job.JobRepository
import com.stringpro.application.ports.out.payment.PaymentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate

class PaymentServiceTest {
    private val paymentRepository: PaymentRepository = mockk()
    private val jobRepository: JobRepository = mockk()
    private val service = PaymentService(paymentRepository, jobRepository)

    private fun aJob(
        id: String = "job-1",
        customerId: String = "cust-1",
        serviceFeeCents: Long = 2000,
        mainsFeeCents: Long = 1000,
        amountPaidCents: Long = 0,
        fullyPaid: Boolean = false,
    ) = Job(
        id = id,
        customerId = customerId,
        racketId = "rack-1",
        dueDate = LocalDate.of(2026, 7, 1),
        notes = null,
        mainsTensionDeciKg = 250,
        crossesTensionDeciKg = 240,
        hybrid = false,
        mainsString = StringChoice.Reel("reel-1", mainsFeeCents),
        crossesString = null,
        serviceFeeCents = serviceFeeCents,
        stage = Stage.ANNOUNCED,
        createdAt = Instant.parse("2026-06-28T10:00:00Z"),
        amountPaidCents = amountPaidCents,
        fullyPaid = fullyPaid,
    )

    private fun aPayment(
        id: String = "pay-x",
        amountCents: Long = 1000,
        deletedAt: Instant? = null,
    ) = Payment(
        id = id,
        jobId = "job-1",
        customerId = "cust-1",
        amountCents = amountCents,
        method = PaymentMethod.CASH,
        createdAt = Instant.parse("2026-06-28T10:00:00Z"),
        deletedAt = deletedAt,
    )

    private fun aCommand(
        jobId: String = "job-1",
        customerId: String = "cust-1",
        amountCents: Long = 2000,
    ) = CreatePaymentCommand(jobId, customerId, amountCents, PaymentMethod.CASH)

    @Test
    fun `should record a payment and recompute the job paid total`() {
        val paymentSlot = slot<Payment>()
        val jobSlot = slot<Job>()
        every { jobRepository.findById("job-1") } returns aJob() // total 3000
        every { paymentRepository.save(capture(paymentSlot)) } answers { paymentSlot.captured }
        // After saving the new 2000 payment, an earlier 1000 payment already exists -> sum 3000.
        every { paymentRepository.findAllByJobId("job-1") } returns
            listOf(aPayment(id = "pay-old", amountCents = 1000), aPayment(id = "pay-new", amountCents = 2000))
        every { jobRepository.save(capture(jobSlot)) } answers { jobSlot.captured }

        val result = service.create(aCommand())

        assertNotNull(result.id)
        assertEquals("cust-1", result.customerId)
        assertEquals(2000, result.amountCents)
        assertNull(result.deletedAt)
        // Job recomputed: paid 3000 of 3000 -> fully paid.
        assertEquals(3000, jobSlot.captured.amountPaidCents)
        assertTrue(jobSlot.captured.fullyPaid)
    }

    @Test
    fun `should leave the job not fully paid when payments fall short`() {
        val jobSlot = slot<Job>()
        every { jobRepository.findById("job-1") } returns aJob() // total 3000
        every { paymentRepository.save(any()) } answers { firstArg() }
        every { paymentRepository.findAllByJobId("job-1") } returns listOf(aPayment(amountCents = 1500))
        every { jobRepository.save(capture(jobSlot)) } answers { jobSlot.captured }

        service.create(aCommand(amountCents = 1500))

        assertEquals(1500, jobSlot.captured.amountPaidCents)
        assertFalse(jobSlot.captured.fullyPaid)
    }

    @Test
    fun `should reject a payment for a missing job`() {
        every { jobRepository.findById("job-1") } returns null

        assertThrows<JobNotFoundException> { service.create(aCommand()) }
        verify(exactly = 0) { paymentRepository.save(any()) }
        verify(exactly = 0) { jobRepository.save(any()) }
    }

    @Test
    fun `should reject a payment when the customer does not own the job`() {
        every { jobRepository.findById("job-1") } returns aJob(customerId = "cust-1")

        assertThrows<PaymentCustomerMismatchException> { service.create(aCommand(customerId = "other")) }
        verify(exactly = 0) { paymentRepository.save(any()) }
        verify(exactly = 0) { jobRepository.save(any()) }
    }

    @Test
    fun `should soft-delete a payment and recompute the job paid total`() {
        val paymentSlot = slot<Payment>()
        val jobSlot = slot<Job>()
        every { paymentRepository.findById("pay-1") } returns aPayment(id = "pay-1", amountCents = 2000)
        every { jobRepository.findById("job-1") } returns
            aJob(amountPaidCents = 3000, fullyPaid = true) // was fully paid
        every { paymentRepository.save(capture(paymentSlot)) } answers { paymentSlot.captured }
        // After deletion only a 1000 payment remains.
        every { paymentRepository.findAllByJobId("job-1") } returns listOf(aPayment(amountCents = 1000))
        every { jobRepository.save(capture(jobSlot)) } answers { jobSlot.captured }

        service.delete("pay-1")

        assertNotNull(paymentSlot.captured.deletedAt)
        assertEquals(1000, jobSlot.captured.amountPaidCents)
        assertFalse(jobSlot.captured.fullyPaid)
    }

    @Test
    fun `should throw when deleting a missing payment`() {
        every { paymentRepository.findById("pay-1") } returns null

        assertThrows<PaymentNotFoundException> { service.delete("pay-1") }
        verify(exactly = 0) { paymentRepository.save(any()) }
    }

    @Test
    fun `should get a payment by id`() {
        every { paymentRepository.findById("pay-1") } returns aPayment(id = "pay-1")

        assertEquals("pay-1", service.get("pay-1").id)
    }

    @Test
    fun `should throw when getting a missing payment`() {
        every { paymentRepository.findById("pay-1") } returns null

        assertThrows<PaymentNotFoundException> { service.get("pay-1") }
    }

    @Test
    fun `should list payments via the repository with filters`() {
        val page = PageResult(listOf(aPayment()), 1, 1, 0, 20)
        every { paymentRepository.findAll(0, 20, "job-1", null) } returns page

        val result = service.list(ListPaymentsQuery(0, 20, "job-1", null))

        assertEquals(1, result.totalElements)
        verify { paymentRepository.findAll(0, 20, "job-1", null) }
    }
}
