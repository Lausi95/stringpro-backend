package com.stringpro.infrastructure.web.customer

import tools.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.customer.Customer
import com.stringpro.application.domain.model.customer.CustomerNotFoundException
import com.stringpro.application.domain.model.customer.EmailAlreadyExistsException
import com.stringpro.application.ports.`in`.customer.CreateCustomerUseCase
import com.stringpro.application.ports.`in`.customer.DeleteCustomerUseCase
import com.stringpro.application.ports.`in`.customer.GetCustomerUseCase
import com.stringpro.application.ports.`in`.customer.ListCustomersUseCase
import com.stringpro.application.ports.`in`.customer.UpdateCustomerUseCase
import com.stringpro.infrastructure.config.SecurityConfig
import com.stringpro.infrastructure.web.GlobalExceptionHandler
import io.mockk.every
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
import org.springframework.test.web.servlet.put

@WebMvcTest(CustomerController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class CustomerControllerTest {

    @Autowired private lateinit var mvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var jwtDecoder: JwtDecoder
    @MockkBean private lateinit var createCustomer: CreateCustomerUseCase
    @MockkBean private lateinit var getCustomer: GetCustomerUseCase
    @MockkBean private lateinit var listCustomers: ListCustomersUseCase
    @MockkBean private lateinit var updateCustomer: UpdateCustomerUseCase
    @MockkBean private lateinit var deleteCustomer: DeleteCustomerUseCase

    @Test
    fun `should create customer and return 201 with location header`() {
        every { createCustomer.create(any()) } returns aCustomer()

        mvc.post("/customers") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest())
        }.andExpect {
            status { isCreated() }
            header { string("Location", "/customers/id-1") }
            jsonPath("$.id") { value("id-1") }
            jsonPath("$.email") { value("tom@example.com") }
        }
    }

    @Test
    fun `should return 409 when email already exists on create`() {
        every { createCustomer.create(any()) } throws EmailAlreadyExistsException("tom@example.com")

        mvc.post("/customers") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest())
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    fun `should return 400 when required field is missing on create`() {
        mvc.post("/customers") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = """{"firstName": "Tom"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should list customers with pagination`() {
        every { listCustomers.list(any()) } returns PageResult(listOf(aCustomer()), 1L, 1, 0, 20)

        mvc.get("/customers") {
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$.content[0].id") { value("id-1") }
            jsonPath("$.totalElements") { value(1) }
            jsonPath("$.page") { value(0) }
        }
    }

    @Test
    fun `should get customer by id`() {
        every { getCustomer.get("id-1") } returns aCustomer()

        mvc.get("/customers/id-1") {
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value("id-1") }
        }
    }

    @Test
    fun `should return 404 when customer not found`() {
        every { getCustomer.get("unknown") } throws CustomerNotFoundException("unknown")

        mvc.get("/customers/unknown") {
            with(jwt())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should update customer and return 200`() {
        every { updateCustomer.update(any(), any()) } returns aCustomer()

        mvc.put("/customers/id-1") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aUpdateRequest())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value("id-1") }
        }
    }

    @Test
    fun `should delete customer and return 204`() {
        every { deleteCustomer.delete("id-1") } returns Unit

        mvc.delete("/customers/id-1") {
            with(jwt())
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mvc.get("/customers").andExpect { status { isUnauthorized() } }
    }

    private fun aCustomer() = Customer(
        id = "id-1",
        firstName = "Tom",
        lastName = "Lausmann",
        email = "tom@example.com",
        phoneNumber = "+49123456",
        notes = null,
        createdAt = java.time.Instant.EPOCH,
    )

    private fun aCreateRequest() = CreateCustomerRequest(
        firstName = "Tom",
        lastName = "Lausmann",
        email = "tom@example.com",
        phoneNumber = "+49123456",
        notes = null,
    )

    private fun aUpdateRequest() = UpdateCustomerRequest(
        firstName = "Tom",
        lastName = "Lausmann",
        email = "tom@example.com",
        phoneNumber = "+49123456",
        notes = null,
    )
}
