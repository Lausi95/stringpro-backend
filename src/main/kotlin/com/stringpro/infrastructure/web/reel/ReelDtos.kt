package com.stringpro.infrastructure.web.reel

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.reel.Material
import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelState
import com.stringpro.infrastructure.web.centsToEuros
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

data class PagedReelResponse(
    val content: List<ReelResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
)

fun PageResult<Reel>.toResponse() =
    PagedReelResponse(
        content = content.map { it.toResponse() },
        totalElements = totalElements,
        totalPages = totalPages,
        page = page,
        size = size,
    )

fun Reel.toResponse() =
    ReelResponse(
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

// --- Unit conversions at the API edge (euro <-> cents live in the shared MoneyConversions.kt) ---

fun decimalMmToHundredths(mm: BigDecimal): Int = mm.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact()

fun hundredthsMmToDecimal(hundredths: Int): BigDecimal = BigDecimal.valueOf(hundredths.toLong()).movePointLeft(2)
