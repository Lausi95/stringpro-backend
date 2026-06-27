package com.stringpro.application.domain.model.racket

class RacketNotFoundException(id: String) : RuntimeException("Racket not found: $id")
