package com.stringpro.application.domain.model.job

import java.time.Instant
import java.time.LocalDate

data class Job(
    val id: String,
    val customerId: String,
    val racketId: String,
    val dueDate: LocalDate,
    val notes: String?,
    val mainsTensionDeciKg: Int,
    val crossesTensionDeciKg: Int,
    val hybrid: Boolean,
    val mainsString: StringChoice,
    val crossesString: StringChoice?,
    val serviceFeeCents: Long,
    val stage: Stage,
    val createdAt: Instant,
    val deletedAt: Instant? = null,
) {
    init {
        require(hybrid == (crossesString != null)) {
            "A hybrid Job must have a crosses string; a non-hybrid Job must not"
        }
        require(mainsTensionDeciKg > 0) { "Mains tension must be positive" }
        require(crossesTensionDeciKg > 0) { "Crosses tension must be positive" }
        require(serviceFeeCents >= 0) { "Service fee cannot be negative" }
    }

    /** Total customer-facing String Fee: mains plus crosses (charged once when not hybrid). */
    val totalStringFeeCents: Long
        get() = mainsString.feeCents + (crossesString?.feeCents ?: 0)

    /** Full price of the Job: Service Fee plus total String Fee. */
    val totalCents: Long
        get() = serviceFeeCents + totalStringFeeCents

    /** Advance (or hold) the Stage; backward moves are rejected. May skip ahead. */
    fun withStage(target: Stage): Job {
        if (target.ordinal < stage.ordinal) {
            throw InvalidStageTransitionException(stage, target)
        }
        return copy(stage = target)
    }
}
