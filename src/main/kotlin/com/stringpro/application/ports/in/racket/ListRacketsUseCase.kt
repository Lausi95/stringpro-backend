package com.stringpro.application.ports.`in`.racket

import com.stringpro.application.domain.model.racket.Racket

interface ListRacketsUseCase {
    fun list(query: ListRacketsQuery): List<Racket>
}

data class ListRacketsQuery(
    val customerId: String,
)
