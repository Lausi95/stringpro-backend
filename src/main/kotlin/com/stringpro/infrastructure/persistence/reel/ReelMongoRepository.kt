package com.stringpro.infrastructure.persistence.reel

import com.stringpro.application.domain.model.reel.ReelState
import org.springframework.data.mongodb.repository.MongoRepository

interface ReelMongoRepository : MongoRepository<ReelDocument, String> {
    fun findByIdAndDeletedAtIsNull(id: String): ReelDocument?

    fun findAllByDeletedAtIsNull(): List<ReelDocument>

    fun findAllByStateAndDeletedAtIsNull(state: ReelState): List<ReelDocument>
}
