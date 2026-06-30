package com.stringpro.infrastructure.persistence.job

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.job.Job
import com.stringpro.application.domain.model.job.Stage
import com.stringpro.application.domain.model.job.StringChoice
import com.stringpro.application.domain.model.job.StringSourceType
import com.stringpro.application.ports.out.job.JobRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import kotlin.math.ceil

@Component
class JobRepositoryAdapter(
    private val mongoRepository: JobMongoRepository,
    private val mongoTemplate: MongoTemplate,
) : JobRepository {
    override fun save(job: Job): Job = mongoRepository.save(job.toDocument()).toDomain()

    override fun findById(id: String): Job? = mongoRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()

    override fun findAll(
        page: Int,
        size: Int,
        stage: Stage?,
        customerId: String?,
        racketId: String?,
        reelId: String?,
        fullyPaid: Boolean?,
    ): PageResult<Job> {
        val filters = mutableListOf(Criteria.where("deletedAt").isNull)
        stage?.let { filters += Criteria.where("stage").isEqualTo(it) }
        customerId?.let { filters += Criteria.where("customerId").isEqualTo(it) }
        racketId?.let { filters += Criteria.where("racketId").isEqualTo(it) }
        reelId?.let {
            filters +=
                Criteria().orOperator(
                    Criteria.where("mainsReelId").isEqualTo(it),
                    Criteria.where("crossesReelId").isEqualTo(it),
                )
        }
        fullyPaid?.let { filters += Criteria.where("fullyPaid").isEqualTo(it) }
        val criteria = Criteria().andOperator(*filters.toTypedArray())

        val totalElements = mongoTemplate.count(Query(criteria), JobDocument::class.java)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "dueDate"))
        val content =
            mongoTemplate
                .find(Query(criteria).with(pageable), JobDocument::class.java)
                .map { it.toDomain() }

        return PageResult(
            content = content,
            totalElements = totalElements,
            totalPages = if (size == 0) 0 else ceil(totalElements.toDouble() / size).toInt(),
            page = page,
            size = size,
        )
    }

    private fun Job.toDocument() =
        JobDocument(
            id = id,
            customerId = customerId,
            racketId = racketId,
            dueDate = dueDate,
            notes = notes,
            mainsTensionDeciKg = mainsTensionDeciKg,
            crossesTensionDeciKg = crossesTensionDeciKg,
            hybrid = hybrid,
            mainsStringType = mainsString.sourceType,
            mainsReelId = (mainsString as? StringChoice.Reel)?.reelId,
            mainsStringName = (mainsString as? StringChoice.Own)?.stringName,
            mainsStringFeeCents = mainsString.feeCents,
            crossesStringType = crossesString?.sourceType,
            crossesReelId = (crossesString as? StringChoice.Reel)?.reelId,
            crossesStringName = (crossesString as? StringChoice.Own)?.stringName,
            crossesStringFeeCents = crossesString?.feeCents,
            serviceFeeCents = serviceFeeCents,
            stage = stage,
            createdAt = createdAt,
            amountPaidCents = amountPaidCents,
            fullyPaid = fullyPaid,
            deletedAt = deletedAt,
        )

    private fun JobDocument.toDomain() =
        Job(
            id = id,
            customerId = customerId,
            racketId = racketId,
            dueDate = dueDate,
            notes = notes,
            mainsTensionDeciKg = mainsTensionDeciKg,
            crossesTensionDeciKg = crossesTensionDeciKg,
            hybrid = hybrid,
            mainsString =
                toStringChoice(
                    mainsStringType,
                    mainsReelId,
                    mainsStringName,
                    mainsStringFeeCents,
                )!!,
            crossesString =
                toStringChoice(
                    crossesStringType,
                    crossesReelId,
                    crossesStringName,
                    crossesStringFeeCents,
                ),
            serviceFeeCents = serviceFeeCents,
            stage = stage,
            createdAt = createdAt,
            amountPaidCents = amountPaidCents,
            fullyPaid = fullyPaid,
            deletedAt = deletedAt,
        )

    private fun toStringChoice(
        type: StringSourceType?,
        reelId: String?,
        stringName: String?,
        feeCents: Long?,
    ): StringChoice? =
        when (type) {
            null -> null
            StringSourceType.OWN -> StringChoice.Own(stringName!!)
            StringSourceType.REEL -> StringChoice.Reel(reelId!!, feeCents!!)
        }
}
