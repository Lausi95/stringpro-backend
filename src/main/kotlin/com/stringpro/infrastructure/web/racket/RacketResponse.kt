package com.stringpro.infrastructure.web.racket

import com.stringpro.application.domain.model.racket.Racket
import java.time.Instant

data class RacketResponse(
    val id: String,
    val customerId: String,
    val brand: String,
    val model: String,
    val headSize: Int,
    val stringMains: Int,
    val stringCrosses: Int,
    val notes: String?,
    val createdAt: Instant,
)

fun Racket.toResponse() = RacketResponse(
    id = id,
    customerId = customerId,
    brand = brand,
    model = model,
    headSize = headSize,
    stringMains = stringMains,
    stringCrosses = stringCrosses,
    notes = notes,
    createdAt = createdAt,
)
