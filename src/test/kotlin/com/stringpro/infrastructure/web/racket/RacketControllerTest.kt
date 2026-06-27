package com.stringpro.infrastructure.web.racket

import com.ninjasquad.springmockk.MockkBean
import com.stringpro.application.domain.model.customer.CustomerNotFoundException
import com.stringpro.application.domain.model.racket.Racket
import com.stringpro.application.domain.model.racket.RacketNotFoundException
import com.stringpro.application.ports.`in`.racket.CreateRacketUseCase
import com.stringpro.application.ports.`in`.racket.DeleteRacketUseCase
import com.stringpro.application.ports.`in`.racket.GetRacketUseCase
import com.stringpro.application.ports.`in`.racket.ListRacketsUseCase
import com.stringpro.application.ports.`in`.racket.UpdateRacketUseCase
import com.stringpro.infrastructure.config.SecurityConfig
import com.stringpro.infrastructure.web.GlobalExceptionHandler
import io.mockk.every
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
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@WebMvcTest(RacketController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class RacketControllerTest {

    @Autowired private lateinit var mvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var jwtDecoder: JwtDecoder
    @MockkBean private lateinit var createRacket: CreateRacketUseCase
    @MockkBean private lateinit var getRacket: GetRacketUseCase
    @MockkBean private lateinit var listRackets: ListRacketsUseCase
    @MockkBean private lateinit var updateRacket: UpdateRacketUseCase
    @MockkBean private lateinit var deleteRacket: DeleteRacketUseCase

    @Test
    fun `should create racket and return 201 with location header`() {
        every { createRacket.create(any()) } returns aRacket()

        mvc.post("/rackets") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest())
        }.andExpect {
            status { isCreated() }
            header { string("Location", "/rackets/id-1") }
            jsonPath("$.id") { value("id-1") }
            jsonPath("$.customerId") { value("cust-1") }
            jsonPath("$.stringMains") { value(16) }
        }
    }

    @Test
    fun `should return 404 when customer does not exist on create`() {
        every { createRacket.create(any()) } throws CustomerNotFoundException("cust-1")

        mvc.post("/rackets") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should return 400 when required field is missing on create`() {
        mvc.post("/rackets") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = """{"customerId": "cust-1"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should return 400 when head size is out of range on create`() {
        mvc.post("/rackets") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest().copy(headSize = 50))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should list rackets for a customer`() {
        every { listRackets.list(any()) } returns listOf(aRacket())

        mvc.get("/rackets") {
            with(jwt())
            param("customerId", "cust-1")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value("id-1") }
            jsonPath("$[0].customerId") { value("cust-1") }
        }
    }

    @Test
    fun `should return 400 when customerId is missing on list`() {
        mvc.get("/rackets") {
            with(jwt())
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should get racket by id`() {
        every { getRacket.get("id-1") } returns aRacket()

        mvc.get("/rackets/id-1") {
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value("id-1") }
        }
    }

    @Test
    fun `should return 404 when racket not found`() {
        every { getRacket.get("unknown") } throws RacketNotFoundException("unknown")

        mvc.get("/rackets/unknown") {
            with(jwt())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should update racket and return 200`() {
        every { updateRacket.update(any(), any()) } returns aRacket()

        mvc.put("/rackets/id-1") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aUpdateRequest())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value("id-1") }
        }
    }

    @Test
    fun `should delete racket and return 204`() {
        every { deleteRacket.delete("id-1") } returns Unit

        mvc.delete("/rackets/id-1") {
            with(jwt())
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mvc.get("/rackets") {
            param("customerId", "cust-1")
        }.andExpect { status { isUnauthorized() } }
    }

    private fun aRacket() = Racket(
        id = "id-1",
        customerId = "cust-1",
        brand = "Babolat",
        model = "Pure Aero",
        headSize = 645,
        stringMains = 16,
        stringCrosses = 19,
        notes = null,
        createdAt = Instant.EPOCH,
    )

    private fun aCreateRequest() = CreateRacketRequest(
        customerId = "cust-1",
        brand = "Babolat",
        model = "Pure Aero",
        headSize = 645,
        stringMains = 16,
        stringCrosses = 19,
        notes = null,
    )

    private fun aUpdateRequest() = UpdateRacketRequest(
        brand = "Babolat",
        model = "Pure Aero",
        headSize = 645,
        stringMains = 16,
        stringCrosses = 19,
        notes = null,
    )
}
