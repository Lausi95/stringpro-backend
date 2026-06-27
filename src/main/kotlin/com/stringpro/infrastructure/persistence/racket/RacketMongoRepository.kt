package com.stringpro.infrastructure.persistence.racket

import org.springframework.data.mongodb.repository.MongoRepository

interface RacketMongoRepository : MongoRepository<RacketDocument, String> {
    fun findByIdAndDeletedAtIsNull(id: String): RacketDocument?

    fun findAllByCustomerIdAndDeletedAtIsNull(customerId: String): List<RacketDocument>
}
