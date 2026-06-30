package com.stringpro.infrastructure.web.job

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.job.Job
import com.stringpro.application.domain.model.job.Stage
import com.stringpro.application.domain.model.job.StringChoice
import com.stringpro.application.domain.model.job.StringSourceType
import com.stringpro.application.ports.`in`.job.StringChoiceInput
import com.stringpro.infrastructure.web.centsToEuros
import com.stringpro.infrastructure.web.eurosToCents
import com.stringpro.infrastructure.web.validation.HalfStep
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate

// --- Requests (API speaks decimal euros and kilograms; storage is integer minor units) ---

data class StringSideRequest(
    @field:NotNull val type: StringSourceType?,
    val stringName: String? = null,
    val reelId: String? = null,
    @field:DecimalMin(value = "0.00")
    @field:Digits(integer = 7, fraction = 2)
    val stringFee: BigDecimal? = null,
)

data class CreateJobRequest(
    @field:NotBlank val customerId: String,
    @field:NotBlank val racketId: String,
    @field:NotNull @field:FutureOrPresent val dueDate: LocalDate,
    val notes: String? = null,
    @field:NotNull
    @field:DecimalMin(value = "5.0")
    @field:DecimalMax(value = "40.0")
    @field:Digits(integer = 2, fraction = 1)
    @field:HalfStep
    val mainsTension: BigDecimal,
    @field:NotNull
    @field:DecimalMin(value = "5.0")
    @field:DecimalMax(value = "40.0")
    @field:Digits(integer = 2, fraction = 1)
    @field:HalfStep
    val crossesTension: BigDecimal,
    val hybrid: Boolean,
    @field:Valid @field:NotNull val mains: StringSideRequest,
    @field:Valid val crosses: StringSideRequest? = null,
    @field:NotNull
    @field:DecimalMin(value = "0.00")
    @field:Digits(integer = 7, fraction = 2)
    val serviceFee: BigDecimal,
)

data class UpdateJobRequest(
    @field:NotNull val dueDate: LocalDate,
    val notes: String? = null,
    @field:NotNull
    @field:DecimalMin(value = "5.0")
    @field:DecimalMax(value = "40.0")
    @field:Digits(integer = 2, fraction = 1)
    @field:HalfStep
    val mainsTension: BigDecimal,
    @field:NotNull
    @field:DecimalMin(value = "5.0")
    @field:DecimalMax(value = "40.0")
    @field:Digits(integer = 2, fraction = 1)
    @field:HalfStep
    val crossesTension: BigDecimal,
    val hybrid: Boolean,
    @field:Valid @field:NotNull val mains: StringSideRequest,
    @field:Valid val crosses: StringSideRequest? = null,
    @field:NotNull
    @field:DecimalMin(value = "0.00")
    @field:Digits(integer = 7, fraction = 2)
    val serviceFee: BigDecimal,
)

data class ChangeJobStageRequest(
    @field:NotNull val stage: Stage?,
)

// --- Responses ---

data class StringSideResponse(
    val type: StringSourceType,
    val stringName: String?,
    val reelId: String?,
    val stringFee: BigDecimal,
)

data class JobResponse(
    val id: String,
    val customerId: String,
    val racketId: String,
    val dueDate: LocalDate,
    val notes: String?,
    val mainsTension: BigDecimal,
    val crossesTension: BigDecimal,
    val hybrid: Boolean,
    val mains: StringSideResponse,
    val crosses: StringSideResponse?,
    val serviceFee: BigDecimal,
    val totalStringFee: BigDecimal,
    val total: BigDecimal,
    val amountPaid: BigDecimal,
    val fullyPaid: Boolean,
    val stage: Stage,
    val createdAt: Instant,
)

data class PagedJobResponse(
    val content: List<JobResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
)

// --- Mapping ---

fun StringSideRequest.toInput() =
    StringChoiceInput(
        type = type!!,
        stringName = stringName,
        reelId = reelId,
        stringFeeCents = stringFee?.let { eurosToCents(it) },
    )

fun StringChoice.toResponse() =
    StringSideResponse(
        type = sourceType,
        stringName = (this as? StringChoice.Own)?.stringName,
        reelId = (this as? StringChoice.Reel)?.reelId,
        stringFee = centsToEuros(feeCents),
    )

fun Job.toResponse() =
    JobResponse(
        id = id,
        customerId = customerId,
        racketId = racketId,
        dueDate = dueDate,
        notes = notes,
        mainsTension = deciKgToDecimal(mainsTensionDeciKg),
        crossesTension = deciKgToDecimal(crossesTensionDeciKg),
        hybrid = hybrid,
        mains = mainsString.toResponse(),
        crosses = crossesString?.toResponse(),
        serviceFee = centsToEuros(serviceFeeCents),
        totalStringFee = centsToEuros(totalStringFeeCents),
        total = centsToEuros(totalCents),
        amountPaid = centsToEuros(amountPaidCents),
        fullyPaid = fullyPaid,
        stage = stage,
        createdAt = createdAt,
    )

fun PageResult<Job>.toResponse() =
    PagedJobResponse(
        content = content.map { it.toResponse() },
        totalElements = totalElements,
        totalPages = totalPages,
        page = page,
        size = size,
    )

// --- Unit conversions at the API edge (kilograms <-> tenths of a kilogram) ---

fun decimalKgToDeci(kg: BigDecimal): Int = kg.movePointRight(1).setScale(0, RoundingMode.HALF_UP).intValueExact()

fun deciKgToDecimal(deci: Int): BigDecimal = BigDecimal.valueOf(deci.toLong()).movePointLeft(1)
