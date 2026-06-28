package com.stringpro.infrastructure.persistence.job

import org.springframework.data.mongodb.repository.MongoRepository

interface JobMongoRepository : MongoRepository<JobDocument, String> {
    fun findByIdAndDeletedAtIsNull(id: String): JobDocument?
}
