package com.stringpro.infrastructure.persistence.settings

import com.stringpro.application.domain.model.settings.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import java.time.Instant

@DataMongoTest
class SettingsRepositoryAdapterTest {
    @Autowired private lateinit var mongoRepository: SettingsMongoRepository

    @Autowired private lateinit var mongoTemplate: org.springframework.data.mongodb.core.MongoTemplate
    private lateinit var adapter: SettingsRepositoryAdapter

    @BeforeEach
    fun setUp() {
        mongoRepository.deleteAll()
        adapter = SettingsRepositoryAdapter(mongoRepository)
    }

    @Test
    fun `should return null when nothing has been saved`() {
        assertNull(adapter.find())
    }

    @Test
    fun `should save and find settings`() {
        adapter.save(aSettings())

        val found = adapter.find()

        assertNotNull(found)
        assertEquals(1550, found!!.serviceFeeCents)
        assertEquals("Jane Stringer", found.fullName)
        assertEquals("JaneStringer", found.paypalHandle)
        assertEquals("DE89370400440532013000", found.iban)
        assertEquals("123 Court St", found.address)
        assertEquals(Instant.EPOCH, found.updatedAt)
    }

    @Test
    fun `should keep exactly one document across repeated saves`() {
        adapter.save(aSettings(serviceFeeCents = 1000))
        adapter.save(aSettings(serviceFeeCents = 2000))
        adapter.save(aSettings(serviceFeeCents = 3000))

        assertEquals(1L, mongoRepository.count())
        assertEquals(3000, adapter.find()!!.serviceFeeCents)
    }

    @Test
    fun `should default paypalHandle to blank when reading a document that predates the field`() {
        // Simulate a document written before the email→paypalHandle rename: no paypalHandle key.
        mongoTemplate.save(
            org.bson.Document(
                mapOf(
                    "_id" to "settings",
                    "serviceFeeCents" to 1550L,
                    "fullName" to "Jane Stringer",
                    "email" to "jane@example.com",
                    "iban" to "DE89370400440532013000",
                    "address" to "123 Court St",
                ),
            ),
            "settings",
        )

        val found = adapter.find()

        assertNotNull(found)
        assertEquals("", found!!.paypalHandle)
        assertEquals("Jane Stringer", found.fullName)
    }

    private fun aSettings(serviceFeeCents: Long = 1550) =
        Settings(
            serviceFeeCents = serviceFeeCents,
            fullName = "Jane Stringer",
            paypalHandle = "JaneStringer",
            iban = "DE89370400440532013000",
            address = "123 Court St",
            updatedAt = Instant.EPOCH,
        )
}
