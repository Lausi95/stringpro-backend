package com.stringpro.infrastructure.persistence.racket

import com.stringpro.application.domain.model.racket.Racket
import com.stringpro.application.ports.out.racket.RacketRepository
import org.springframework.stereotype.Component

@Component
class RacketRepositoryAdapter(
    private val mongoRepository: RacketMongoRepository,
) : RacketRepository {
    override fun save(racket: Racket): Racket = mongoRepository.save(racket.toDocument()).toDomain()

    override fun findById(id: String): Racket? = mongoRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()

    override fun findByCustomerId(customerId: String): List<Racket> =
        mongoRepository.findAllByCustomerIdAndDeletedAtIsNull(customerId).map { it.toDomain() }

    private fun Racket.toDocument() =
        RacketDocument(
            id = id,
            customerId = customerId,
            brand = brand,
            model = model,
            headSize = headSize,
            stringMains = stringMains,
            stringCrosses = stringCrosses,
            notes = notes,
            createdAt = createdAt,
            deletedAt = deletedAt,
        )

    private fun RacketDocument.toDomain() =
        Racket(
            id = id,
            customerId = customerId,
            brand = brand,
            model = model,
            headSize = headSize,
            stringMains = stringMains,
            stringCrosses = stringCrosses,
            notes = notes,
            createdAt = createdAt,
            deletedAt = deletedAt,
        )
}
