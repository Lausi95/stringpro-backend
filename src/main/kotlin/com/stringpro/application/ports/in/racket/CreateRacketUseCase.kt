package com.stringpro.application.ports.`in`.racket

import com.stringpro.application.domain.model.racket.Racket

interface CreateRacketUseCase {
    fun create(command: CreateRacketCommand): Racket
}

data class CreateRacketCommand(
    val customerId: String,
    val brand: String,
    val model: String,
    val headSize: Int,
    val stringMains: Int,
    val stringCrosses: Int,
    val notes: String?,
)
