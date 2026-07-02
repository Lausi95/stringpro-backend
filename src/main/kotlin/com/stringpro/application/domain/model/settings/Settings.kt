package com.stringpro.application.domain.model.settings

import java.time.Instant

data class Settings(
    val serviceFeeCents: Long,
    val fullName: String,
    val paypalHandle: String,
    val iban: String,
    val address: String,
    val updatedAt: Instant?,
) {
    companion object {
        val DEFAULT =
            Settings(
                serviceFeeCents = 0,
                fullName = "",
                paypalHandle = "",
                iban = "",
                address = "",
                updatedAt = null,
            )
    }
}
