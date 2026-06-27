package com.stringpro.infrastructure.web.reel

import com.stringpro.application.domain.model.reel.Material
import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelState
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate

// --- Requests (API speaks decimal euros and millimetres; storage is integer minor units, see ADR 0002) ---

data class CreateReelRequest(
    @field:NotBlank val brand: String,
    @field:NotBlank val model: String,
    @field:NotNull val material: Material?,
    @field:DecimalMin(value = "0.0", inclusive = false)
    @field:Digits(integer = 2, fraction = 2)
    val gauge: BigDecimal,
    @field:Min(1) val reelLengthMeters: Int,
    @field:DecimalMin(value = "0.00")
    @field:Digits(integer = 7, fraction = 2)
    val cost: BigDecimal,
    @field:DecimalMin(value = "0.00")
    @field:Digits(integer = 7, fraction = 2)
    val stringFee: BigDecimal,
    @field:Min(1) val metersPerJob: Int,
    @field:PastOrPresent val purchaseDate: LocalDate,
)

data class UpdateReelRequest(
    @field:NotBlank val brand: String,
    @field:NotBlank val model: String,
    @field:NotNull val material: Material?,
    @field:DecimalMin(value = "0.0", inclusive = false)
    @field:Digits(integer = 2, fraction = 2)
    val gauge: BigDecimal,
    @field:Min(1) val reelLengthMeters: Int,
    @field:DecimalMin(value = "0.00")
    @field:Digits(integer = 7, fraction = 2)
    val cost: BigDecimal,
    @field:DecimalMin(value = "0.00")
    @field:Digits(integer = 7, fraction = 2)
    val stringFee: BigDecimal,
    @field:Min(1) val metersPerJob: Int,
    @field:PastOrPresent val purchaseDate: LocalDate,
)

data class ChangeReelStateRequest(
    @field:NotNull val state: ReelState?,
)

// --- Response ---

data class ReelResponse(
    val id: String,
    val brand: String,
    val model: String,
    val material: Material,
    val gauge: BigDecimal,
    val reelLengthMeters: Int,
    val cost: BigDecimal,
    val stringFee: BigDecimal,
    val metersPerJob: Int,
    val purchaseDate: LocalDate,
    val state: ReelState,
    val createdAt: Instant,
)

fun Reel.toResponse() = ReelResponse(
    id = id,
    brand = brand,
    model = model,
    material = material,
    gauge = hundredthsMmToDecimal(gaugeHundredthsMm),
    reelLengthMeters = reelLengthMeters,
    cost = centsToEuros(costCents),
    stringFee = centsToEuros(stringFeeCents),
    metersPerJob = metersPerJob,
    purchaseDate = purchaseDate,
    state = state,
    createdAt = createdAt,
)

// --- Unit conversions at the API edge ---

fun eurosToCents(euros: BigDecimal): Long =
    euros.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()

fun centsToEuros(cents: Long): BigDecimal =
    BigDecimal.valueOf(cents).movePointLeft(2)

fun decimalMmToHundredths(mm: BigDecimal): Int =
    mm.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact()

fun hundredthsMmToDecimal(hundredths: Int): BigDecimal =
    BigDecimal.valueOf(hundredths.toLong()).movePointLeft(2)
