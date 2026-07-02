package com.stringpro.infrastructure.web.settings

import com.ninjasquad.springmockk.MockkBean
import com.stringpro.application.domain.model.settings.Settings
import com.stringpro.application.ports.`in`.settings.GetSettingsUseCase
import com.stringpro.application.ports.`in`.settings.UpdateSettingsCommand
import com.stringpro.application.ports.`in`.settings.UpdateSettingsUseCase
import com.stringpro.infrastructure.config.SecurityConfig
import com.stringpro.infrastructure.web.GlobalExceptionHandler
import io.mockk.every
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Instant

@WebMvcTest(SettingsController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class SettingsControllerTest {
    @Autowired private lateinit var mvc: MockMvc

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var jwtDecoder: JwtDecoder

    @MockkBean private lateinit var getSettings: GetSettingsUseCase

    @MockkBean private lateinit var updateSettings: UpdateSettingsUseCase

    @Test
    fun `should return settings with 200 and decimal service fee`() {
        every { getSettings.get() } returns aSettings()

        mvc.get("/settings") {
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$.serviceFee") { value(15.50) }
            jsonPath("$.fullName") { value("Jane Stringer") }
            jsonPath("$.paypalHandle") { value("JaneStringer") }
            jsonPath("$.iban") { value("DE89370400440532013000") }
            jsonPath("$.address") { value("123 Court St") }
        }
    }

    @Test
    fun `should return defaults before anything is saved`() {
        every { getSettings.get() } returns Settings.DEFAULT

        mvc.get("/settings") {
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$.serviceFee") { value(0.0) }
            jsonPath("$.fullName") { value("") }
            jsonPath("$.updatedAt") { value(null) }
        }
    }

    @Test
    fun `should update settings and return 200`() {
        every { updateSettings.update(any()) } returns aSettings()

        mvc.put("/settings") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aUpdateRequest())
        }.andExpect {
            status { isOk() }
            jsonPath("$.fullName") { value("Jane Stringer") }
        }
    }

    @Test
    fun `should convert decimal euro service fee to integer cents`() {
        val slot = slot<UpdateSettingsCommand>()
        every { updateSettings.update(capture(slot)) } returns aSettings()

        mvc.put("/settings") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aUpdateRequest())
        }.andExpect { status { isOk() } }

        assertEquals(1550, slot.captured.serviceFeeCents)
    }

    @Test
    fun `should accept blank optional identity fields`() {
        val slot = slot<UpdateSettingsCommand>()
        every { updateSettings.update(capture(slot)) } returns Settings.DEFAULT

        mvc.put("/settings") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = """{"serviceFee": 15.50}"""
        }.andExpect { status { isOk() } }

        assertEquals("", slot.captured.fullName)
        assertEquals("", slot.captured.iban)
    }

    @Test
    fun `should normalise a spaced lower-case iban before storing`() {
        val slot = slot<UpdateSettingsCommand>()
        every { updateSettings.update(capture(slot)) } returns aSettings()

        mvc.put("/settings") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aUpdateRequest().copy(iban = "de89 3704 0044 0532 0130 00"))
        }.andExpect { status { isOk() } }

        assertEquals("DE89370400440532013000", slot.captured.iban)
    }

    @Test
    fun `should return 400 when iban fails the checksum`() {
        mvc.put("/settings") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aUpdateRequest().copy(iban = "DE89370400440532013001"))
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `should trim surrounding whitespace from the paypal handle before storing`() {
        val slot = slot<UpdateSettingsCommand>()
        every { updateSettings.update(capture(slot)) } returns aSettings()

        mvc.put("/settings") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aUpdateRequest().copy(paypalHandle = "  JaneStringer  "))
        }.andExpect { status { isOk() } }

        assertEquals("JaneStringer", slot.captured.paypalHandle)
    }

    @Test
    fun `should return 400 when the paypal handle contains illegal characters`() {
        mvc.put("/settings") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aUpdateRequest().copy(paypalHandle = "@JaneStringer"))
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `should return 400 when service fee is negative`() {
        mvc.put("/settings") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aUpdateRequest().copy(serviceFee = BigDecimal("-1.00")))
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mvc.get("/settings") {
        }.andExpect { status { isUnauthorized() } }
    }

    private fun aSettings() =
        Settings(
            serviceFeeCents = 1550,
            fullName = "Jane Stringer",
            paypalHandle = "JaneStringer",
            iban = "DE89370400440532013000",
            address = "123 Court St",
            updatedAt = Instant.EPOCH,
        )

    private fun aUpdateRequest() =
        UpdateSettingsRequest(
            serviceFee = BigDecimal("15.50"),
            fullName = "Jane Stringer",
            paypalHandle = "JaneStringer",
            iban = "DE89370400440532013000",
            address = "123 Court St",
        )
}
