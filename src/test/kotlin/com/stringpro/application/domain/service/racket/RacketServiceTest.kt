package com.stringpro.application.domain.service.racket

import com.stringpro.application.domain.model.customer.Customer
import com.stringpro.application.domain.model.customer.CustomerNotFoundException
import com.stringpro.application.domain.model.racket.Racket
import com.stringpro.application.domain.model.racket.RacketNotFoundException
import com.stringpro.application.ports.`in`.racket.CreateRacketCommand
import com.stringpro.application.ports.`in`.racket.ListRacketsQuery
import com.stringpro.application.ports.`in`.racket.UpdateRacketCommand
import com.stringpro.application.ports.out.customer.CustomerRepository
import com.stringpro.application.ports.out.racket.RacketRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class RacketServiceTest {
    private val racketRepository: RacketRepository = mockk()
    private val customerRepository: CustomerRepository = mockk()
    private val service = RacketService(racketRepository, customerRepository)

    @Test
    fun `should create racket when customer exists`() {
        val slot = slot<Racket>()
        every { customerRepository.findById("cust-1") } returns aCustomer()
        every { racketRepository.save(capture(slot)) } answers { slot.captured }

        val result =
            service.create(
                CreateRacketCommand("cust-1", "Babolat", "Pure Aero", 645, 16, 19, null),
            )

        assertNotNull(result.id)
        assertEquals("cust-1", result.customerId)
        assertEquals("Babolat", result.brand)
        assertEquals(645, result.headSize)
        assertEquals(16, result.stringMains)
        assertEquals(19, result.stringCrosses)
        assertNull(result.deletedAt)
    }

    @Test
    fun `should throw CustomerNotFoundException when creating racket for non-existent customer`() {
        every { customerRepository.findById("unknown") } returns null

        assertThrows<CustomerNotFoundException> {
            service.create(CreateRacketCommand("unknown", "Babolat", "Pure Aero", 645, 16, 19, null))
        }
        verify(exactly = 0) { racketRepository.save(any()) }
    }

    @Test
    fun `should get racket by id`() {
        every { racketRepository.findById("id-1") } returns aRacket(id = "id-1")

        val result = service.get("id-1")

        assertEquals("id-1", result.id)
    }

    @Test
    fun `should throw RacketNotFoundException when racket does not exist`() {
        every { racketRepository.findById("unknown") } returns null

        assertThrows<RacketNotFoundException> { service.get("unknown") }
    }

    @Test
    fun `should list rackets for a customer`() {
        every { racketRepository.findByCustomerId("cust-1") } returns listOf(aRacket(), aRacket(id = "id-2"))

        val result = service.list(ListRacketsQuery("cust-1"))

        assertEquals(2, result.size)
    }

    @Test
    fun `should return empty list when customer has no rackets`() {
        every { racketRepository.findByCustomerId("cust-1") } returns emptyList()

        val result = service.list(ListRacketsQuery("cust-1"))

        assertEquals(0, result.size)
    }

    @Test
    fun `should update racket attributes`() {
        val slot = slot<Racket>()
        every { racketRepository.findById("id-1") } returns aRacket(id = "id-1")
        every { racketRepository.save(capture(slot)) } answers { slot.captured }

        val result =
            service.update(
                "id-1",
                UpdateRacketCommand("Wilson", "Blade", 626, 18, 20, "stiffened"),
            )

        assertEquals("Wilson", result.brand)
        assertEquals("Blade", result.model)
        assertEquals(626, result.headSize)
        assertEquals(18, result.stringMains)
        assertEquals(20, result.stringCrosses)
        assertEquals("stiffened", result.notes)
    }

    @Test
    fun `should not change customerId on update`() {
        val slot = slot<Racket>()
        every { racketRepository.findById("id-1") } returns aRacket(id = "id-1", customerId = "cust-1")
        every { racketRepository.save(capture(slot)) } answers { slot.captured }

        val result =
            service.update(
                "id-1",
                UpdateRacketCommand("Wilson", "Blade", 626, 18, 20, null),
            )

        assertEquals("cust-1", result.customerId)
    }

    @Test
    fun `should throw RacketNotFoundException when updating non-existent racket`() {
        every { racketRepository.findById("unknown") } returns null

        assertThrows<RacketNotFoundException> {
            service.update("unknown", UpdateRacketCommand("Wilson", "Blade", 626, 18, 20, null))
        }
    }

    @Test
    fun `should soft delete racket`() {
        val slot = slot<Racket>()
        every { racketRepository.findById("id-1") } returns aRacket(id = "id-1")
        every { racketRepository.save(capture(slot)) } answers { slot.captured }

        service.delete("id-1")

        assertNotNull(slot.captured.deletedAt)
    }

    @Test
    fun `should throw RacketNotFoundException when deleting non-existent racket`() {
        every { racketRepository.findById("unknown") } returns null

        assertThrows<RacketNotFoundException> { service.delete("unknown") }
    }

    private fun aCustomer(id: String = "cust-1") =
        Customer(
            id = id,
            firstName = "Tom",
            lastName = "Lausmann",
            email = "tom@example.com",
            phoneNumber = "+49123456",
            notes = null,
            createdAt = Instant.now(),
        )

    private fun aRacket(
        id: String = "id-1",
        customerId: String = "cust-1",
    ) = Racket(
        id = id,
        customerId = customerId,
        brand = "Babolat",
        model = "Pure Aero",
        headSize = 645,
        stringMains = 16,
        stringCrosses = 19,
        notes = null,
        createdAt = Instant.now(),
    )
}
