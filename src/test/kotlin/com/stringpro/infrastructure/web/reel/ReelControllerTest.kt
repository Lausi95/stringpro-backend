package com.stringpro.infrastructure.web.reel

import com.ninjasquad.springmockk.MockkBean
import com.stringpro.application.domain.model.reel.Material
import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelNotFoundException
import com.stringpro.application.domain.model.reel.ReelState
import com.stringpro.application.ports.`in`.reel.ChangeReelStateUseCase
import com.stringpro.application.ports.`in`.reel.CreateReelUseCase
import com.stringpro.application.ports.`in`.reel.DeleteReelUseCase
import com.stringpro.application.ports.`in`.reel.GetReelUseCase
import com.stringpro.application.ports.`in`.reel.ListReelsQuery
import com.stringpro.application.ports.`in`.reel.ListReelsUseCase
import com.stringpro.application.ports.`in`.reel.UpdateReelUseCase
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@WebMvcTest(ReelController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class ReelControllerTest {
    @Autowired private lateinit var mvc: MockMvc

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var jwtDecoder: JwtDecoder

    @MockkBean private lateinit var createReel: CreateReelUseCase

    @MockkBean private lateinit var getReel: GetReelUseCase

    @MockkBean private lateinit var listReels: ListReelsUseCase

    @MockkBean private lateinit var updateReel: UpdateReelUseCase

    @MockkBean private lateinit var changeReelState: ChangeReelStateUseCase

    @MockkBean private lateinit var deleteReel: DeleteReelUseCase

    @Test
    fun `should create reel and return 201 with location header and decimal fields`() {
        every { createReel.create(any()) } returns aReel()

        mvc.post("/reels") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest())
        }.andExpect {
            status { isCreated() }
            header { string("Location", "/reels/id-1") }
            jsonPath("$.id") { value("id-1") }
            jsonPath("$.material") { value("POLYESTER") }
            jsonPath("$.gauge") { value(1.25) }
            jsonPath("$.cost") { value(120.00) }
            jsonPath("$.stringFee") { value(25.00) }
            jsonPath("$.state") { value("NEW") }
        }
    }

    @Test
    fun `should convert decimal euros and millimetres to integer minor units`() {
        val slot = slot<com.stringpro.application.ports.`in`.reel.CreateReelCommand>()
        every { createReel.create(capture(slot)) } returns aReel()

        mvc.post("/reels") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest())
        }.andExpect { status { isCreated() } }

        assertEquals(125, slot.captured.gaugeHundredthsMm)
        assertEquals(12000, slot.captured.costCents)
        assertEquals(2500, slot.captured.stringFeeCents)
    }

    @Test
    fun `should return 400 when required field is missing on create`() {
        mvc.post("/reels") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = """{"brand": "Luxilon"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should return 400 when material is not a known value`() {
        mvc.post("/reels") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest()).replace("POLYESTER", "GRAPHITE")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should return 400 when gauge is not positive`() {
        mvc.post("/reels") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest().copy(gauge = BigDecimal("0.00")))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should return 400 when purchase date is in the future`() {
        mvc.post("/reels") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest().copy(purchaseDate = LocalDate.now().plusDays(1)))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should list all reels when no state filter`() {
        val slot = slot<ListReelsQuery>()
        every { listReels.list(capture(slot)) } returns listOf(aReel())

        mvc.get("/reels") {
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value("id-1") }
        }
        assertEquals(null, slot.captured.state)
    }

    @Test
    fun `should list reels filtered by state`() {
        val slot = slot<ListReelsQuery>()
        every { listReels.list(capture(slot)) } returns listOf(aReel(state = ReelState.IN_USE))

        mvc.get("/reels") {
            with(jwt())
            param("state", "IN_USE")
        }.andExpect {
            status { isOk() }
        }
        assertEquals(ReelState.IN_USE, slot.captured.state)
    }

    @Test
    fun `should return 400 problem detail when state filter is not a known value`() {
        mvc.get("/reels") {
            with(jwt())
            param("state", "BROKEN")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.detail") { value("Invalid value for parameter 'state'") }
        }
    }

    @Test
    fun `should get reel by id`() {
        every { getReel.get("id-1") } returns aReel()

        mvc.get("/reels/id-1") {
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value("id-1") }
        }
    }

    @Test
    fun `should return 404 when reel not found`() {
        every { getReel.get("unknown") } throws ReelNotFoundException("unknown")

        mvc.get("/reels/unknown") {
            with(jwt())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should update reel and return 200`() {
        every { updateReel.update(any(), any()) } returns aReel()

        mvc.put("/reels/id-1") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aUpdateRequest())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value("id-1") }
        }
    }

    @Test
    fun `should change reel state and return 200`() {
        every { changeReelState.changeState("id-1", any()) } returns aReel(state = ReelState.USED_UP)

        mvc.patch("/reels/id-1/state") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = """{"state": "USED_UP"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.state") { value("USED_UP") }
        }
    }

    @Test
    fun `should return 400 when state is missing on change state`() {
        mvc.patch("/reels/id-1/state") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = """{}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should return 404 when changing state of non-existent reel`() {
        every { changeReelState.changeState("unknown", any()) } throws ReelNotFoundException("unknown")

        mvc.patch("/reels/unknown/state") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = """{"state": "USED_UP"}"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should delete reel and return 204`() {
        every { deleteReel.delete("id-1") } returns Unit

        mvc.delete("/reels/id-1") {
            with(jwt())
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mvc.get("/reels") {
        }.andExpect { status { isUnauthorized() } }
    }

    private fun aReel(state: ReelState = ReelState.NEW) =
        Reel(
            id = "id-1",
            brand = "Luxilon",
            model = "ALU Power",
            material = Material.POLYESTER,
            gaugeHundredthsMm = 125,
            reelLengthMeters = 200,
            costCents = 12000,
            stringFeeCents = 2500,
            metersPerJob = 11,
            purchaseDate = LocalDate.of(2026, 1, 15),
            state = state,
            createdAt = Instant.EPOCH,
        )

    private fun aCreateRequest() =
        CreateReelRequest(
            brand = "Luxilon",
            model = "ALU Power",
            material = Material.POLYESTER,
            gauge = BigDecimal("1.25"),
            reelLengthMeters = 200,
            cost = BigDecimal("120.00"),
            stringFee = BigDecimal("25.00"),
            metersPerJob = 11,
            purchaseDate = LocalDate.of(2026, 1, 15),
        )

    private fun aUpdateRequest() =
        UpdateReelRequest(
            brand = "Luxilon",
            model = "ALU Power",
            material = Material.POLYESTER,
            gauge = BigDecimal("1.25"),
            reelLengthMeters = 200,
            cost = BigDecimal("120.00"),
            stringFee = BigDecimal("25.00"),
            metersPerJob = 11,
            purchaseDate = LocalDate.of(2026, 1, 15),
        )
}
