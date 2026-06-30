package com.stringpro.infrastructure.persistence.payment

import org.springframework.data.mongodb.repository.MongoRepository

interface PaymentMongoRepository : MongoRepository<PaymentDocument, String> {
    fun findByIdAndDeletedAtIsNull(id: String): PaymentDocument?

    fun findAllByJobIdAndDeletedAtIsNull(jobId: String): List<PaymentDocument>
}
