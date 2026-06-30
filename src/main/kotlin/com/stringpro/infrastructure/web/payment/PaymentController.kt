package com.stringpro.infrastructure.web.payment

import com.stringpro.application.ports.`in`.payment.CreatePaymentCommand
import com.stringpro.application.ports.`in`.payment.CreatePaymentUseCase
import com.stringpro.application.ports.`in`.payment.DeletePaymentUseCase
import com.stringpro.application.ports.`in`.payment.GetPaymentUseCase
import com.stringpro.application.ports.`in`.payment.ListPaymentsQuery
import com.stringpro.application.ports.`in`.payment.ListPaymentsUseCase
import com.stringpro.infrastructure.web.eurosToCents
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments")
class PaymentController(
    private val createPayment: CreatePaymentUseCase,
    private val getPayment: GetPaymentUseCase,
    private val listPayments: ListPaymentsUseCase,
    private val deletePayment: DeletePaymentUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    @Operation(summary = "Record a payment for a job")
    @ApiResponse(responseCode = "201", description = "Payment recorded")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "404", description = "Job not found")
    @ApiResponse(responseCode = "409", description = "Customer does not own the job")
    fun create(
        @Valid @RequestBody request: CreatePaymentRequest,
    ): ResponseEntity<PaymentResponse> {
        val payment =
            createPayment.create(
                CreatePaymentCommand(
                    jobId = request.jobId,
                    customerId = request.customerId,
                    amountCents = eurosToCents(request.amount!!),
                    method = request.method!!,
                ),
            )
        MDC.put("paymentId", payment.id)
        MDC.put("jobId", payment.jobId)
        log.info("Payment recorded")
        return ResponseEntity
            .created(URI.create("/payments/${payment.id}"))
            .body(payment.toResponse())
    }

    @GetMapping
    @Operation(summary = "List payments (paginated), optionally filtered by job and customer")
    @ApiResponse(responseCode = "200", description = "OK")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) jobId: String?,
        @RequestParam(required = false) customerId: String?,
    ): PagedPaymentResponse = listPayments.list(ListPaymentsQuery(page, size, jobId, customerId)).toResponse()

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment by ID")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    fun get(
        @PathVariable id: String,
    ): PaymentResponse {
        MDC.put("paymentId", id)
        return getPayment.get(id).toResponse()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a payment")
    @ApiResponse(responseCode = "204", description = "Payment deleted")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    fun delete(
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        MDC.put("paymentId", id)
        deletePayment.delete(id)
        return ResponseEntity.noContent().build()
    }
}
