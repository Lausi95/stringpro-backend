package com.stringpro.application.ports.`in`.job

import com.stringpro.application.domain.model.job.Job
import java.time.LocalDate

interface UpdateJobUseCase {
    fun update(
        id: String,
        command: UpdateJobCommand,
    ): Job
}

/** Editable fields of a Job. customerId and racketId are immutable after creation. */
data class UpdateJobCommand(
    val dueDate: LocalDate,
    val notes: String?,
    val mainsTensionDeciKg: Int,
    val crossesTensionDeciKg: Int,
    val hybrid: Boolean,
    val mains: StringChoiceInput,
    val crosses: StringChoiceInput?,
    val serviceFeeCents: Long,
)
