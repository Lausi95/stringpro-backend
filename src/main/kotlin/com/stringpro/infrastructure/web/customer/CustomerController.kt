package com.stringpro.infrastructure.web.customer

import com.stringpro.application.ports.`in`.customer.CreateCustomerCommand
import com.stringpro.application.ports.`in`.customer.CreateCustomerUseCase
import com.stringpro.application.ports.`in`.customer.DeleteCustomerUseCase
import com.stringpro.application.ports.`in`.customer.GetCustomerUseCase
import com.stringpro.application.ports.`in`.customer.ListCustomersQuery
import com.stringpro.application.ports.`in`.customer.ListCustomersUseCase
import com.stringpro.application.ports.`in`.customer.UpdateCustomerCommand
import com.stringpro.application.ports.`in`.customer.UpdateCustomerUseCase
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/customers")
@Tag(name = "Customers")
class CustomerController(
    private val createCustomer: CreateCustomerUseCase,
    private val getCustomer: GetCustomerUseCase,
    private val listCustomers: ListCustomersUseCase,
    private val updateCustomer: UpdateCustomerUseCase,
    private val deleteCustomer: DeleteCustomerUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    @Operation(summary = "Create a customer")
    @ApiResponse(responseCode = "201", description = "Customer created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    fun create(@Valid @RequestBody request: CreateCustomerRequest): ResponseEntity<CustomerResponse> {
        val customer = createCustomer.create(
            CreateCustomerCommand(
                firstName = request.firstName,
                lastName = request.lastName,
                email = request.email,
                phoneNumber = request.phoneNumber,
                notes = request.notes,
            ),
        )
        MDC.put("customerId", customer.id)
        log.info("Customer created")
        return ResponseEntity
            .created(URI.create("/customers/${customer.id}"))
            .body(customer.toResponse())
    }

    @GetMapping
    @Operation(summary = "List customers")
    @ApiResponse(responseCode = "200", description = "OK")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) name: String?,
    ): PagedCustomerResponse =
        listCustomers.list(ListCustomersQuery(page, size, name)).toResponse()

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer by ID")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    fun get(@PathVariable id: String): CustomerResponse =
        getCustomer.get(id).toResponse()

    @PutMapping("/{id}")
    @Operation(summary = "Update a customer")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateCustomerRequest,
    ): CustomerResponse {
        MDC.put("customerId", id)
        return updateCustomer.update(
            id,
            UpdateCustomerCommand(
                firstName = request.firstName,
                lastName = request.lastName,
                email = request.email,
                phoneNumber = request.phoneNumber,
                notes = request.notes,
            ),
        ).toResponse()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer")
    @ApiResponse(responseCode = "204", description = "Customer deleted")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        MDC.put("customerId", id)
        deleteCustomer.delete(id)
        return ResponseEntity.noContent().build()
    }
}
