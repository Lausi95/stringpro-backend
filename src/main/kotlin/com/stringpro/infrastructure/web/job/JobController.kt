package com.stringpro.infrastructure.web.job

import com.stringpro.application.domain.model.job.Stage
import com.stringpro.application.ports.`in`.job.ChangeJobStageCommand
import com.stringpro.application.ports.`in`.job.ChangeJobStageUseCase
import com.stringpro.application.ports.`in`.job.CreateJobCommand
import com.stringpro.application.ports.`in`.job.CreateJobUseCase
import com.stringpro.application.ports.`in`.job.DeleteJobUseCase
import com.stringpro.application.ports.`in`.job.GetJobUseCase
import com.stringpro.application.ports.`in`.job.ListJobsQuery
import com.stringpro.application.ports.`in`.job.ListJobsUseCase
import com.stringpro.application.ports.`in`.job.UpdateJobCommand
import com.stringpro.application.ports.`in`.job.UpdateJobUseCase
import com.stringpro.infrastructure.web.eurosToCents
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/jobs")
@Tag(name = "Jobs")
class JobController(
    private val createJob: CreateJobUseCase,
    private val getJob: GetJobUseCase,
    private val listJobs: ListJobsUseCase,
    private val updateJob: UpdateJobUseCase,
    private val changeJobStage: ChangeJobStageUseCase,
    private val deleteJob: DeleteJobUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    @Operation(summary = "Create a job")
    @ApiResponse(responseCode = "201", description = "Job created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "404", description = "Customer, racket or reel not found")
    @ApiResponse(responseCode = "409", description = "Racket not owned by customer")
    fun create(
        @Valid @RequestBody request: CreateJobRequest,
    ): ResponseEntity<JobResponse> {
        val job =
            createJob.create(
                CreateJobCommand(
                    customerId = request.customerId,
                    racketId = request.racketId,
                    dueDate = request.dueDate,
                    notes = request.notes,
                    mainsTensionDeciKg = decimalKgToDeci(request.mainsTension),
                    crossesTensionDeciKg = decimalKgToDeci(request.crossesTension),
                    hybrid = request.hybrid,
                    mains = request.mains.toInput(),
                    crosses = request.crosses?.toInput(),
                    serviceFeeCents = eurosToCents(request.serviceFee),
                ),
            )
        MDC.put("jobId", job.id)
        log.info("Job created")
        return ResponseEntity
            .created(URI.create("/jobs/${job.id}"))
            .body(job.toResponse())
    }

    @GetMapping
    @Operation(summary = "List jobs (paginated), filterable by stage, customer, racket and reel")
    @ApiResponse(responseCode = "200", description = "OK")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) stage: Stage?,
        @RequestParam(required = false) customerId: String?,
        @RequestParam(required = false) racketId: String?,
        @RequestParam(required = false) reelId: String?,
    ): PagedJobResponse =
        listJobs
            .list(ListJobsQuery(page, size, stage, customerId, racketId, reelId))
            .toResponse()

    @GetMapping("/{id}")
    @Operation(summary = "Get a job by ID")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Job not found")
    fun get(
        @PathVariable id: String,
    ): JobResponse {
        MDC.put("jobId", id)
        return getJob.get(id).toResponse()
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a job (customer and racket are immutable)")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "404", description = "Job or reel not found")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateJobRequest,
    ): JobResponse {
        MDC.put("jobId", id)
        return updateJob
            .update(
                id,
                UpdateJobCommand(
                    dueDate = request.dueDate,
                    notes = request.notes,
                    mainsTensionDeciKg = decimalKgToDeci(request.mainsTension),
                    crossesTensionDeciKg = decimalKgToDeci(request.crossesTension),
                    hybrid = request.hybrid,
                    mains = request.mains.toInput(),
                    crosses = request.crosses?.toInput(),
                    serviceFeeCents = eurosToCents(request.serviceFee),
                ),
            ).toResponse()
    }

    @PatchMapping("/{id}/stage")
    @Operation(summary = "Advance a job's stage (forward only)")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "404", description = "Job not found")
    @ApiResponse(responseCode = "409", description = "Stage cannot move backward")
    fun changeStage(
        @PathVariable id: String,
        @Valid @RequestBody request: ChangeJobStageRequest,
    ): JobResponse {
        MDC.put("jobId", id)
        val job = changeJobStage.changeStage(id, ChangeJobStageCommand(request.stage!!))
        log.info("Job stage changed to {}", job.stage)
        return job.toResponse()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job")
    @ApiResponse(responseCode = "204", description = "Job deleted")
    @ApiResponse(responseCode = "404", description = "Job not found")
    fun delete(
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        MDC.put("jobId", id)
        deleteJob.delete(id)
        return ResponseEntity.noContent().build()
    }
}
