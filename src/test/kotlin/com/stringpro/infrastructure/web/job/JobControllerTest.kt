package com.stringpro.infrastructure.web.job

import com.ninjasquad.springmockk.MockkBean
import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.job.InvalidStageTransitionException
import com.stringpro.application.domain.model.job.Job
import com.stringpro.application.domain.model.job.JobNotFoundException
import com.stringpro.application.domain.model.job.RacketNotOwnedByCustomerException
import com.stringpro.application.domain.model.job.Stage
import com.stringpro.application.domain.model.job.StringChoice
import com.stringpro.application.domain.model.job.StringSourceType
import com.stringpro.application.ports.`in`.job.ChangeJobStageUseCase
import com.stringpro.application.ports.`in`.job.CreateJobCommand
import com.stringpro.application.ports.`in`.job.CreateJobUseCase
import com.stringpro.application.ports.`in`.job.DeleteJobUseCase
import com.stringpro.application.ports.`in`.job.GetJobUseCase
import com.stringpro.application.ports.`in`.job.ListJobsQuery
import com.stringpro.application.ports.`in`.job.ListJobsUseCase
import com.stringpro.application.ports.`in`.job.UpdateJobUseCase
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

@WebMvcTest(JobController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class)
class JobControllerTest {
    @Autowired private lateinit var mvc: MockMvc

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var jwtDecoder: JwtDecoder

    @MockkBean private lateinit var createJob: CreateJobUseCase

    @MockkBean private lateinit var getJob: GetJobUseCase

    @MockkBean private lateinit var listJobs: ListJobsUseCase

    @MockkBean private lateinit var updateJob: UpdateJobUseCase

    @MockkBean private lateinit var changeJobStage: ChangeJobStageUseCase

    @MockkBean private lateinit var deleteJob: DeleteJobUseCase

    private val dueDate: LocalDate = LocalDate.now().plusDays(10)

