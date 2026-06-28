package com.stringpro.infrastructure.web.settings

import com.stringpro.application.ports.`in`.settings.GetSettingsUseCase
import com.stringpro.application.ports.`in`.settings.UpdateSettingsCommand
import com.stringpro.application.ports.`in`.settings.UpdateSettingsUseCase
import com.stringpro.infrastructure.web.eurosToCents
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/settings")
@Tag(name = "Settings")
class SettingsController(
    private val getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @Operation(summary = "Get the global settings (returns defaults until first saved)")
    @ApiResponse(responseCode = "200", description = "OK")
    fun get(): SettingsResponse = getSettings.get().toResponse()

    @PutMapping
    @Operation(summary = "Update the global settings")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    fun update(
        @Valid @RequestBody request: UpdateSettingsRequest,
    ): SettingsResponse {
        val settings =
            updateSettings.update(
                UpdateSettingsCommand(
                    serviceFeeCents = eurosToCents(request.serviceFee),
                    fullName = request.fullName.orEmpty(),
                    email = request.email.orEmpty(),
                    iban = request.iban.orEmpty().replace(" ", "").uppercase(),
                    address = request.address.orEmpty(),
                ),
            )
        log.info("Settings updated")
        return settings.toResponse()
    }
}
