package com.stringpro.application.domain.model.settings

import java.time.Instant

data class Settings(
    val serviceFeeCents: Long,
    val fullName: String,
    val email: String,
    val iban: String,
    val address: String,
    val updatedAt: Instant?,
) {
    companion object {
        val DEFAULT =
            Settings(
                serviceFeeCents = 0,
                fullName = "",
                email = "",
                iban = "",
                address = "",
                updatedAt = null,
            )
    }
}
