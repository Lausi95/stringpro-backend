package com.stringpro.infrastructure.persistence.settings

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "settings")
data class SettingsDocument(
    @Id val id: String,
    val serviceFeeCents: Long,
    val fullName: String,
    // Defaults to blank so documents written before the email→paypalHandle rename
    // (which lack this field) still map cleanly instead of failing on a missing value.
    val paypalHandle: String = "",
    val iban: String,
    val address: String,
    val updatedAt: Instant?,
)
