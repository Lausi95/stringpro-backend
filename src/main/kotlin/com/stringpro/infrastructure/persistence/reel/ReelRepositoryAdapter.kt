package com.stringpro.infrastructure.persistence.reel

import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelState
import com.stringpro.application.ports.out.reel.ReelRepository
import org.springframework.stereotype.Component

@Component
class ReelRepositoryAdapter(
    private val mongoRepository: ReelMongoRepository,
) : ReelRepository {

    override fun save(reel: Reel): Reel =
        mongoRepository.save(reel.toDocument()).toDomain()

    override fun findById(id: String): Reel? =
        mongoRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()

    override fun findAll(): List<Reel> =
        mongoRepository.findAllByDeletedAtIsNull().map { it.toDomain() }

    override fun findByState(state: ReelState): List<Reel> =
        mongoRepository.findAllByStateAndDeletedAtIsNull(state).map { it.toDomain() }

    private fun Reel.toDocument() = ReelDocument(
        id = id,
        brand = brand,
        model = model,
        material = material,
        gaugeHundredthsMm = gaugeHundredthsMm,
        reelLengthMeters = reelLengthMeters,
        costCents = costCents,
        stringFeeCents = stringFeeCents,
        metersPerJob = metersPerJob,
        purchaseDate = purchaseDate,
        state = state,
        createdAt = createdAt,
        deletedAt = deletedAt,
    )

    private fun ReelDocument.toDomain() = Reel(
        id = id,
        brand = brand,
        model = model,
        material = material,
        gaugeHundredthsMm = gaugeHundredthsMm,
        reelLengthMeters = reelLengthMeters,
        costCents = costCents,
        stringFeeCents = stringFeeCents,
        metersPerJob = metersPerJob,
        purchaseDate = purchaseDate,
        state = state,
        createdAt = createdAt,
        deletedAt = deletedAt,
    )
}
