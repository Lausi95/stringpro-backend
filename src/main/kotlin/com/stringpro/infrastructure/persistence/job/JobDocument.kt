package com.stringpro.infrastructure.persistence.job

import com.stringpro.application.domain.model.job.Stage
import com.stringpro.application.domain.model.job.StringSourceType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.time.LocalDate

/**
 * Mongo document for a Job. The domain's sealed [com.stringpro.application.domain.model.job.StringChoice]
 * is flattened per side so the by-reel filter can query [mainsReelId] / [crossesReelId] directly.
 * The crosses-* fields are null when the Job is not hybrid.
 */
@Document(collection = "jobs")
data class JobDocument(
    @Id val id: String,
    val customerId: String,
    val racketId: String,
    val dueDate: LocalDate,
    val notes: String?,
    val mainsTensionDeciKg: Int,
    val crossesTensionDeciKg: Int,
    val hybrid: Boolean,
    val mainsStringType: StringSourceType,
    val mainsReelId: String?,
    val mainsStringName: String?,
    val mainsStringFeeCents: Long,
    val crossesStringType: StringSourceType?,
    val crossesReelId: String?,
    val crossesStringName: String?,
    val crossesStringFeeCents: Long?,
    val serviceFeeCents: Long,
    val stage: Stage,
    val createdAt: Instant,
    val amountPaidCents: Long,
    val fullyPaid: Boolean,
    val deletedAt: Instant?,
)
