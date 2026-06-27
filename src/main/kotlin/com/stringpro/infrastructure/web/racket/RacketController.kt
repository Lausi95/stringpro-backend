package com.stringpro.infrastructure.web.racket

import com.stringpro.application.ports.`in`.racket.CreateRacketCommand
import com.stringpro.application.ports.`in`.racket.CreateRacketUseCase
import com.stringpro.application.ports.`in`.racket.DeleteRacketUseCase
import com.stringpro.application.ports.`in`.racket.GetRacketUseCase
import com.stringpro.application.ports.`in`.racket.ListRacketsQuery
import com.stringpro.application.ports.`in`.racket.ListRacketsUseCase
import com.stringpro.application.ports.`in`.racket.UpdateRacketCommand
import com.stringpro.application.ports.`in`.racket.UpdateRacketUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/rackets")
@Tag(name = "Rackets")
class RacketController(
    private val createRacket: CreateRacketUseCase,
    private val getRacket: GetRacketUseCase,
    private val listRackets: ListRacketsUseCase,
    private val updateRacket: UpdateRacketUseCase,
    private val deleteRacket: DeleteRacketUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    @Operation(summary = "Create a racket")
    @ApiResponse(responseCode = "201", description = "Racket created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    fun create(@Valid @RequestBody request: CreateRacketRequest): ResponseEntity<RacketResponse> {
        MDC.put("customerId", request.customerId)
        val racket = createRacket.create(
            CreateRacketCommand(
                customerId = request.customerId,
                brand = request.brand,
                model = request.model,
                headSize = request.headSize,
                stringMains = request.stringMains,
                stringCrosses = request.stringCrosses,
                notes = request.notes,
            ),
        )
        MDC.put("racketId", racket.id)
        log.info("Racket created")
        return ResponseEntity
            .created(URI.create("/rackets/${racket.id}"))
            .body(racket.toResponse())
    }

    @GetMapping
    @Operation(summary = "List rackets for a customer")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Missing customerId")
    fun list(@RequestParam customerId: String): List<RacketResponse> {
        MDC.put("customerId", customerId)
        return listRackets.list(ListRacketsQuery(customerId)).map { it.toResponse() }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a racket by ID")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Racket not found")
    fun get(@PathVariable id: String): RacketResponse {
        MDC.put("racketId", id)
        return getRacket.get(id).toResponse()
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a racket")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "404", description = "Racket not found")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateRacketRequest,
    ): RacketResponse {
        MDC.put("racketId", id)
        return updateRacket.update(
            id,
            UpdateRacketCommand(
                brand = request.brand,
                model = request.model,
                headSize = request.headSize,
                stringMains = request.stringMains,
                stringCrosses = request.stringCrosses,
                notes = request.notes,
            ),
        ).toResponse()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a racket")
    @ApiResponse(responseCode = "204", description = "Racket deleted")
    @ApiResponse(responseCode = "404", description = "Racket not found")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        MDC.put("racketId", id)
        deleteRacket.delete(id)
        return ResponseEntity.noContent().build()
    }
}
