package com.stringpro.infrastructure.web.racket

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class CreateRacketRequest(
    @field:NotBlank val customerId: String,
    @field:NotBlank val brand: String,
    @field:NotBlank val model: String,
    @field:Min(400) @field:Max(900) val headSize: Int,
    @field:Min(14) @field:Max(20) val stringMains: Int,
    @field:Min(15) @field:Max(22) val stringCrosses: Int,
    val notes: String?,
)

data class UpdateRacketRequest(
    @field:NotBlank val brand: String,
    @field:NotBlank val model: String,
    @field:Min(400) @field:Max(900) val headSize: Int,
    @field:Min(14) @field:Max(20) val stringMains: Int,
    @field:Min(15) @field:Max(22) val stringCrosses: Int,
    val notes: String?,
)
