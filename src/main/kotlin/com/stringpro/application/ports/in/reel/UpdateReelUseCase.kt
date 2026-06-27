package com.stringpro.application.ports.`in`.reel

import com.stringpro.application.domain.model.reel.Material
import com.stringpro.application.domain.model.reel.Reel
import java.time.LocalDate

interface UpdateReelUseCase {
    fun update(id: String, command: UpdateReelCommand): Reel
}

data class UpdateReelCommand(
    val brand: String,
    val model: String,
    val material: Material,
    val gaugeHundredthsMm: Int,
    val reelLengthMeters: Int,
    val costCents: Long,
    val stringFeeCents: Long,
    val metersPerJob: Int,
    val purchaseDate: LocalDate,
)
