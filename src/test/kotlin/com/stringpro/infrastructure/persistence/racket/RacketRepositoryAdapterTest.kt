package com.stringpro.infrastructure.persistence.racket

import com.stringpro.application.domain.model.racket.Racket
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import java.time.Instant
import java.util.UUID

@DataMongoTest
class RacketRepositoryAdapterTest {
    @Autowired private lateinit var mongoRepository: RacketMongoRepository
    private lateinit var adapter: RacketRepositoryAdapter

    @BeforeEach
    fun setUp() {
        mongoRepository.deleteAll()
        adapter = RacketRepositoryAdapter(mongoRepository)
    }

    @Test
    fun `should save and find racket by id`() {
        val saved = adapter.save(aRacket())

        val found = adapter.findById(saved.id)

        assertNotNull(found)
        assertEquals(saved.id, found!!.id)
        assertEquals("Babolat", found.brand)
        assertEquals(16, found.stringMains)
        assertEquals(19, found.stringCrosses)
    }

    @Test
    fun `should not find soft-deleted racket by id`() {
        val saved = adapter.save(aRacket())
        adapter.save(saved.copy(deletedAt = Instant.now()))

        val found = adapter.findById(saved.id)

        assertNull(found)
    }

    @Test
    fun `should find all rackets for a customer`() {
        adapter.save(aRacket(customerId = "cust-1"))
        adapter.save(aRacket(customerId = "cust-1"))
        adapter.save(aRacket(customerId = "cust-2"))

        val result = adapter.findByCustomerId("cust-1")

        assertEquals(2, result.size)
        assertTrue(result.all { it.customerId == "cust-1" })
    }

    @Test
    fun `should not return soft-deleted rackets for a customer`() {
        adapter.save(aRacket(customerId = "cust-1"))
        val toDelete = adapter.save(aRacket(customerId = "cust-1"))
        adapter.save(toDelete.copy(deletedAt = Instant.now()))

        val result = adapter.findByCustomerId("cust-1")

        assertEquals(1, result.size)
    }

    @Test
    fun `should return empty list when customer has no rackets`() {
        adapter.save(aRacket(customerId = "cust-1"))

        val result = adapter.findByCustomerId("cust-2")

        assertEquals(0, result.size)
    }

    private fun aRacket(customerId: String = "cust-1") =
        Racket(
            id = UUID.randomUUID().toString(),
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
