package com.stringpro.infrastructure.persistence.payment

import com.stringpro.application.domain.model.payment.Payment
import com.stringpro.application.domain.model.payment.PaymentMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import java.time.Instant
import java.util.UUID

@DataMongoTest
class PaymentRepositoryAdapterTest {
    @Autowired private lateinit var mongoRepository: PaymentMongoRepository

    @Autowired private lateinit var mongoTemplate: MongoTemplate
    private lateinit var adapter: PaymentRepositoryAdapter

    @BeforeEach
    fun setUp() {
        mongoRepository.deleteAll()
        adapter = PaymentRepositoryAdapter(mongoRepository, mongoTemplate)
    }

    @Test
    fun `should save and find payment by id`() {
        val saved = adapter.save(aPayment())

        val found = adapter.findById(saved.id)

        assertNotNull(found)
        assertEquals(saved.id, found!!.id)
        assertEquals("job-1", found.jobId)
        assertEquals("cust-1", found.customerId)
        assertEquals(1500, found.amountCents)
        assertEquals(PaymentMethod.CASH, found.method)
    }

    @Test
    fun `should not find soft-deleted payment by id`() {
        val saved = adapter.save(aPayment())
        adapter.save(saved.copy(deletedAt = Instant.now()))

        assertNull(adapter.findById(saved.id))
    }

    @Test
    fun `should exclude soft-deleted payments from list`() {
        adapter.save(aPayment())
        val toDelete = adapter.save(aPayment())
        adapter.save(toDelete.copy(deletedAt = Instant.now()))

        val result = adapter.findAll(0, 20, null, null)

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `should filter by job`() {
        adapter.save(aPayment(jobId = "job-1"))
        adapter.save(aPayment(jobId = "job-2"))

        val result = adapter.findAll(0, 20, "job-1", null)

        assertEquals(1, result.totalElements)
        assertEquals("job-1", result.content.single().jobId)
    }

    @Test
    fun `should filter by customer`() {
        adapter.save(aPayment(customerId = "cust-1"))
        adapter.save(aPayment(customerId = "cust-2"))

        val result = adapter.findAll(0, 20, null, "cust-1")

        assertEquals(1, result.totalElements)
        assertEquals("cust-1", result.content.single().customerId)
    }

    @Test
    fun `should return only non-deleted payments for a job`() {
        adapter.save(aPayment(jobId = "job-1"))
        val deleted = adapter.save(aPayment(jobId = "job-1"))
        adapter.save(deleted.copy(deletedAt = Instant.now()))
        adapter.save(aPayment(jobId = "job-2"))

        val result = adapter.findAllByJobId("job-1")

        assertEquals(1, result.size)
    }

    @Test
    fun `should sort by created date descending`() {
        adapter.save(aPayment(createdAt = Instant.parse("2026-06-01T10:00:00Z")))
        adapter.save(aPayment(createdAt = Instant.parse("2026-06-03T10:00:00Z")))
        adapter.save(aPayment(createdAt = Instant.parse("2026-06-02T10:00:00Z")))

        val result = adapter.findAll(0, 20, null, null)

        assertEquals(
            listOf(
                Instant.parse("2026-06-03T10:00:00Z"),
                Instant.parse("2026-06-02T10:00:00Z"),
                Instant.parse("2026-06-01T10:00:00Z"),
            ),
            result.content.map { it.createdAt },
        )
    }

    private fun aPayment(
        jobId: String = "job-1",
        customerId: String = "cust-1",
        createdAt: Instant = Instant.parse("2026-06-28T10:00:00Z"),
    ) = Payment(
        id = UUID.randomUUID().toString(),
        jobId = jobId,
        customerId = customerId,
        amountCents = 1500,
        method = PaymentMethod.CASH,
        createdAt = createdAt,
    )
}
