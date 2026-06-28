package com.stringpro.application.domain.service.settings

import com.stringpro.application.domain.model.settings.Settings
import com.stringpro.application.ports.`in`.settings.UpdateSettingsCommand
import com.stringpro.application.ports.out.settings.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class SettingsServiceTest {
    private val settingsRepository: SettingsRepository = mockk()
    private val service = SettingsService(settingsRepository)

    @Test
    fun `should return defaults when nothing has been saved`() {
        every { settingsRepository.find() } returns null

        val result = service.get()

        assertEquals(0, result.serviceFeeCents)
        assertEquals("", result.fullName)
        assertEquals("", result.email)
        assertEquals("", result.iban)
        assertEquals("", result.address)
        assertNull(result.updatedAt)
    }

    @Test
    fun `should return saved settings when present`() {
        every { settingsRepository.find() } returns
            Settings(
                serviceFeeCents = 2000,
                fullName = "Jane Stringer",
                email = "jane@example.com",
                iban = "DE89370400440532013000",
                address = "123 Court St",
                updatedAt = Instant.EPOCH,
            )

        val result = service.get()

        assertEquals(2000, result.serviceFeeCents)
        assertEquals("Jane Stringer", result.fullName)
    }

    @Test
    fun `should map command and stamp updatedAt on update`() {
        val slot = slot<Settings>()
        every { settingsRepository.save(capture(slot)) } answers { slot.captured }

        val result =
            service.update(
                UpdateSettingsCommand(
                    serviceFeeCents = 1550,
                    fullName = "Jane Stringer",
                    email = "jane@example.com",
                    iban = "DE89370400440532013000",
                    address = "123 Court St",
                ),
            )

        assertEquals(1550, result.serviceFeeCents)
        assertEquals("Jane Stringer", result.fullName)
        assertEquals("jane@example.com", result.email)
        assertEquals("DE89370400440532013000", result.iban)
        assertEquals("123 Court St", result.address)
        assertNotNull(result.updatedAt)
    }
}
