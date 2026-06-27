package com.stringpro.application.ports.`in`.racket

import com.stringpro.application.domain.model.racket.Racket

interface GetRacketUseCase {
    fun get(id: String): Racket
}
