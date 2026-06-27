package com.stringpro.infrastructure.persistence.customer

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface CustomerMongoRepository : MongoRepository<CustomerDocument, String> {
    fun findByIdAndDeletedAtIsNull(id: String): CustomerDocument?
    fun findByEmailAndDeletedAtIsNull(email: String): CustomerDocument?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<CustomerDocument>

    @Query("{ 'deletedAt': null, '\$or': [{ 'firstName': { '\$regex': ?0, '\$options': 'i' } }, { 'lastName': { '\$regex': ?0, '\$options': 'i' } }] }")
    fun findAllByNameAndDeletedAtIsNull(name: String, pageable: Pageable): Page<CustomerDocument>
}