    @Test
    fun `should create job and return 201 with location header and derived totals`() {
        every { createJob.create(any()) } returns aJob()

        mvc.post("/jobs") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest())
        }.andExpect {
            status { isCreated() }
            header { string("Location", "/jobs/job-1") }
            jsonPath("$.id") { value("job-1") }
            jsonPath("$.stage") { value("ANNOUNCED") }
            jsonPath("$.mains.type") { value("REEL") }
            jsonPath("$.mains.stringFee") { value(30.00) }
            jsonPath("$.serviceFee") { value(25.00) }
            jsonPath("$.totalStringFee") { value(30.00) }
            jsonPath("$.total") { value(55.00) }
            jsonPath("$.mainsTension") { value(23.5) }
        }
    }

    @Test
    fun `should convert decimal euros and kilograms to integer minor units`() {
        val slot = slot<CreateJobCommand>()
        every { createJob.create(capture(slot)) } returns aJob()

        mvc.post("/jobs") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest())
        }.andExpect { status { isCreated() } }

        assertEquals(235, slot.captured.mainsTensionDeciKg)
        assertEquals(225, slot.captured.crossesTensionDeciKg)
        assertEquals(2500, slot.captured.serviceFeeCents)
        assertEquals(3000, slot.captured.mains.stringFeeCents)
    }

    @Test
    fun `should return 400 when required field is missing on create`() {
        mvc.post("/jobs") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = """{"customerId": "cust-1"}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `should return 400 when due date is in the past on create`() {
        mvc.post("/jobs") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(aCreateRequest().copy(dueDate = LocalDate.now().minusDays(1)))
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `should return 400 when tension is not a half-kg step`() {
        mvc.post("/jobs") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(aCreateRequest().copy(mainsTension = BigDecimal("23.3")))
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `should return 400 when tension is out of range`() {
        mvc.post("/jobs") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(aCreateRequest().copy(mainsTension = BigDecimal("50.0")))
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `should return 409 when racket is not owned by customer`() {
        every { createJob.create(any()) } throws RacketNotOwnedByCustomerException("rack-1", "cust-1")

        mvc.post("/jobs") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(aCreateRequest())
        }.andExpect { status { isConflict() } }
    }

    @Test
    fun `should list jobs passing all filters through to the query`() {
        val slot = slot<ListJobsQuery>()
        every { listJobs.list(capture(slot)) } returns PageResult(listOf(aJob()), 1L, 1, 0, 20)

        mvc.get("/jobs") {
            with(jwt())
            param("stage", "IN_PROGRESS")
            param("customerId", "cust-1")
            param("racketId", "rack-1")
            param("reelId", "reel-1")
        }.andExpect {
            status { isOk() }
            jsonPath("$.content[0].id") { value("job-1") }
        }
        assertEquals(Stage.IN_PROGRESS, slot.captured.stage)
        assertEquals("cust-1", slot.captured.customerId)
        assertEquals("rack-1", slot.captured.racketId)
        assertEquals("reel-1", slot.captured.reelId)
    }

    @Test
    fun `should return 400 when stage filter is not a known value`() {
        mvc.get("/jobs") {
            with(jwt())
            param("stage", "PAID")
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `should get job by id`() {
        every { getJob.get("job-1") } returns aJob()

        mvc.get("/jobs/job-1") {
            with(jwt())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value("job-1") }
        }
    }

    @Test
    fun `should return 404 when job not found`() {
        every { getJob.get("unknown") } throws JobNotFoundException("unknown")

        mvc.get("/jobs/unknown") {
            with(jwt())
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `should update job and allow a past due date`() {
        val slot = slot<com.stringpro.application.ports.`in`.job.UpdateJobCommand>()
        every { updateJob.update(any(), capture(slot)) } returns aJob()

        mvc.put("/jobs/job-1") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(anUpdateRequest().copy(dueDate = LocalDate.of(2020, 1, 1)))
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value("job-1") }
        }
        assertEquals(LocalDate.of(2020, 1, 1), slot.captured.dueDate)
    }

    @Test
    fun `should change stage and return 200`() {
        every { changeJobStage.changeStage("job-1", any()) } returns aJob(stage = Stage.IN_PROGRESS)

        mvc.patch("/jobs/job-1/stage") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = """{"stage": "IN_PROGRESS"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.stage") { value("IN_PROGRESS") }
        }
    }

    @Test
    fun `should return 409 when stage moves backward`() {
        every { changeJobStage.changeStage("job-1", any()) } throws
            InvalidStageTransitionException(Stage.DONE, Stage.PICKED_UP)

        mvc.patch("/jobs/job-1/stage") {
            with(jwt())
            contentType = MediaType.APPLICATION_JSON
            content = """{"stage": "PICKED_UP"}"""
        }.andExpect { status { isConflict() } }
    }

    @Test
    fun `should delete job and return 204`() {
        every { deleteJob.delete("job-1") } returns Unit

        mvc.delete("/jobs/job-1") {
            with(jwt())
        }.andExpect { status { isNoContent() } }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mvc.get("/jobs") {
        }.andExpect { status { isUnauthorized() } }
    }

    private fun aJob(stage: Stage = Stage.ANNOUNCED) =
        Job(
            id = "job-1",
            customerId = "cust-1",
            racketId = "rack-1",
            dueDate = LocalDate.of(2026, 12, 1),
            notes = null,
            mainsTensionDeciKg = 235,
            crossesTensionDeciKg = 225,
            hybrid = false,
            mainsString = StringChoice.Reel("reel-1", 3000),
            crossesString = null,
            serviceFeeCents = 2500,
            stage = stage,
            createdAt = Instant.EPOCH,
        )

    private fun aCreateRequest() =
        CreateJobRequest(
            customerId = "cust-1",
            racketId = "rack-1",
            dueDate = dueDate,
            notes = "handle with care",
            mainsTension = BigDecimal("23.5"),
            crossesTension = BigDecimal("22.5"),
            hybrid = false,
            mains = StringSideRequest(StringSourceType.REEL, null, "reel-1", BigDecimal("30.00")),
            crosses = null,
            serviceFee = BigDecimal("25.00"),
        )

    private fun anUpdateRequest() =
        UpdateJobRequest(
            dueDate = dueDate,
            notes = "edited",
            mainsTension = BigDecimal("24.0"),
            crossesTension = BigDecimal("24.0"),
            hybrid = false,
            mains = StringSideRequest(StringSourceType.REEL, null, "reel-1", BigDecimal("30.00")),
            crosses = null,
            serviceFee = BigDecimal("26.00"),
        )
}
