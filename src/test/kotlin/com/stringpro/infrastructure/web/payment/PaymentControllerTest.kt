package com.stringpro.infrastructure.web.payment

import com.ninjasquad.springmockk.MockkBean
import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.job.JobNotFoundException
import com.stringpro.application.domain.model.payment.Payment
import com.stringpro.application.domain.model.payment.PaymentCustomerMismatchException
import com.stringpro.application.domain.model.payment.PaymentMethod
import com.stringpro.application.domain.model.payment.PaymentNotFoundException
import com.stringpro.application.ports.`in`.payment.CreatePaymentCommand
import com.stringpro.application.ports.`in`.payment.CreatePaymentUseCase
import com.stringpro.application.ports.`in`.payment.DeletePaymentUseCase
import com.stringpro.application.ports.`in`.payment.GetPaymentUseCase
import com.stringpro.application.ports.`in`.payment.ListPaymentsQuery
import com.stringpro.application.ports.`in`.payment.ListPaymentsUseCase
import com.stringpro.infrastructure.config.SecurityConfig
import com.stringpro.infrastructure.web.GlobalExceptionHandler
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@WebMvcTest(PaymentController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class PaymentControllerTest {
    @Autowired private lateinit var mvc: MockMvc

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var jwtDecoder: JwtDecoder

    @MockkBean private lateinit var createPayment: CreatePaymentUseCase

    @MockkBean private lateinit var getPayment: GetPaymentUseCase

    @MockkBean private lateinit var listPayments: ListPaymentsUseCase

    @MockkBean private lateinit var deletePayment: DeletePaymentUseCase

    @Test
    fun `should record payment and return 201 with location and decimal amount`() {
        val slot = slot<CreatePaymentCommand>()
        every { createPayment.create(capture(slot)) } returns aPayment()

        mvc.post("/payments") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest())
        }.andExpect {
            status { isCreated() }
            header { string("Location", "/payments/pay-1") }
            jsonPath("$.id") { value("pay-1") }
            jsonPath("$.jobId") { value("job-1") }
            jsonPath("$.amount") { value(15.00) }
            jsonPath("$.method") { value("CASH") }
        }

        // Euros are converted to cents at the edge.
        assertEquals(1500, slot.captured.amountCents)
    }

    @Test
    fun `should return 400 when amount is zero`() {
        mvc.post("/payments") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest(amount = "0.00"))
        }.andExpect {
            status { isBadRequest() }
        }
        verify(exactly = 0) { createPayment.create(any()) }
    }

    @Test
    fun `should return 404 when job does not exist`() {
        every { createPayment.create(any()) } throws JobNotFoundException("job-1")

        mvc.post("/payments") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should return 409 when customer does not own the job`() {
        every { createPayment.create(any()) } throws PaymentCustomerMismatchException("job-1", "cust-9")

        mvc.post("/payments") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest(customerId = "cust-9"))
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `should list payments filtered by job`() {
        val slot = slot<ListPaymentsQuery>()
        every { listPayments.list(capture(slot)) } returns PageResult(listOf(aPayment()), 1, 1, 0, 20)

        mvc.get("/payments?jobId=job-1") { with(jwt()) }.andExpect {
            status { isOk() }
            jsonPath("$.content[0].id") { value("pay-1") }
            jsonPath("$.totalElements") { value(1) }
        }

        assertEquals("job-1", slot.captured.jobId)
    }

    @Test
    fun `should get payment by id`() {
        every { getPayment.get("pay-1") } returns aPayment()

        mvc.get("/payments/pay-1") { with(jwt()) }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value("pay-1") }
        }
    }

    @Test
    fun `should return 404 when getting a missing payment`() {
        every { getPayment.get("pay-1") } throws PaymentNotFoundException("pay-1")

        mvc.get("/payments/pay-1") { with(jwt()) }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should delete payment and return 204`() {
        every { deletePayment.delete("pay-1") } returns Unit

        mvc.delete("/payments/pay-1") { with(jwt()) }.andExpect {
            status { isNoContent() }
        }
        verify { deletePayment.delete("pay-1") }
    }

    private fun aCreateRequest(
        customerId: String = "cust-1",
        amount: String = "15.00",
    ) = mapOf(
        "jobId" to "job-1",
        "customerId" to customerId,
        "amount" to amount,
        "method" to "CASH",
    )

    private fun aPayment() =
        Payment(
            id = "pay-1",
            jobId = "job-1",
            customerId = "cust-1",
            amountCents = 1500,
            method = PaymentMethod.CASH,
            createdAt = Instant.parse("2026-06-28T10:00:00Z"),
        )
}
