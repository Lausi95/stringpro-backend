package com.stringpro.infrastructure.web.settings

import com.stringpro.application.domain.model.settings.Settings
import com.stringpro.infrastructure.web.centsToEuros
import com.stringpro.infrastructure.web.validation.Iban
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Email
import java.math.BigDecimal
import java.time.Instant

// --- Request (API speaks decimal euros; storage is integer cents, see ADR 0002) ---
// Identity fields are optional and default to blank; format is validated only when present.

data class UpdateSettingsRequest(
    @field:DecimalMin(value = "0.00")
    @field:Digits(integer = 7, fraction = 2)
    val serviceFee: BigDecimal,
    val fullName: String? = null,
    @field:Email val email: String? = null,
    @field:Iban val iban: String? = null,
    val address: String? = null,
)

// --- Response ---

data class SettingsResponse(
    val serviceFee: BigDecimal,
    val fullName: String,
    val email: String,
    val iban: String,
    val address: String,
    val updatedAt: Instant?,
)

fun Settings.toResponse() =
    SettingsResponse(
        serviceFee = centsToEuros(serviceFeeCents),
        fullName = fullName,
        email = email,
        iban = iban,
        address = address,
        updatedAt = updatedAt,
    )
