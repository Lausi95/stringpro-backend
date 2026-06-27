package com.stringpro.infrastructure.persistence.reel

import com.stringpro.application.domain.model.reel.Material
import com.stringpro.application.domain.model.reel.ReelState
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.time.LocalDate

@Document(collection = "reels")
data class ReelDocument(
    @Id val id: String,
    val brand: String,
    val model: String,
    val material: Material,
    val gaugeHundredthsMm: Int,
    val reelLengthMeters: Int,
    val costCents: Long,
    val stringFeeCents: Long,
    val metersPerJob: Int,
    val purchaseDate: LocalDate,
    val state: ReelState,
    val createdAt: Instant,
    val deletedAt: Instant?,
)
