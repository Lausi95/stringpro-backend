package com.stringpro.infrastructure.persistence.reel

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelState
import com.stringpro.application.ports.out.reel.ReelRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class ReelRepositoryAdapter(
    private val mongoRepository: ReelMongoRepository,
) : ReelRepository {
    override fun save(reel: Reel): Reel = mongoRepository.save(reel.toDocument()).toDomain()

    override fun findById(id: String): Reel? = mongoRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()

    override fun findAll(
        page: Int,
        size: Int,
        state: ReelState?,
    ): PageResult<Reel> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result: Page<ReelDocument> =
            if (state == null) {
                mongoRepository.findAllByDeletedAtIsNull(pageable)
            } else {
                mongoRepository.findAllByStateAndDeletedAtIsNull(state, pageable)
            }
        return PageResult(
            content = result.content.map { it.toDomain() },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.number,
            size = result.size,
        )
    }

    private fun Reel.toDocument() =
        ReelDocument(
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

    private fun ReelDocument.toDomain() =
        Reel(
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
