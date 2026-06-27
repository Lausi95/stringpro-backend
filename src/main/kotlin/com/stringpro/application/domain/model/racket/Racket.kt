package com.stringpro.application.domain.model.racket

import java.time.Instant

data class Racket(
    val id: String,
    val customerId: String,
    val brand: String,
    val model: String,
    val headSize: Int,
    val stringMains: Int,
    val stringCrosses: Int,
    val notes: String?,
    val createdAt: Instant,
    val deletedAt: Instant? = null,
)
