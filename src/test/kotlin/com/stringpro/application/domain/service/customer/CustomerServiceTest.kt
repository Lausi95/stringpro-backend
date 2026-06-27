package com.stringpro.application.domain.service.customer

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.customer.Customer
import com.stringpro.application.domain.model.customer.CustomerNotFoundException
import com.stringpro.application.domain.model.customer.EmailAlreadyExistsException
import com.stringpro.application.ports.`in`.customer.CreateCustomerCommand
import com.stringpro.application.ports.`in`.customer.ListCustomersQuery
import com.stringpro.application.ports.`in`.customer.UpdateCustomerCommand
import com.stringpro.application.ports.out.customer.CustomerRepository
import io.mockk.every
import java.time.Instant
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CustomerServiceTest {

    private val customerRepository: CustomerRepository = mockk()
    private val service = CustomerService(customerRepository)

    @Test
    fun `should create customer when email is not taken`() {
        val slot = slot<Customer>()
        every { customerRepository.findByEmail("tom@example.com") } returns null
        every { customerRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.create(
            CreateCustomerCommand("Tom", "Lausmann", "tom@example.com", "+49123456", null),
        )

        assertNotNull(result.id)
        assertEquals("Tom", result.firstName)
        assertEquals("tom@example.com", result.email)
        assertNull(result.deletedAt)
    }

    @Test
    fun `should throw EmailAlreadyExistsException when email is taken on create`() {
        every { customerRepository.findByEmail("tom@example.com") } returns aCustomer()

        assertThrows<EmailAlreadyExistsException> {
            service.create(
                CreateCustomerCommand("Tom", "Lausmann", "tom@example.com", "+49123456", null),
            )
        }
    }

    @Test
    fun `should get customer by id`() {
        every { customerRepository.findById("id-1") } returns aCustomer(id = "id-1")

        val result = service.get("id-1")

        assertEquals("id-1", result.id)
    }

    @Test
    fun `should throw CustomerNotFoundException when customer does not exist`() {
        every { customerRepository.findById("unknown") } returns null

        assertThrows<CustomerNotFoundException> { service.get("unknown") }
    }

    @Test
    fun `should list customers`() {
        every { customerRepository.findAll(0, 20, null) } returns PageResult(listOf(aCustomer()), 1L, 1, 0, 20)

        val result = service.list(ListCustomersQuery(0, 20, null))

        assertEquals(1, result.content.size)
    }

    @Test
    fun `should update customer when email changes to available address`() {
        val slot = slot<Customer>()
        every { customerRepository.findById("id-1") } returns aCustomer(id = "id-1", email = "old@example.com")
        every { customerRepository.findByEmail("new@example.com") } returns null
        every { customerRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.update(
            "id-1",
            UpdateCustomerCommand("Tom", "Lausmann", "new@example.com", "+49", null),
        )

        assertEquals("new@example.com", result.email)
    }

    @Test
    fun `should update customer when email is unchanged`() {
        val slot = slot<Customer>()
        every { customerRepository.findById("id-1") } returns aCustomer(id = "id-1")
        every { customerRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.update(
            "id-1",
            UpdateCustomerCommand("Tom", "Updated", "tom@example.com", "+49", null),
        )

        assertEquals("Updated", result.lastName)
    }

    @Test
    fun `should throw EmailAlreadyExistsException when updating to a taken email`() {
        every { customerRepository.findById("id-1") } returns aCustomer(id = "id-1", email = "old@example.com")
        every { customerRepository.findByEmail("taken@example.com") } returns aCustomer(id = "id-2")

        assertThrows<EmailAlreadyExistsException> {
            service.update(
                "id-1",
                UpdateCustomerCommand("Tom", "Lausmann", "taken@example.com", "+49", null),
            )
        }
    }

    @Test
    fun `should soft delete customer`() {
        val slot = slot<Customer>()
        every { customerRepository.findById("id-1") } returns aCustomer(id = "id-1")
        every { customerRepository.save(capture(slot)) } answers { slot.captured }

        service.delete("id-1")

        assertNotNull(slot.captured.deletedAt)
    }

    @Test
    fun `should throw CustomerNotFoundException when deleting non-existent customer`() {
        every { customerRepository.findById("unknown") } returns null

        assertThrows<CustomerNotFoundException> { service.delete("unknown") }
    }

    private fun aCustomer(id: String = "id-1", email: String = "tom@example.com") = Customer(
        id = id,
        firstName = "Tom",
        lastName = "Lausmann",
        email = email,
        phoneNumber = "+49123456",
        notes = null,
        createdAt = Instant.now(),
    )
}
