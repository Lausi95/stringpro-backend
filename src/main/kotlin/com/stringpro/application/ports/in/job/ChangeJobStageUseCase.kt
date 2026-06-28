package com.stringpro.application.ports.`in`.job

import com.stringpro.application.domain.model.job.Job
import com.stringpro.application.domain.model.job.Stage

interface ChangeJobStageUseCase {
    fun changeStage(
        id: String,
        command: ChangeJobStageCommand,
    ): Job
}

data class ChangeJobStageCommand(
    val targetStage: Stage,
)
