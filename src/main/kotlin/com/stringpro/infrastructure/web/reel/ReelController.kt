package com.stringpro.infrastructure.web.reel

import com.stringpro.application.domain.model.reel.ReelState
import com.stringpro.application.ports.`in`.reel.ChangeReelStateCommand
import com.stringpro.application.ports.`in`.reel.ChangeReelStateUseCase
import com.stringpro.application.ports.`in`.reel.CreateReelCommand
import com.stringpro.application.ports.`in`.reel.CreateReelUseCase
import com.stringpro.application.ports.`in`.reel.DeleteReelUseCase
import com.stringpro.application.ports.`in`.reel.GetReelUseCase
import com.stringpro.application.ports.`in`.reel.ListReelsQuery
import com.stringpro.application.ports.`in`.reel.ListReelsUseCase
import com.stringpro.application.ports.`in`.reel.UpdateReelCommand
import com.stringpro.application.ports.`in`.reel.UpdateReelUseCase
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
@RequestMapping("/reels")
@Tag(name = "Reels")
class ReelController(
    private val createReel: CreateReelUseCase,
    private val getReel: GetReelUseCase,
    private val listReels: ListReelsUseCase,
    private val updateReel: UpdateReelUseCase,
    private val changeReelState: ChangeReelStateUseCase,
    private val deleteReel: DeleteReelUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    @Operation(summary = "Create a string reel")
    @ApiResponse(responseCode = "201", description = "Reel created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    fun create(
        @Valid @RequestBody request: CreateReelRequest,
    ): ResponseEntity<ReelResponse> {
        val reel =
            createReel.create(
                CreateReelCommand(
                    brand = request.brand,
                    model = request.model,
                    material = request.material!!,
                    gaugeHundredthsMm = decimalMmToHundredths(request.gauge),
                    reelLengthMeters = request.reelLengthMeters,
                    costCents = eurosToCents(request.cost),
                    stringFeeCents = eurosToCents(request.stringFee),
                    metersPerJob = request.metersPerJob,
                    purchaseDate = request.purchaseDate,
                ),
            )
        MDC.put("reelId", reel.id)
        log.info("Reel created")
        return ResponseEntity
            .created(URI.create("/reels/${reel.id}"))
            .body(reel.toResponse())
    }

    @GetMapping
    @Operation(summary = "List string reels, optionally filtered by state")
    @ApiResponse(responseCode = "200", description = "OK")
    fun list(
        @RequestParam(required = false) state: ReelState?,
    ): List<ReelResponse> = listReels.list(ListReelsQuery(state)).map { it.toResponse() }

    @GetMapping("/{id}")
    @Operation(summary = "Get a string reel by ID")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Reel not found")
    fun get(
        @PathVariable id: String,
    ): ReelResponse {
        MDC.put("reelId", id)
        return getReel.get(id).toResponse()
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a string reel")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "404", description = "Reel not found")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateReelRequest,
    ): ReelResponse {
        MDC.put("reelId", id)
        return updateReel.update(
            id,
            UpdateReelCommand(
                brand = request.brand,
                model = request.model,
                material = request.material!!,
                gaugeHundredthsMm = decimalMmToHundredths(request.gauge),
                reelLengthMeters = request.reelLengthMeters,
                costCents = eurosToCents(request.cost),
                stringFeeCents = eurosToCents(request.stringFee),
                metersPerJob = request.metersPerJob,
                purchaseDate = request.purchaseDate,
            ),
        ).toResponse()
    }

    @PatchMapping("/{id}/state")
    @Operation(summary = "Change a string reel's state")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "404", description = "Reel not found")
    fun changeState(
        @PathVariable id: String,
        @Valid @RequestBody request: ChangeReelStateRequest,
    ): ReelResponse {
        MDC.put("reelId", id)
        val reel = changeReelState.changeState(id, ChangeReelStateCommand(request.state!!))
        log.info("Reel state changed to {}", reel.state)
        return reel.toResponse()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a string reel")
    @ApiResponse(responseCode = "204", description = "Reel deleted")
    @ApiResponse(responseCode = "404", description = "Reel not found")
    fun delete(
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        MDC.put("reelId", id)
        deleteReel.delete(id)
        return ResponseEntity.noContent().build()
    }
}
