package com.stringpro.application.domain.model.job

class RacketNotOwnedByCustomerException(
    racketId: String,
    customerId: String,
) : RuntimeException("Racket $racketId is not owned by customer $customerId")
