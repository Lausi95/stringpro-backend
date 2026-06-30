package com.stringpro.infrastructure.persistence.payment

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.payment.Payment
import com.stringpro.application.ports.out.payment.PaymentRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import kotlin.math.ceil

@Component
class PaymentRepositoryAdapter(
    private val mongoRepository: PaymentMongoRepository,
    private val mongoTemplate: MongoTemplate,
) : PaymentRepository {
    override fun save(payment: Payment): Payment = mongoRepository.save(payment.toDocument()).toDomain()

    override fun findById(id: String): Payment? = mongoRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()

    override fun findAll(
        page: Int,
        size: Int,
        jobId: String?,
        customerId: String?,
    ): PageResult<Payment> {
        val filters = mutableListOf(Criteria.where("deletedAt").isNull)
        jobId?.let { filters += Criteria.where("jobId").isEqualTo(it) }
        customerId?.let { filters += Criteria.where("customerId").isEqualTo(it) }
        val criteria = Criteria().andOperator(*filters.toTypedArray())

        val totalElements = mongoTemplate.count(Query(criteria), PaymentDocument::class.java)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val content =
            mongoTemplate
                .find(Query(criteria).with(pageable), PaymentDocument::class.java)
                .map { it.toDomain() }

        return PageResult(
            content = content,
            totalElements = totalElements,
            totalPages = if (size == 0) 0 else ceil(totalElements.toDouble() / size).toInt(),
            page = page,
            size = size,
        )
    }

    override fun findAllByJobId(jobId: String): List<Payment> =
        mongoRepository.findAllByJobIdAndDeletedAtIsNull(jobId).map { it.toDomain() }

    private fun Payment.toDocument() =
        PaymentDocument(
            id = id,
            jobId = jobId,
            customerId = customerId,
            amountCents = amountCents,
            method = method,
            createdAt = createdAt,
            deletedAt = deletedAt,
        )

    private fun PaymentDocument.toDomain() =
        Payment(
            id = id,
            jobId = jobId,
            customerId = customerId,
            amountCents = amountCents,
            method = method,
            createdAt = createdAt,
            deletedAt = deletedAt,
        )
}
