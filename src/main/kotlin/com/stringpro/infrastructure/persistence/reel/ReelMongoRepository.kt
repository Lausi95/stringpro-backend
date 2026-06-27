package com.stringpro.infrastructure.persistence.reel

import com.stringpro.application.domain.model.reel.ReelState
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface ReelMongoRepository : MongoRepository<ReelDocument, String> {
    fun findByIdAndDeletedAtIsNull(id: String): ReelDocument?

    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<ReelDocument>

    fun findAllByStateAndDeletedAtIsNull(
        state: ReelState,
        pageable: Pageable,
    ): Page<ReelDocument>
}
