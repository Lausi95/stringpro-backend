package com.stringpro.application.ports.`in`.job

import com.stringpro.application.domain.model.job.Job
import java.time.LocalDate

interface CreateJobUseCase {
    fun create(command: CreateJobCommand): Job
}

data class CreateJobCommand(
    val customerId: String,
    val racketId: String,
    val dueDate: LocalDate,
    val notes: String?,
    val mainsTensionDeciKg: Int,
    val crossesTensionDeciKg: Int,
    val hybrid: Boolean,
    val mains: StringChoiceInput,
    val crosses: StringChoiceInput?,
    val serviceFeeCents: Long,
)
