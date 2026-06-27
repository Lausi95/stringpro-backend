package com.stringpro.application.ports.`in`.racket

import com.stringpro.application.domain.model.racket.Racket

interface UpdateRacketUseCase {
    fun update(
        id: String,
        command: UpdateRacketCommand,
    ): Racket
}

data class UpdateRacketCommand(
    val brand: String,
    val model: String,
    val headSize: Int,
    val stringMains: Int,
    val stringCrosses: Int,
    val notes: String?,
)
