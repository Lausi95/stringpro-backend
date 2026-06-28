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
        assertEquals("jane@example.com", found.email)
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

    private fun aSettings(serviceFeeCents: Long = 1550) =
        Settings(
            serviceFeeCents = serviceFeeCents,
            fullName = "Jane Stringer",
            email = "jane@example.com",
            iban = "DE89370400440532013000",
            address = "123 Court St",
            updatedAt = Instant.EPOCH,
        )
}
