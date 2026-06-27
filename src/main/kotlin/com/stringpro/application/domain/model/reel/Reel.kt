package com.stringpro.application.domain.model.reel

import java.time.Instant
import java.time.LocalDate

data class Reel(
    val id: String,
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
    val deletedAt: Instant? = null,
)
