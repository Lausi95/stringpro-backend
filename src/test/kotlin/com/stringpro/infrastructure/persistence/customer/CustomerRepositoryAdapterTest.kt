package com.stringpro.infrastructure.persistence.customer

import com.stringpro.application.domain.model.customer.Customer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import java.time.Instant
import java.util.UUID

@DataMongoTest
class CustomerRepositoryAdapterTest {
    @Autowired private lateinit var mongoRepository: CustomerMongoRepository
    private lateinit var adapter: CustomerRepositoryAdapter

    @BeforeEach
    fun setUp() {
        mongoRepository.deleteAll()
        adapter = CustomerRepositoryAdapter(mongoRepository)
    }

    @Test
    fun `should save and find customer by id`() {
        val saved = adapter.save(aCustomer())

        val found = adapter.findById(saved.id)

        assertNotNull(found)
        assertEquals(saved.id, found!!.id)
        assertEquals("tom@example.com", found.email)
    }

    @Test
    fun `should find customer by email`() {
        adapter.save(aCustomer())

        val found = adapter.findByEmail("tom@example.com")

        assertNotNull(found)
        assertEquals("tom@example.com", found!!.email)
    }

    @Test
    fun `should not find soft-deleted customer by id`() {
        val saved = adapter.save(aCustomer())
        adapter.save(saved.copy(deletedAt = Instant.now()))

        val found = adapter.findById(saved.id)

        assertNull(found)
    }

    @Test
    fun `should not find soft-deleted customer by email`() {
        val saved = adapter.save(aCustomer())
        adapter.save(saved.copy(deletedAt = Instant.now()))

        val found = adapter.findByEmail("tom@example.com")

        assertNull(found)
    }

    @Test
    fun `should list only active customers`() {
        adapter.save(aCustomer(email = "active@example.com"))
        val toDelete = adapter.save(aCustomer(email = "deleted@example.com"))
        adapter.save(toDelete.copy(deletedAt = Instant.now()))

        val result = adapter.findAll(0, 20, null)

        assertEquals(1, result.totalElements)
        assertEquals("active@example.com", result.content[0].email)
    }

    @Test
    fun `should search by partial first name case-insensitively`() {
        adapter.save(aCustomer(firstName = "Tom", lastName = "Lausmann", email = "tom@example.com"))
        adapter.save(aCustomer(firstName = "Anna", lastName = "Schmidt", email = "anna@example.com"))

        val result = adapter.findAll(0, 20, "laus")

        assertEquals(1, result.totalElements)
        assertEquals("Tom", result.content[0].firstName)
    }

    @Test
    fun `should search by partial last name case-insensitively`() {
        adapter.save(aCustomer(firstName = "Tom", lastName = "Lausmann", email = "tom@example.com"))
        adapter.save(aCustomer(firstName = "Anna", lastName = "Schmidt", email = "anna@example.com"))

        val result = adapter.findAll(0, 20, "SCHMIDT")

        assertEquals(1, result.totalElements)
        assertEquals("Anna", result.content[0].firstName)
    }

    @Test
    fun `should return all customers when name filter is blank`() {
        adapter.save(aCustomer(email = "a@example.com"))
        adapter.save(aCustomer(email = "b@example.com"))

        val result = adapter.findAll(0, 20, "")

        assertEquals(2, result.totalElements)
    }

    private fun aCustomer(
        firstName: String = "Tom",
        lastName: String = "Lausmann",
        email: String = "tom@example.com",
    ) = Customer(
        id = UUID.randomUUID().toString(),
        firstName = firstName,
        lastName = lastName,
        email = email,
        phoneNumber = "+49123456",
        notes = null,
        createdAt = Instant.now(),
    )
}
