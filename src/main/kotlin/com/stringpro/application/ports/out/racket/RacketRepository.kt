package com.stringpro.application.ports.out.racket

import com.stringpro.application.domain.model.racket.Racket

interface RacketRepository {
    fun save(racket: Racket): Racket

    fun findById(id: String): Racket?

    fun findByCustomerId(customerId: String): List<Racket>
}
